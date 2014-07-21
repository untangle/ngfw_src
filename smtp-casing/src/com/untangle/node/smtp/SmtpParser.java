/**
 * $Id$
 */
package com.untangle.node.smtp;

import static com.untangle.node.smtp.SmtpNodeImpl.PROTOCOL_NAME;

import java.nio.ByteBuffer;

import org.apache.log4j.Logger;

import com.untangle.node.token.AbstractParser;
import com.untangle.node.token.Chunk;
import com.untangle.node.token.TokenStreamer;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.vnet.NodeTCPSession;

/**
 * Base class for the SmtpClient/ServerParser
 */
abstract class SmtpParser extends AbstractParser
{
    private final Logger logger = Logger.getLogger(SmtpParser.class);

    protected static final String SHARED_STATE_KEY = "SMTP-shared-state";
    
    /**
     * @param session
     *            the session
     * @param parent
     *            the parent casing
     * @param clientSide
     *            true if this is a client-side casing
     */
    protected SmtpParser( boolean clientSide )
    {
        super( clientSide );
    }

    /**
     * Is the casing currently in passthru mode
     */
    protected boolean isPassthru( NodeTCPSession session )
    {
        SmtpSharedState sharedState = (SmtpSharedState) session.attachment( SHARED_STATE_KEY );
        return sharedState.passthru;
    }

    /**
     * Called by the unparser to declare that we are now in passthru mode. This is called either because of a parsing
     * error by the caller, or the reciept of a passthru token.
     * 
     */
    protected void declarePassthru( NodeTCPSession session)
    {
        SmtpSharedState sharedState = (SmtpSharedState) session.attachment( SHARED_STATE_KEY );
        sharedState.passthru = true;
    }

    @Override
    public final void endSession( NodeTCPSession session )
    {
        logger.debug("(" + PROTOCOL_NAME + ")(" + (isClientSide() ? "client" : "server") + ") endSession()");
        // getCasing().endSession(isClientSide());
        super.endSession( session );
        return;
    }

    public void handleFinalized( NodeTCPSession session )
    {
        logger.debug("(" + PROTOCOL_NAME + ")(" + (isClientSide() ? "client" : "server") + ") handleFinalized()");
    }

    public final void parse( NodeTCPSession session, ByteBuffer buf )
    {
        try {
            if ( isPassthru( session ) ) {
                if ( clientSide )
                    session.sendObjectToServer( new Chunk(buf) );
                else
                    session.sendObjectToClient( new Chunk(buf) );
                return;
            } else {
                doParse( session, buf );
                return;
            }
        } catch ( Exception exn ) {
            session.shutdownClient();
            session.shutdownServer();
            return;
        }
    }

    /**
     * Delegated "parse" method. Superclass takes care of some housekeeping before calling this method. <br>
     * <br>
     * Note that if the casing is {@link #isPassthru in passthru} then this method will not be called.
     */
    protected abstract void doParse( NodeTCPSession session, ByteBuffer buf );

    public final void parseEnd( NodeTCPSession session, ByteBuffer buf )
    {
        if ( buf.hasRemaining() ) {
            logger.debug("(" + PROTOCOL_NAME + ")(" + (isClientSide() ? "client" : "server") + ") Passing final chunk of size: " + buf.remaining());

            if ( clientSide )
                session.sendObjectToServer( new Chunk(buf) );
            else
                session.sendObjectToClient( new Chunk(buf) );
            return;
        }
        return;
    }

    /**
     * Helper which compacts (and possibly expands) the buffer if anything remains. Otherwise, just returns null.
     */
    protected static ByteBuffer compactIfNotEmpty(ByteBuffer buf, int maxSz)
    {
        if (buf.hasRemaining()) {
            buf.compact();
            if (buf.limit() < maxSz) {
                ByteBuffer b = ByteBuffer.allocate(maxSz);
                buf.flip();
                b.put(buf);
                return b;
            }
            return buf;
        } else {
            return null;
        }
    }

}
