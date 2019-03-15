/**
 * $Id$
 */

package com.untangle.app.virus_blocker;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.net.InetAddress;

import org.apache.log4j.Logger;

import com.untangle.app.ftp.FtpCommand;
import com.untangle.app.ftp.FtpFunction;
import com.untangle.app.ftp.FtpReply;
import com.untangle.app.ftp.FtpEventHandler;
import com.untangle.uvm.vnet.ChunkToken;
import com.untangle.uvm.vnet.EndMarkerToken;
import com.untangle.uvm.vnet.TokenStreamer;
import com.untangle.uvm.vnet.TokenStreamerAdaptor;
import com.untangle.uvm.vnet.AppSession;
import com.untangle.uvm.vnet.AppTCPSession;
import com.untangle.uvm.vnet.TCPStreamer;
import com.untangle.uvm.app.GenericRule;
import com.untangle.uvm.util.GlobUtil;

/**
 * Handler for the FTP protocol.
 */
class VirusFtpHandler extends FtpEventHandler
{
    private final VirusBlockerBaseApp app;

    private final Logger logger = Logger.getLogger(FtpEventHandler.class);

    /**
     * Holds the virus scanner FTP state
     */
    private class VirusFtpState extends VirusBlockerState
    {
        private VirusFileManager fileManager = null;
        private boolean memoryMode = false;
        private boolean scan = false;
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

    /**
     * Constructor
     * 
     * @param app
     *        blocker base application
     */
    VirusFtpHandler(VirusBlockerBaseApp app)
    {
        this.app = app;
    }

    /**
     * Handle new TCP sessions
     * 
     * @param session
     *        The session
     */
    @Override
    public void handleTCPNewSession(AppTCPSession session)
    {
        VirusFtpState state = new VirusFtpState();
        session.attach(state);
    }

    /**
     * Handle client data
     * 
     * @param session
     *        The session
     * @param c
     *        The data chunk
     */
    @Override
    protected void doClientData(AppTCPSession session, ChunkToken c)
    {
        VirusFtpState state = (VirusFtpState) session.attachment();
        if (app.getSettings().getScanFtp()) {
            logger.debug("doServerData()");

            if (state.fileManager == null) {
                logger.debug("creating file for client");
                createFile(session);
                state.c2s = true;
            }

            ChunkToken outChunkToken = trickle(session, c.getData());

            session.sendObjectToServer(outChunkToken);
            return;
        } else {
            session.sendObjectToServer(c);
            return;
        }
    }

    /**
     * Handle server data
     * 
     * @param session
     *        The session
     * @param c
     *        The data chunk
     */
    @Override
    protected void doServerData(AppTCPSession session, ChunkToken c)
    {
        VirusFtpState state = (VirusFtpState) session.attachment();
        if (app.getSettings().getScanFtp()) {
            logger.debug("doServerData()");

            if (state.fileManager == null) {
                logger.debug("creating file for server");
                createFile(session);
                state.c2s = false;
            }

            ChunkToken outChunkToken = trickle(session, c.getData());

            session.sendObjectToClient(outChunkToken);
            return;
        } else {
            session.sendObjectToClient(c);
            return;
        }
    }

    /**
     * Handle end of client data
     * 
     * @param session
     *        The session
     */
    @Override
    protected void doClientDataEnd(AppTCPSession session)
    {
        VirusFtpState state = (VirusFtpState) session.attachment();
        logger.debug("doClientDataEnd()");

        if (app.getSettings().getScanFtp() && state.c2s && state.fileManager != null) {
            state.fileHash = state.fileManager.getFileHash();

            if (logger.isDebugEnabled()) {
                logger.debug("c2s file: " + state.fileManager.getFileDisplayName());
            }

            TCPStreamer ts = scan(session);
            if (null != ts) {
                session.sendStreamerToServer(ts);
            }
            state.fileManager = null;
        } else {
            session.shutdownServer();
        }
    }

    /**
     * Handle end of server data
     * 
     * @param session
     *        The session
     */
    @Override
    protected void doServerDataEnd(AppTCPSession session)
    {
        VirusFtpState state = (VirusFtpState) session.attachment();
        logger.debug("doServerDataEnd()");

        if (app.getSettings().getScanFtp() && !state.c2s && state.fileManager != null) {
            state.fileHash = state.fileManager.getFileHash();

            if (logger.isDebugEnabled()) {
                logger.debug("!c2s file: " + state.fileManager.getFileDisplayName());
            }

            TCPStreamer ts = scan(session);
            if (null != ts) {
                session.sendStreamerToClient(ts);
            }
            state.fileManager = null;
        } else {
            session.shutdownClient();
        }
    }

    /**
     * Handle command
     * 
     * @param session
     *        The session
     * @param command
     *        The command
     */
    @Override
    protected void doCommand(AppTCPSession session, FtpCommand command)
    {
        // no longer have a setting for blocking partial fetches
        // it causes too many issues
        // if (FtpFunction.REST == command.getFunction() &&
        // !app.getSettings().getAllowFtpResume()) {
        // FtpReply reply = FtpReply.makeReply(502, "Command not implemented.");
        // session.sendObjectToClient( reply );
        // return;
        // }

        if (command.getFunction() == FtpFunction.RETR) {
            String fileName = command.getArgument();
            addFileName(session.getSessionId(), fileName);
        }
        session.sendObjectToServer(command);
        return;
    }

