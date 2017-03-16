/**
 * $Id$
 */
package com.untangle.app.ftp;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import com.untangle.uvm.vnet.ChunkToken;
import com.untangle.uvm.vnet.EndMarkerToken;
import com.untangle.uvm.vnet.Token;
import com.untangle.uvm.vnet.ReleaseToken;
import com.untangle.uvm.vnet.TokenStreamer;
import com.untangle.uvm.util.AsciiCharBuffer;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.AppTCPSession;
import com.untangle.uvm.vnet.AbstractEventHandler;

/**
 * Parser for the server side of FTP connection.
 */
public class FtpServerParserEventHandler extends AbstractEventHandler
{
    private static final char SP = ' ';
    private static final char HYPHEN = '-';
    private static final char CR = '\r';
    private static final char LF = '\n';

    private final Logger logger = Logger.getLogger(FtpServerParserEventHandler.class);

    public FtpServerParserEventHandler()
    {
    }

    @Override
    public void handleTCPNewSession( AppTCPSession session )
    {
        session.serverLineBuffering( true );
    }

    @Override
    public void handleTCPClientChunk( AppTCPSession session, ByteBuffer data )
    {
        logger.warn("Received data when expect object");
        throw new RuntimeException("Received data when expect object");
    }

    @Override
    public void handleTCPServerChunk( AppTCPSession session, ByteBuffer data )
    {
        parse( session, data, true, false );
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
        if ( data.hasRemaining() ) {
            logger.warn("Received data when expect object");
            throw new RuntimeException("Received data when expect object");
        }
    }

    @Override
    public void handleTCPServerDataEnd( AppTCPSession session, ByteBuffer data )
    {
        parse( session, data, true, true );
    }

    @Override
    public void handleTCPClientFIN( AppTCPSession session )
    {
        logger.warn("Received unexpected event.");
        throw new RuntimeException("Received unexpected event.");
    }

    @Override
    public void handleTCPServerFIN( AppTCPSession session )
    {
        endSession( session );
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
        Fitting fitting = session.pipelineConnector().getOutputFitting();
        if (Fitting.FTP_CTL_STREAM == fitting) {
            parseServerCtl( session, buf );
            return;
        } else if (Fitting.FTP_DATA_STREAM == fitting) {
            parseServerData( session, buf );
            return;
        } else {
            throw new IllegalStateException("bad input fitting: " + fitting);
        }
    }

    public void parseEnd( AppTCPSession session, ByteBuffer buf )
    {
        Fitting fitting = session.pipelineConnector().getOutputFitting();
        if ( fitting == Fitting.FTP_DATA_STREAM ) {
            session.sendObjectToClient( EndMarkerToken.MARKER );
            return;
        } else {
            if (buf.hasRemaining()) {
                logger.warn("unread data in read buffer: " + buf.remaining());
            }
            return;
        }
    }

    public void endSession( AppTCPSession session )
    {
        session.shutdownClient();
        return;
    }

    private void parseServerCtl( AppTCPSession session, ByteBuffer buf )
    {
        ByteBuffer dup = buf.duplicate();

        if (completeLine(dup)) {
            int replyCode = replyCode(dup);

            if (-1 == replyCode) {
                throw new RuntimeException("expected reply code");
            }

            switch (dup.get()) {
            case SP: {
                String message = AsciiCharBuffer.wrap(buf).toString();

                FtpReply reply = new FtpReply(replyCode, message);
                session.sendObjectToClient( reply );
                return;
            }

            case HYPHEN: {
                int i = dup.limit() - 2;
                while (3 < --i && LF != dup.get(i));

                if (LF != dup.get(i++)) {
                    break;
                }

                ByteBuffer end = dup.duplicate();
                end.position(i);
                end.limit(end.limit() - 2);
                int endCode = replyCode(end);

                if (-1 == endCode || SP != end.get()) {
                    break;
                }

                String message = AsciiCharBuffer.wrap(buf).toString();

                FtpReply reply = new FtpReply(replyCode, message);

                session.sendObjectToClient( reply );
                return;
            }

            default:
                throw new RuntimeException("expected a space");
            }
        }

        // incomplete input
        if (buf.limit() + 80 > buf.capacity()) {
            ByteBuffer b = ByteBuffer.allocate(2 * buf.capacity());
            b.put(buf);
            buf = b;
        } else {
            buf.compact();
        }

        session.setServerBuffer( buf ); // wait for more data
        return;
    }

    private void parseServerData( AppTCPSession session, ByteBuffer buf )
    {
        ChunkToken c = new ChunkToken(buf.duplicate());
        session.sendObjectToClient( c );
        return;
    }

    private int replyCode(ByteBuffer buf)
    {
        int i = 0;

        byte c = buf.get();
        if (48 <= c && 57 >= c) {
            i = (c - 48) * 100;
        } else {
            return -1;
        }

        c = buf.get();
        if (48 <= c && 57 >= c) {
            i += (c - 48) * 10;
        } else {
            return -1;
        }

        c = buf.get();
        if (48 <= c && 57 >= c) {
            i += (c - 48);
        } else {
            return -1;
        }

        return i;
    }

    /**
     * Checks if the buffer contains a complete line.
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
