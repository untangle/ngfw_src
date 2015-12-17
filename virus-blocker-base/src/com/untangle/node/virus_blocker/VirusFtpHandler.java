/**
 * $Id$
 */
package com.untangle.node.virus_blocker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.net.InetAddress;

import org.apache.log4j.Logger;

import com.untangle.node.ftp.FtpCommand;
import com.untangle.node.ftp.FtpFunction;
import com.untangle.node.ftp.FtpReply;
import com.untangle.node.ftp.FtpEventHandler;
import com.untangle.uvm.vnet.ChunkToken;
import com.untangle.uvm.vnet.EndMarkerToken;
import com.untangle.uvm.vnet.FileChunkStreamer;
import com.untangle.uvm.vnet.Token;
import com.untangle.uvm.vnet.TokenStreamer;
import com.untangle.uvm.vnet.TokenStreamerAdaptor;
import com.untangle.uvm.vnet.NodeSession;
import com.untangle.uvm.vnet.NodeTCPSession;
import com.untangle.uvm.vnet.TCPStreamer;
import com.untangle.uvm.node.GenericRule;
import com.untangle.uvm.util.GlobUtil;

/**
 * Handler for the FTP protocol.
 */
class VirusFtpHandler extends FtpEventHandler
{
    private final VirusBlockerBaseApp node;

    private final Logger logger = Logger.getLogger(FtpEventHandler.class);

    private class VirusFtpState
    {
        private File file = null;
        private FileInputStream inStream = null;
        private FileOutputStream outStream = null;
        private FileChannel inChannel = null;
        private FileChannel outChannel = null;
        private MessageDigest msgDigest = null;
        private DigestOutputStream msgStream = null;
        private WritableByteChannel msgChannel = null;
        private boolean c2s;
    }

    /**
     * Map of filenames requested.
     * 
     * Used by ftp virus scanner to know the name of the file being scanned -
     * the name is obtained on the control session and passed to the data
     * session using this map.
     */
    private static final Map<Long, String> fileNamesByCtlSessionId = new ConcurrentHashMap<Long, String>();

    VirusFtpHandler( VirusBlockerBaseApp node )
    {
        this.node = node;
    }

    @Override
    public void handleTCPNewSession( NodeTCPSession session )
    {
        VirusFtpState state = new VirusFtpState();
        session.attach( state );
    }
    
    @Override
    protected void doClientData( NodeTCPSession session, ChunkToken c )
    {
        VirusFtpState state = (VirusFtpState) session.attachment();
        if ( node.getSettings().getScanFtp() ) {
            logger.debug("doServerData()");

            if ( state.file == null ) {
                logger.debug("creating file for client");
                createFile( session );
                state.c2s = true;
            }

            ChunkToken outChunkToken = trickle( session, c.getData() );

            session.sendObjectToServer( outChunkToken );
            return;
        } else {
            session.sendObjectToServer( c );
            return;
        }
    }

    @Override
    protected void doServerData( NodeTCPSession session, ChunkToken c )
    {
        VirusFtpState state = (VirusFtpState) session.attachment();
        if ( node.getSettings().getScanFtp() ) {
            logger.debug("doServerData()");

            if ( state.file == null ) {
                logger.debug("creating file for server");
                createFile( session );
                state.c2s = false;
            }

            ChunkToken outChunkToken = trickle( session, c.getData() );

            session.sendObjectToClient( outChunkToken );
            return;
        } else {
            session.sendObjectToClient( c );
            return;
        }
    }

    @Override
    protected void doClientDataEnd( NodeTCPSession session )
    {
        VirusFtpState state = (VirusFtpState) session.attachment();
        logger.debug("doClientDataEnd()");

        if ( node.getSettings().getScanFtp() && state.c2s && state.file != null ) {
            try {
                BigInteger val = new BigInteger(1, state.msgDigest.digest());
                logger.info("FtpHandler MD5 = " + String.format("%1$032x", val));
                state.outChannel.close();
            } catch (IOException exn) {
                logger.warn("could not close out channel");
            }

            if (logger.isDebugEnabled()) {
                logger.debug("c2s file: " + state.file);
            }
            TCPStreamer ts = scan( session );
            if (null != ts) {
                session.sendStreamerToServer(ts);
            }
            state.file = null;
        } else {
            session.shutdownServer();
        }
    }

    @Override
    protected void doServerDataEnd( NodeTCPSession session )
    {
        VirusFtpState state = (VirusFtpState) session.attachment();
        logger.debug("doServerDataEnd()");

        if ( node.getSettings().getScanFtp() && !state.c2s && state.file != null ) {
            try {
                BigInteger val = new BigInteger(1, state.msgDigest.digest());
                logger.info("FtpHandler MD5 = " + String.format("%1$032x", val));
                state.outChannel.close();
            } catch (IOException exn) {
                logger.warn("could not close out channel", exn);
            }

            if (logger.isDebugEnabled()) {
                logger.debug("!c2s file: " + state.file);
            }
            TCPStreamer ts = scan( session );
            if (null != ts) {
                session.sendStreamerToClient(ts);
            }
            state.file = null;
        } else {
            session.shutdownClient();
        }
    }