    /**
     * Handle reply
     * 
     * @param session
     *        The session
     * @param reply
     *        The reply
     */
    protected void doReply(AppTCPSession session, FtpReply reply)
    {
        if (reply.getReplyCode() == FtpReply.PASV || reply.getReplyCode() == FtpReply.EPSV) {
            try {
                InetSocketAddress socketAddress = reply.getSocketAddress();
                addDataSocket(socketAddress, session.getSessionId());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        session.sendObjectToClient(reply);
        return;
    }

    /**
     * Handle trickle
     * 
     * @param session
     *        The session
     * @param b
     *        The buffer
     * @return The chunk
     */
    private ChunkToken trickle(AppTCPSession session, ByteBuffer b)
    {
        VirusFtpState state = (VirusFtpState) session.attachment();
        int l = b.remaining() * app.getTricklePercent() / 100;

        while (b.hasRemaining()) {
            state.fileManager.write(b);
        }

        b.clear().limit(l);

        while (b.hasRemaining()) {
            state.fileManager.read(b);
        }

        b.flip();

        return new ChunkToken(b);
    }

    /**
     * Scan a stream
     * 
     * @param session
     *        The session
     * @return The streamer
     */
    private TCPStreamer scan(AppTCPSession session)
    {
        VirusFtpState state = (VirusFtpState) session.attachment();
        VirusScannerResult result;

        try {
            app.incrementScanCount();
            if (ignoredHost(session.sessionEvent().getSServerAddr())) {
                result = VirusScannerResult.CLEAN;
            } else {
                logger.debug("Scanning the SMTP file: " + state.fileManager.getFileDisplayName());
                result = app.getScanner().scanFile(state.fileManager.getFileObject(), session);
            }
        } catch (Exception exn) {
            // Should never happen
            throw new RuntimeException("could not scan", exn);
        }

        String fileName = (String) session.globalAttachment(AppSession.KEY_FTP_FILE_NAME);
        app.logEvent(new VirusFtpEvent(session.sessionEvent(), result.isClean(), result.getVirusName(), app.getName(), fileName));

        if (result.isClean()) {
            app.incrementPassCount();
            TokenStreamer tokSt = new VirusChunkStreamer(state.fileManager, null, EndMarkerToken.MARKER, true);
            return new TokenStreamerAdaptor(tokSt, session);
        } else {
            app.incrementBlockCount();
            session.shutdownClient();
            session.shutdownServer();
            return null;
        }
    }

    /**
     * Handle TCP finalized
     * 
     * @param session
     *        The session
     */
    @Override
    public void handleTCPFinalized(AppTCPSession session)
    {
        session.cleanupTempFiles();
    }

    /**
     * Create a file to be scaned
     * 
     * @param session
     *        The session
     */
    private void createFile(AppTCPSession session)
    {
        VirusFtpState state = (VirusFtpState) session.attachment();

        // start with the memory only flag in the settings
        state.memoryMode = app.getSettings().getForceMemoryMode();

        // if the file scanner is not installed we MUST use memory mode
        if (app.isFileScannerAvailable() == false) {
            state.memoryMode = true;
        }

        try {
            state.fileManager = new VirusFileManager(state.memoryMode, "VirusFtpHandler-");
            state.scan = true;
            if (state.memoryMode == false) {
                session.attachTempFile(state.fileManager.getTempFileAbsolutePath());
            }
        } catch (Exception exn) {
            logger.warn("Unable to initialize file manager: ", exn);
        }

        /**
         * Obtain the sessionId of the control session that opened this data
         * session
         */
        Long ctlSessionId = removeDataSocket(new InetSocketAddress(session.getServerAddr(), session.getServerPort()));
        if (ctlSessionId == null) {
            ctlSessionId = removeDataSocket(new InetSocketAddress(session.getClientAddr(), session.getClientPort()));
        }

        /**
         * Obtain the file name and attach it to the current session
         */
        if (ctlSessionId != null) {
            String fileName = removeFileName(ctlSessionId);
            if (fileName != null) session.globalAttach(AppSession.KEY_FTP_FILE_NAME, fileName);
        }

    }

    /**
     * Add filename
     * 
     * @param ctlSessionId
     *        The session ID
     * @param fileName
     *        The filename
     */
    public static void addFileName(Long ctlSessionId, String fileName)
    {
        fileNamesByCtlSessionId.put(ctlSessionId, fileName);
    }

    /**
     * Remove filename
     * 
     * @param ctlSessionId
     *        The session ID
     * @return result
     */
    public static String removeFileName(Long ctlSessionId)
    {
        if (fileNamesByCtlSessionId.containsKey(ctlSessionId)) {
            return fileNamesByCtlSessionId.remove(ctlSessionId);
        }
        return null;
    }

    /**
     * Check the ignore status for a host
     * 
     * @param host
     *        The host to check
     * @return To if ignored, otherwise false
     */
    private boolean ignoredHost(InetAddress host)
    {
        if (host == null) {
            return false;
        }
        Pattern p;

        for (Iterator<GenericRule> i = app.getSettings().getPassSites().iterator(); i.hasNext();) {
            GenericRule sr = i.next();
            if (sr.getEnabled()) {
                p = (Pattern) sr.attachment();
                if (null == p) {
                    try {
                        p = Pattern.compile(GlobUtil.globToRegex(sr.getString()));
                    } catch (Exception error) {
                        logger.error("Unable to compile passSite=" + sr.getString());
                    }
                    sr.attach(p);
                }
                if(p != null){
                    if (p.matcher(host.getHostName()).matches()) {
                        return true;
                    }
                    if (p.matcher(host.getHostAddress()).matches()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Clear the event handler cache
     */
    protected void clearEventHandlerCache()
    {
    }
}
