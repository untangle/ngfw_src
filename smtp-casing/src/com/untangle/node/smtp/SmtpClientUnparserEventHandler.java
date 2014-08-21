/**
 * $Id$
 */
package com.untangle.node.smtp;

import java.nio.ByteBuffer;

import org.apache.log4j.Logger;

import com.untangle.node.smtp.Response;
import com.untangle.node.smtp.SASLExchangeToken;
import com.untangle.uvm.vnet.MetadataToken;
import com.untangle.uvm.vnet.Token;
import com.untangle.uvm.vnet.ReleaseToken;
import com.untangle.uvm.vnet.NodeTCPSession;
import com.untangle.uvm.vnet.AbstractEventHandler;

public class SmtpClientUnparserEventHandler extends AbstractEventHandler
{
    protected static final String SHARED_STATE_KEY = "SMTP-shared-state";

    private final Logger logger = Logger.getLogger(SmtpClientUnparserEventHandler.class);

    public SmtpClientUnparserEventHandler()
    {
        super();
    }

    @Override
    public void handleTCPClientChunk( NodeTCPSession session, ByteBuffer data )
    {
        logger.warn("Received data when expect object");
        throw new RuntimeException("Received data when expect object");
    }

    @Override
    public void handleTCPServerChunk( NodeTCPSession session, ByteBuffer data )
    {
        logger.warn("Received data when expect object");
        throw new RuntimeException("Received data when expect object");
    }

    @Override
    public void handleTCPClientObject( NodeTCPSession session, Object obj )
    {
        logger.warn("Received object but expected data.");
        throw new RuntimeException("Received object but expected data.");
    }
    
    @Override
    public void handleTCPServerObject( NodeTCPSession session, Object obj )
    {
        unparse( session, obj, true );
    }

    @Override
    public void handleTCPClientDataEnd( NodeTCPSession session, ByteBuffer data )
    {
        if ( data.hasRemaining() ) {
            logger.warn("Received data when expect object");
            throw new RuntimeException("Received data when expect object");
        }
    }

    @Override
    public void handleTCPServerDataEnd( NodeTCPSession session, ByteBuffer data )
    {
        if ( data.hasRemaining() ) {
            logger.warn("Received data when expect object");
            throw new RuntimeException("Received data when expect object");
        }
    }
    
    @Override
    public void handleTCPClientFIN( NodeTCPSession session )
    {
        logger.warn("Received unexpected event.");
        throw new RuntimeException("Received unexpected event.");
    }

    @Override
    public void handleTCPServerFIN( NodeTCPSession session )
    {
        session.shutdownClient();
    }

    // private methods --------------------------------------------------------

    private void unparse( NodeTCPSession session, Object obj, boolean s2c )
    {
        Token token = (Token) obj;

        try {
            if (token instanceof ReleaseToken) {
                ReleaseToken release = (ReleaseToken)token;

                if ( s2c )
                    session.sendDataToClient( release.getData() );
                else 
                    session.sendDataToServer( release.getData() );

                session.release();

                return;
            } else if (token instanceof PassThruToken) {
                logger.debug("Received PassThruToken");
                declarePassthru( session );// Inform the parser of this state
                return;
            } else {
                doUnparse( session, token );
                return;
            }
        } catch (Exception exn) {
            logger.error("internal error, closing connection", exn);

            session.resetClient();
            session.resetServer();

            return;
        }
    }

    protected void doUnparse( NodeTCPSession session, Token token )
    {
        SmtpSharedState clientSideSharedState = (SmtpSharedState) session.attachment( SHARED_STATE_KEY );

        // -----------------------------------------------------------
        if (token instanceof SASLExchangeToken) {
            logger.debug("Received SASLExchangeToken token");

            ByteBuffer buf = token.getBytes();

            if ( ! clientSideSharedState.isInSASLLogin() ) {
                logger.error("Received SASLExchangeToken without an open exchange");
            } else {
                switch ( clientSideSharedState.getSASLObserver().serverData(buf.duplicate() ) ) {
                    case EXCHANGE_COMPLETE:
                        logger.debug("SASL Exchange complete");
                        clientSideSharedState.closeSASLExchange();
                        break;
                    case IN_PROGRESS:
                        // Nothing to do
                        break;
                    case RECOMMEND_PASSTHRU:
                        logger.debug("Entering passthru on advice of SASLObserver");
                        declarePassthru( session );
                }
            }
            session.sendDataToClient( buf );
            return;
        }

        // -----------------------------------------------------------
        if (token instanceof MetadataToken) {
            // Don't pass along metadata tokens
            logger.debug("Pass along Metadata token as nothing");
            return;
        }

        // -----------------------------------------------------------
        if (token instanceof Response) {
            Response resp = (Response) token;
            clientSideSharedState.responseReceived(resp);

            logger.debug("Passing response to client: " + resp.toDebugString());
        } else {
            logger.debug("Unparse token of type " + (token == null ? "null" : token.getClass().getName()));
        }

        session.sendDataToClient( token.getBytes() );
        return;
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
    
}
