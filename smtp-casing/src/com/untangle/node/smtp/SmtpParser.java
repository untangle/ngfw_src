/**
 * $Id$
 */
package com.untangle.node.smtp;

import static com.untangle.node.smtp.SmtpNodeImpl.PROTOCOL_NAME;

import java.nio.ByteBuffer;

import org.apache.log4j.Logger;

import com.untangle.node.smtp.FatalMailParseException;
import com.untangle.node.token.AbstractParser;
import com.untangle.node.token.Chunk;
import com.untangle.node.token.ParseException;
import com.untangle.node.token.ParseResult;
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
        SmtpSharedState sharedState = (SmtpSharedState) session.globalAttachment( SHARED_STATE_KEY );
        return sharedState.passthru;
    }

    /**
     * Called by the unparser to declare that we are now in passthru mode. This is called either because of a parsing
     * error by the caller, or the reciept of a passthru token.
     * 
     */
    protected void declarePassthru( NodeTCPSession session)
    {
        SmtpSharedState sharedState = (SmtpSharedState) session.globalAttachment( SHARED_STATE_KEY );
        sharedState.passthru = true;
    }

    @Override
    public final TokenStreamer endSession( NodeTCPSession session )
    {
        logger.debug("(" + PROTOCOL_NAME + ")(" + (isClientSide() ? "client" : "server") + ") endSession()");
        // getCasing().endSession(isClientSide());
        return super.endSession( session );
    }

    public void handleFinalized( NodeTCPSession session )
    {
        logger.debug("(" + PROTOCOL_NAME + ")(" + (isClientSide() ? "client" : "server") + ") handleFinalized()");
    }

    public final ParseResult parse( NodeTCPSession session, ByteBuffer buf ) throws ParseException
    {
        try {
            return isPassthru( session ) ? new ParseResult(new Chunk(buf)) : doParse( session, buf );
        } catch (FatalMailParseException exn) {
            session.shutdownClient();
            session.shutdownServer();
            return new ParseResult();
        }
    }

    /**
     * Delegated "parse" method. Superclass takes care of some housekeeping before calling this method. <br>
     * <br>
     * Note that if the casing is {@link #isPassthru in passthru} then this method will not be called.
     */
    protected abstract ParseResult doParse( NodeTCPSession session, ByteBuffer buf ) throws FatalMailParseException;

    public final ParseResult parseEnd( NodeTCPSession session, ByteBuffer buf ) throws ParseException
    {
        if ( buf.hasRemaining() ) {
            logger.debug("(" + PROTOCOL_NAME + ")(" + (isClientSide() ? "client" : "server") + ") Passing final chunk of size: " + buf.remaining());
            return new ParseResult(new Chunk(buf));
        }
        return new ParseResult();
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