    @Override
    protected void doCommand( NodeTCPSession session, FtpCommand command )
    {
        // no longer have a setting for blocking partial fetches
        // it causes too many issues
        // if (FtpFunction.REST == command.getFunction() &&
        // !node.getSettings().getAllowFtpResume()) {
        // FtpReply reply = FtpReply.makeReply(502, "Command not implemented.");
        // session.sendObjectToClient( reply );
        // return;
        // }

        if (command.getFunction() == FtpFunction.RETR){
            String fileName = command.getArgument();
            addFileName( session.getSessionId(), fileName );
        }
        session.sendObjectToServer( command );
        return;
    }
    
    protected void doReply( NodeTCPSession session, FtpReply reply )
    {
        if (reply.getReplyCode() == FtpReply.PASV || reply.getReplyCode() == FtpReply.EPSV){
            try {
                InetSocketAddress socketAddress = reply.getSocketAddress();
                addDataSocket( socketAddress, session.getSessionId() );
            } catch ( Exception e ) {
                throw new RuntimeException(e);
            }
        }
        session.sendObjectToClient( reply );
        return;
    }

    private ChunkToken trickle( NodeTCPSession session, ByteBuffer b )
    {
        VirusFtpState state = (VirusFtpState) session.attachment();
        int l = b.remaining() * node.getTricklePercent() / 100;

        try {
            while (b.hasRemaining()) {
                state.msgChannel.write(b);
            }

            b.clear().limit(l);

            while (b.hasRemaining()) {
                state.inChannel.read(b);
            }
        } catch (IOException exn) {
            throw new RuntimeException("could not trickle", exn);
        }

        b.flip();

        return new ChunkToken(b);
    }

    private TCPStreamer scan( NodeTCPSession session )
    {
        VirusFtpState state = (VirusFtpState) session.attachment();
        VirusScannerResult result;

        try {
            node.incrementScanCount();
            if( ignoredHost( session.sessionEvent().getSServerAddr() ) ){
                result = VirusScannerResult.CLEAN;
            } else {
                result = node.getScanner().scanFile( state.file );                
            }
        } catch (Exception exn) {
            // Should never happen
            throw new RuntimeException("could not scan", exn);
        }

        String fileName = (String) session.globalAttachment(NodeSession.KEY_FTP_FILE_NAME);
        node.logEvent( new VirusFtpEvent( session.sessionEvent(), result.isClean(), result.getVirusName(), node.getName(), fileName ) );

        if ( result.isClean() ) {
            node.incrementPassCount();
            TokenStreamer tokSt = new FileChunkStreamer( state.file, state.inChannel, null, EndMarkerToken.MARKER, true );
            return new TokenStreamerAdaptor( tokSt, session );
        } else {
            node.incrementBlockCount();
            session.shutdownClient();
            session.shutdownServer();
            return null;
        }
    }
    
    @Override
    public void handleTCPFinalized( NodeTCPSession session )
    {
        session.cleanupTempFiles();
    }

    private void createFile( NodeTCPSession session )
    {
        VirusFtpState state = (VirusFtpState) session.attachment();

        try {
            state.file = File.createTempFile("VirusFtpHandler-", null);
            if (state.file != null)
                session.attachTempFile(state.file.getAbsolutePath());
            
            state.inStream = new FileInputStream( state.file );
            state.inChannel = state.inStream.getChannel();

            state.outStream = new FileOutputStream( state.file );
            state.outChannel = state.outStream.getChannel();
            state.msgDigest = MessageDigest.getInstance("MD5");
            state.msgStream = new DigestOutputStream(state.outStream, state.msgDigest);
            state.msgChannel = Channels.newChannel(state.msgStream);
        } catch (IOException ioe) {
            throw new RuntimeException("could not create tmp file");
        }
        catch (NoSuchAlgorithmException nsae) {
            throw new RuntimeException("could not initialize message digest");
        }
        
        /**
         * Obtain the sessionId of the control session that opened this data session
         */
        Long ctlSessionId = removeDataSocket( new InetSocketAddress( session.getServerAddr(), session.getServerPort() ) );
        if (ctlSessionId == null) {
            ctlSessionId = removeDataSocket( new InetSocketAddress( session.getClientAddr(), session.getClientPort() ) );
        }

        /**
         * Obtain the file name and attach it to the current session
         */
        if (ctlSessionId != null) {
            String fileName = removeFileName(ctlSessionId);
            if (fileName != null)
                session.globalAttach( NodeSession.KEY_FTP_FILE_NAME, fileName );
        }

    }

    public static void addFileName(Long ctlSessionId, String fileName)
    {
        fileNamesByCtlSessionId.put(ctlSessionId, fileName);
    }

    public static String removeFileName(Long ctlSessionId)
    {
        if (fileNamesByCtlSessionId.containsKey(ctlSessionId)) {
            return fileNamesByCtlSessionId.remove(ctlSessionId);
        }
        return null;
    }

    private boolean ignoredHost( InetAddress host )
    {
        if (host == null){
            return false;
        }
        Pattern p;
        
        for (Iterator<GenericRule> i = node.getSettings().getPassSites().iterator(); i.hasNext();) {
            GenericRule sr = i.next();
            if (sr.getEnabled() ){
                p = (Pattern) sr.attachment();
                if( null == p ){
                    try{
                        p = Pattern.compile( GlobUtil.globToRegex( sr.getString() ) );
                    }catch( Exception error ){
                        logger.error("Unable to compile passSite="+sr.getString());
                    }                    
                    sr.attach( p );
                }
                if( p.matcher( host.getHostName() ).matches() ){
                    return true;
                }
                if( p.matcher( host.getHostAddress() ).matches() ){
                    return true;
                }
            }
        }
        return false;
    }
}
