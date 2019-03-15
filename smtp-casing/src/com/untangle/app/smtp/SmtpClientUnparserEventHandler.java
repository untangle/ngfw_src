/**
 * $Id$
 */
package com.untangle.app.smtp;

import java.nio.ByteBuffer;

import org.apache.log4j.Logger;

import com.untangle.app.smtp.Response;
import com.untangle.app.smtp.SASLExchangeToken;
import com.untangle.uvm.vnet.MetadataToken;
import com.untangle.uvm.vnet.Token;
import com.untangle.uvm.vnet.ReleaseToken;
import com.untangle.uvm.vnet.AppTCPSession;
import com.untangle.uvm.vnet.AbstractEventHandler;

/**
 * Unparse SMTP client event handler.
 */
public class SmtpClientUnparserEventHandler extends AbstractEventHandler
{
    protected static final String SHARED_STATE_KEY = "SMTP-shared-state";

    private final Logger logger = Logger.getLogger(SmtpClientUnparserEventHandler.class);

    /**
     * Initialize SmtpClientUnparserEventHandler.
     * @return Instance of SmtpClientUnparserEventHandler.
     */
    public SmtpClientUnparserEventHandler()
    {
        super();
    }

    /**
     * Process client chunk.
     * @param session AppTCPSession to handle.
     * @param data    ByteBuffer containing data to process.
     */
    @Override
    public void handleTCPClientChunk( AppTCPSession session, ByteBuffer data )
    {
        // grab the SSL Inspector status attachment and release if set to false
        Boolean sslInspectorStatus = (Boolean)session.globalAttachment(AppTCPSession.KEY_SSL_INSPECTOR_SESSION_INSPECT);

        if ((sslInspectorStatus != null) && (sslInspectorStatus.booleanValue() == false)) {
            session.sendDataToServer(data);
            session.release();
            return;
        }

        logger.warn("Received data when expect object");
        throw new RuntimeException("Received data when expect object");
    }

    /**
     * Process server chunk.
     * @param session AppTCPSession to handle.
     * @param data    ByteBuffer containing data to process.
     */
    @Override
    public void handleTCPServerChunk( AppTCPSession session, ByteBuffer data )
    {
        // grab the SSL Inspector status attachment and release if set to false
        Boolean sslInspectorStatus = (Boolean)session.globalAttachment(AppTCPSession.KEY_SSL_INSPECTOR_SESSION_INSPECT);

        if ((sslInspectorStatus != null) && (sslInspectorStatus.booleanValue() == false)) {
            session.sendDataToClient(data);
            session.release();
            return;
        }

        logger.warn("Received data when expect object");
        throw new RuntimeException("Received data when expect object");
    }

    /**
     * Process client object.
     * @param session AppTCPSession to handle.
     * @param obj     Object to process.
     */
    @Override
    public void handleTCPClientObject( AppTCPSession session, Object obj )
    {
        // grab the SSL Inspector status attachment and release if set to false
        Boolean sslInspectorStatus = (Boolean)session.globalAttachment(AppTCPSession.KEY_SSL_INSPECTOR_SESSION_INSPECT);

        if ((sslInspectorStatus != null) && (sslInspectorStatus.booleanValue() == false)) {
            session.release();
            return;
        }

        logger.warn("Received object but expected data.");
        throw new RuntimeException("Received object but expected data.");
    }
    
    /**
     * Process server object.
     * @param session AppTCPSession to handle.
     * @param obj     Object to process.
     */
    @Override
    public void handleTCPServerObject( AppTCPSession session, Object obj )
    {
        // grab the SSL Inspector status attachment and release if set to false
        Boolean sslInspectorStatus = (Boolean)session.globalAttachment(AppTCPSession.KEY_SSL_INSPECTOR_SESSION_INSPECT);

        if ((sslInspectorStatus != null) && (sslInspectorStatus.booleanValue() == false)) {
            session.release();
            return;
        }

        unparse( session, obj, true );
    }

    /**
     * Process client data end.
     * @param session AppTCPSession to handle.
     * @param data    ByteBuffer to process.
     */
    @Override
    public void handleTCPClientDataEnd( AppTCPSession session, ByteBuffer data )
    {
        if ( data.hasRemaining() ) {
            logger.warn("Received data when expect object");
            throw new RuntimeException("Received data when expect object");
        }
    }

    /**
     * Process server data end.
     * @param session AppTCPSession to handle.
     * @param data    ByteBuffer to process.
     */
    @Override
    public void handleTCPServerDataEnd( AppTCPSession session, ByteBuffer data )
    {
        if ( data.hasRemaining() ) {
            logger.warn("Received data when expect object");
            throw new RuntimeException("Received data when expect object");
        }
    }
    
    /**
     * Process client FIN.
     * @param session AppTCPSession to handle.
     */
    @Override
    public void handleTCPClientFIN( AppTCPSession session )
    {
        logger.warn("Received unexpected event.");
        throw new RuntimeException("Received unexpected event.");
    }

    /**
     * Process server FIN.
     * @param session AppTCPSession to handle.
     */
    @Override
    public void handleTCPServerFIN( AppTCPSession session )
    {
        session.shutdownClient();
    }

    // private methods --------------------------------------------------------

    /**
     * Handle AppTcpSession.
     * @param session AppTCPSession to handle.
     * @param obj     Object to parse.
     * @param s2c     If true, server to client.  Client to server otherwise.
     */
    private void unparse( AppTCPSession session, Object obj, boolean s2c )
    {
        Token token = (Token) obj;

        try {
            if (token instanceof ReleaseToken) {
                ReleaseToken release = (ReleaseToken)token;

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

    /**
     * Perform unparsing of session.
     * @param session AppTCPSession to process.
     * @param token   Token to process.
     */
    protected void doUnparse( AppTCPSession session, Token token )
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

        if(token != null){
            session.sendDataToClient( token.getBytes() );
        }
        return;
    }

    /**
     * Is the casing currently in passthru mode
     * @param session AppTCPSession to handle
     * @return true if passthu, otherwise false.
     */
    protected boolean isPassthru( AppTCPSession session )
    {
        SmtpSharedState sharedState = (SmtpSharedState) session.attachment( SHARED_STATE_KEY );
        return sharedState.passthru;
    }

    /**
     * Called by the unparser to declare that we are now in passthru mode. This is called either because of a parsing
     * error by the caller, or the reciept of a passthru token.
     * @param session AppTCPSession to set.
     */
    protected void declarePassthru( AppTCPSession session)
    {
        SmtpSharedState sharedState = (SmtpSharedState) session.attachment( SHARED_STATE_KEY );
        sharedState.passthru = true;
    }
    
}
