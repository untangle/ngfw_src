/**
 * $Id$
 */
package com.untangle.node.smtp;

import static com.untangle.node.smtp.SmtpNodeImpl.PROTOCOL_NAME;

import org.apache.log4j.Logger;

import com.untangle.node.token.AbstractUnparser;
import com.untangle.node.token.PassThruToken;
import com.untangle.node.token.Token;
import com.untangle.node.token.UnparseResult;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.vnet.NodeTCPSession;
import com.untangle.uvm.vnet.TCPStreamer;

/**
 * Base class for the SmtpClient/ServerUnparser
 */
abstract class SmtpUnparser extends AbstractUnparser
{
    private final SmtpCasing casing;
    private final Logger logger = Logger.getLogger(SmtpUnparser.class);
    private boolean passthru = false;

    private CasingSessionTracker casingSessionTracker;

    protected SmtpUnparser(NodeTCPSession session, boolean clientSide, SmtpCasing casing, CasingSessionTracker tracker)
    {
        super(session, clientSide);
        this.casing = casing;
        casingSessionTracker = tracker;
    }

    public SmtpCasing getCasing()
    {
        return casing;
    }

    /**
     * Is the casing currently in passthru mode
     */
    protected boolean isPassthru()
    {
        return passthru;
    }

    /**
     * Called by the unparser to declare that we are now in passthru mode. This is called either because of a parsing
     * error by the caller, or the reciept of a passthru token.
     * 
     */
    protected void declarePassthru()
    {
        passthru = true;
        casing.passthru();
    }

    /**
     * Called by the casing to declare that this instance should now be in passthru mode.
     */
    protected final void passthru()
    {
        passthru = true;
    }

    public UnparseResult unparse(Token token)
    {
        if (token instanceof PassThruToken) {
            logger.debug("Received PassThruToken");
            declarePassthru();// Inform the parser of this state
            return UnparseResult.NONE;
        }
        return doUnparse(token);
    }

    /**
     * Delegated "unparse" method. The superclass performs some housekeeping before calling this method. <br>
     * <br>
     * Note that subclasses should <b>not</b> worry about tracing, or receiving Passthru tokens
     */
    protected abstract UnparseResult doUnparse(Token token);

    public final TCPStreamer endSession()
    {
        logger.debug("(" + PROTOCOL_NAME + ")(" + (isClientSide() ? "client" : "server") + ") End Session");
        // getCasing().endSession(isClientSide());
        return null;
    }

    @Override
    public void handleFinalized()
    {
        logger.debug("(" + PROTOCOL_NAME + ")(" + (isClientSide() ? "client" : "server") + ") handleFinalized");
    }

    CasingSessionTracker getSessionTracker()
    {
        return casingSessionTracker;
    }
}
