/**
 * $Id$
 */
package com.untangle.node.ftp;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import com.untangle.node.token.AbstractParser;
import com.untangle.node.token.Chunk;
import com.untangle.node.token.EndMarker;
import com.untangle.node.token.Token;
import com.untangle.node.token.TokenStreamer;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.NodeTCPSession;

/**
 * Parses FTP traffic.
 *
 */
public class FtpClientParser extends AbstractParser
{
    private static final char SP = ' ';
    private static final char CR = '\r';
    private static final char LF = '\n';

    private final Logger logger = Logger.getLogger(FtpClientParser.class);

    public FtpClientParser()
    {
        super(true);
    }

    public void handleNewSession( NodeTCPSession session )
    {
        lineBuffering( session, true );
    }

    public void parse( NodeTCPSession session, ByteBuffer buf )
    {
        Fitting fitting = session.pipelineConnector().getInputFitting();
        if ( fitting == Fitting.FTP_CTL_STREAM ) {
            parseCtl( session, buf );
            return;
        } else {
            Chunk c = new Chunk(buf.duplicate());
            session.sendObjectToServer( c );
            return;
        }
    }

    public void parseEnd( NodeTCPSession session, ByteBuffer buf )
    {
        Fitting fitting = session.pipelineConnector().getInputFitting();
        if ( fitting == Fitting.FTP_DATA_STREAM ) {
            session.sendObjectToServer( EndMarker.MARKER );
        } else {
            if (buf.hasRemaining()) {
                logger.warn("unread data in read buffer: " + buf.remaining());
            }
            return;
        }
    }

    public void endSession( NodeTCPSession session )
    {
        session.shutdownServer();
        return;
    }

    private void parseCtl( NodeTCPSession session, ByteBuffer buf )
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
