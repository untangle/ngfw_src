/**
 * $Id$
 */
package com.untangle.app.ftp;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.vnet.ChunkToken;
import com.untangle.uvm.vnet.EndMarkerToken;
import com.untangle.uvm.vnet.Token;
import com.untangle.uvm.vnet.TokenStreamer;
import com.untangle.uvm.vnet.ReleaseToken;
import com.untangle.uvm.vnet.TCPNewSessionRequest;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.Protocol;
import com.untangle.uvm.vnet.AppSession;
import com.untangle.uvm.vnet.AppTCPSession;
import com.untangle.uvm.vnet.AbstractEventHandler;

/**
 * Parses FTP traffic.
 */
public class FtpClientParserEventHandler extends AbstractEventHandler
{
    private static final char SP = ' ';
    private static final char CR = '\r';
    private static final char LF = '\n';

    private final Logger logger = Logger.getLogger(FtpClientParserEventHandler.class);

    public FtpClientParserEventHandler()
    {
    }

    @Override
    public void handleTCPNewSessionRequest( TCPNewSessionRequest request )
    {
        Fitting fitting = request.pipelineConnector().getInputFitting();
        if ( fitting == Fitting.FTP_DATA_STREAM ) {
            request.globalAttach( AppSession.KEY_FTP_DATA_SESSION, Boolean.TRUE );
        }
    }
    
    @Override
    public void handleTCPNewSession( AppTCPSession session )
    {
        session.clientLineBuffering( true );
    }

    @Override
    public void handleTCPClientChunk( AppTCPSession session, ByteBuffer data )
    {
        parse( session, data, false, false );
    }

    @Override
    public void handleTCPServerChunk( AppTCPSession session, ByteBuffer data )
    {
        logger.warn("Received data when expect object");
        throw new RuntimeException("Received data when expect object");
    }

    @Override
    public void handleTCPClientObject( AppTCPSession session, Object obj )
    {
        logger.warn("Received object but expected data.");
        throw new RuntimeException("Received object but expected data.");
    }
    
    @Override
    public void handleTCPServerObject( AppTCPSession session, Object obj )
    {
        logger.warn("Received object but expected data.");
        throw new RuntimeException("Received object but expected data.");
    }
    
    @Override
    public void handleTCPClientDataEnd( AppTCPSession session, ByteBuffer data )
    {
        parse( session, data, false, true);
    }

    @Override
    public void handleTCPServerDataEnd( AppTCPSession session, ByteBuffer data )
    {
        if ( data.hasRemaining() ) {
            logger.warn("Received data when expect object");
            throw new RuntimeException("Received data when expect object");
        }
    }

    @Override
    public void handleTCPClientFIN( AppTCPSession session )
    {
        endSession( session );
    }

    @Override
    public void handleTCPServerFIN( AppTCPSession session )
    {
        logger.warn("Received unexpected event.");
        throw new RuntimeException("Received unexpected event.");
    }

    private void parse( AppTCPSession session, ByteBuffer data, boolean s2c, boolean last )
    {
        ByteBuffer buf = data;
        ByteBuffer dup = buf.duplicate();
        try {
            if (last) {
                parseEnd( session, buf );
            } else {
                parse( session, buf );
            }
        } catch (Throwable exn) {
            String sessionEndpoints = "[" +
                session.getProtocol() + " : " + 
                session.getClientAddr() + ":" + session.getClientPort() + " -> " +
                session.getServerAddr() + ":" + session.getServerPort() + "]";

            session.release();

            if ( s2c ) {
                session.sendObjectToClient( new ReleaseToken() );
                session.sendDataToClient( dup );
            } else {
                session.sendObjectToServer( new ReleaseToken() );
                session.sendDataToServer( dup );
            }
        }

        return;
    }
    
    public void parse( AppTCPSession session, ByteBuffer buf )
    {
        Fitting fitting = session.pipelineConnector().getInputFitting();
        if ( fitting == Fitting.FTP_CTL_STREAM ) {
            parseCtl( session, buf );
            return;
        } else {
            ChunkToken c = new ChunkToken(buf.duplicate());
            session.sendObjectToServer( c );
            return;
        }
    }

    public void parseEnd( AppTCPSession session, ByteBuffer buf )
    {
        Fitting fitting = session.pipelineConnector().getInputFitting();
        if ( fitting == Fitting.FTP_DATA_STREAM ) {
            session.sendObjectToServer( EndMarkerToken.MARKER );
        } else {
            if (buf.hasRemaining()) {
                logger.warn("unread data in read buffer: " + buf.remaining());
            }
            return;
        }
    }

    public void endSession( AppTCPSession session )
    {
        session.shutdownServer();
        return;
    }

    private void parseCtl( AppTCPSession session, ByteBuffer buf )
    {
        if (completeLine(buf)) {
            byte[] ba = new byte[buf.remaining()];
            buf.get(ba);

            FtpFunction fn;
            String arg;
            if (2 == ba.length) {       /* empty line */
                fn = null;
                arg = null;
            } else if (5 > ba.length) { /* weird command */
                String weird = new String(ba);
                logger.warn("strange ftp command: " + weird);
                // XXX we need to do our own 500 response here
                fn = null;
                arg = null;
            } else {                    /* compliant */
                int i = Character.isWhitespace((char)ba[3]) ? 3 : 4;
                String fnStr = new String(ba, 0, i);
                fn = FtpFunction.valueOf(fnStr.toUpperCase());
                if (null == fn) {
                    throw new RuntimeException("Unknown FTP function: " + fnStr);
                }

                while (SP == ba[++i]);

                arg = (ba.length - 2 <= i) ? null
                    : new String(ba, i, ba.length - i - 2); // no CRLF
            }

            FtpCommand cmd = new FtpCommand(fn, arg);
            session.sendObjectToServer( cmd );
            return;
        } else {
            session.setClientBuffer( buf ); // wait for more data
            return;
        }
    }

    /**
     * True the buffer contains a complete line <CRLF>. Obsolete line
     * terminators are not accepted.
     *
     * @param buf to check.
     * @return true if a complete line.
     */
    private boolean completeLine(ByteBuffer buf)
    {
        int l = buf.limit();

        return buf.remaining() >= 2 && buf.get(l - 2) == CR
            && buf.get(l - 1) == LF;
    }
}
