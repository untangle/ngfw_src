/**
 * $Id$
 */
package com.untangle.node.smtp;

import static com.untangle.node.util.ASCIIUtil.bbToString;

import java.nio.ByteBuffer;

import org.apache.log4j.Logger;

import com.untangle.node.smtp.BeginMIMEToken;
import com.untangle.node.smtp.ByteBufferByteStuffer;
import com.untangle.node.smtp.CompleteMIMEToken;
import com.untangle.node.smtp.ContinuedMIMEToken;
import com.untangle.node.smtp.AUTHCommand;
import com.untangle.node.smtp.Command;
import com.untangle.node.smtp.SASLExchangeToken;
import com.untangle.node.smtp.UnparsableCommand;
import com.untangle.node.smtp.mime.MIMEAccumulator;
import com.untangle.node.token.Chunk;
import com.untangle.node.token.MetadataToken;
import com.untangle.node.token.Token;
import com.untangle.uvm.vnet.NodeTCPSession;

class SmtpC2SUnparser extends SmtpUnparser
{
    private static final String SERVER_UNPARSER_STATE_KEY = "SMTP-server-parser-state";

    private static final Logger logger = Logger.getLogger(SmtpC2SUnparser.class);

    private class SmtpC2SUnparserState
    {
        protected ByteBufferByteStuffer byteStuffer;
        protected MIMEAccumulator accumulator;
    }

    public SmtpC2SUnparser()
    {
        super( true );
    }

    public void handleNewSession( NodeTCPSession session )
    {
        SmtpC2SUnparserState state = new SmtpC2SUnparserState();
        session.attach( SERVER_UNPARSER_STATE_KEY, state );
    }
    
    @Override
    protected void doUnparse( NodeTCPSession session, Token token )
    {
        SmtpC2SUnparserState state = (SmtpC2SUnparserState) session.attachment( SERVER_UNPARSER_STATE_KEY );
        SmtpSharedState serverSideSharedState = (SmtpSharedState) session.attachment( SHARED_STATE_KEY );
        
        // -----------------------------------------------------------
        if (token instanceof AUTHCommand) {
            logger.debug("Received AUTHCommand token");

            ByteBuffer buf = token.getBytes();

            AUTHCommand authCmd = (AUTHCommand) token;
            String mechName = authCmd.getMechanismName();

            if (! serverSideSharedState.openSASLExchange( mechName ) ) {
                logger.debug("Unable to find SASLObserver for \"" + mechName + "\"");
                declarePassthru( session );
            } else {
                logger.debug("Opening SASL Exchange");
                switch ( serverSideSharedState.getSASLObserver().initialClientResponse( authCmd.getInitialResponse() ) ) {
                    case EXCHANGE_COMPLETE:
                        logger.debug("SASL Exchange complete");
                        serverSideSharedState.closeSASLExchange();
                        break;
                    case IN_PROGRESS:
                        break;// Nothing interesting to do
                    case RECOMMEND_PASSTHRU:
                        logger.debug("Entering passthru on advice of SASLObserver");
                        declarePassthru( session );
                }
            }

            session.sendDataToServer( buf );
            return;
        }

        // -----------------------------------------------------------
        if (token instanceof SASLExchangeToken) {
            logger.debug("Received SASLExchangeToken token");

            ByteBuffer buf = token.getBytes();

            if ( ! serverSideSharedState.isInSASLLogin() ) {
                logger.error("Received SASLExchangeToken without an open exchange");
            } else {
                switch ( serverSideSharedState.getSASLObserver().clientData( buf.duplicate() ) ) {
                    case EXCHANGE_COMPLETE:
                        logger.debug("SASL Exchange complete");
                        serverSideSharedState.closeSASLExchange();
                        break;
                    case IN_PROGRESS:
                        // Nothing to do
                        break;
                    case RECOMMEND_PASSTHRU:
                        logger.debug("Entering passthru on advice of SASLObserver");
                        declarePassthru( session );
                }
            }
            session.sendDataToServer( buf );
            return;
        }

        // -----------------------------------------------------------
        if (token instanceof Command) {
            Command command = (Command) token;

            if (command instanceof UnparsableCommand) {
                logger.debug("Received UnparsableCommand to pass.  Register "
                        + "response action to know if there is a local parser error, or if "
                        + "this is an errant command");
                serverSideSharedState.commandReceived( command, new CommandParseErrorResponseCallback( session, command.getBytes() ) );
            } else if (command.getType() == CommandType.STARTTLS) {
                logger.debug("Saw STARTTLS command.  Enqueue response action to go into " + "passthru if accepted");
                serverSideSharedState.commandReceived( command, new TLSResponseCallback( session ) );
            } else {
                logger.debug("Send command to server: " + command.toDebugString());
                serverSideSharedState.commandReceived( command );
            }
            session.sendDataToServer( token.getBytes() );
            return;
        }

        // -----------------------------------------------------------
        if (token instanceof BeginMIMEToken) {
            logger.debug("Send BeginMIMEToken to server");
            serverSideSharedState.beginMsgTransmission();
            BeginMIMEToken bmt = (BeginMIMEToken) token;
            // Initialize the byte stuffer.
            state.byteStuffer = new ByteBufferByteStuffer();
            state.accumulator = bmt.getMIMEAccumulator();

            session.beginServerStream( bmt.toStuffedTCPStreamer( state.byteStuffer ) );
            return;
        }

        // -----------------------------------------------------------
        if (token instanceof CompleteMIMEToken) {
            logger.debug("Send CompleteMIMEToken to server");
            serverSideSharedState.beginMsgTransmission();

            session.beginServerStream( ((CompleteMIMEToken) token).toStuffedTCPStreamer( true, session ) );
            return;
        }
        // -----------------------------------------------------------
        if (token instanceof ContinuedMIMEToken) {
            ContinuedMIMEToken continuedToken = (ContinuedMIMEToken) token;

            ByteBuffer sink = null;
            if (continuedToken.shouldUnparse()) {
                logger.debug("Sending continued MIME chunk to server");
                ByteBuffer buf = token.getBytes();
                sink = ByteBuffer.allocate(buf.remaining() + ( state.byteStuffer.getLeftoverCount() * 2));
                state.byteStuffer.transfer(buf, sink);
                logger.debug("After byte stuffing, wound up with: " + sink.remaining() + " bytes");
            } else {
                logger.debug("Continued MIME chunk should not go to server (already sent or empty)");
            }
            if (continuedToken.getMIMEChunk().isLast()) {
                logger.debug("Last MIME chunk");
                ByteBuffer remainder = state.byteStuffer.getLast(true);
                state.byteStuffer = null;
                state.accumulator.dispose();
                state.accumulator = null;
                if ( sink != null ) 
                    session.sendDataToServer( sink );
                session.sendDataToServer( remainder );
                return;    
            } else {
                if (sink != null) {
                    session.sendDataToServer( sink );
                    return;
                } else {
                    logger.debug("Continued token empty (return nothing)");
                    return;
                }
            }
        }
        // -----------------------------------------------------------
        if (token instanceof Chunk) {
            ByteBuffer buf = token.getBytes();
            logger.debug("Sending chunk (" + buf.remaining() + " bytes) to server");

            session.sendDataToServer( buf );
            return;
        }

        // -----------------------------------------------------------
        if (token instanceof MetadataToken) {
            // Don't pass along metadata tokens
            return;
        }

        // Default (bad) case
        logger.error("Received unknown \"" + token.getClass().getName() + "\" token");

        session.sendDataToServer( token.getBytes() );
        return;
    }

    private void tlsStarting( NodeTCPSession session )
    {
        logger.debug("TLS Command accepted.  Enter passthru mode so as to not attempt to parse cyphertext");
        declarePassthru( session );// Inform the parser of this state
    }

    @Override
    public void handleFinalized( NodeTCPSession session )
    {
        SmtpC2SUnparserState state = (SmtpC2SUnparserState) session.attachment( SERVER_UNPARSER_STATE_KEY );

        super.handleFinalized( session );
        if (state.accumulator != null) {
            state.accumulator.dispose();
            state.accumulator = null;
        }
    }

    // ================ Inner Class =================

    /**
     * Callback registered with the CasingSessionTracker for the response to a command that could not be parsed.
     */
    class CommandParseErrorResponseCallback implements SmtpSharedState.ResponseAction
    {
        private NodeTCPSession session;
        private String offendingCommand;

        protected CommandParseErrorResponseCallback( NodeTCPSession session, ByteBuffer bufWithOffendingLine )
        {
            this.session = session;
            this.offendingCommand = bbToString(bufWithOffendingLine);
        }

        public void response(int code)
        {
            if (code < 300) {
                logger.error("Parser could not parse command line \"" + this.offendingCommand + "\" yet accepted by server.  Parser error.  Enter passthru");
                declarePassthru( this.session );
            } else {
                logger.debug("Command \"" + this.offendingCommand + "\" unparsable, and rejected " + "by server.  Do not enter passthru (assume errant client)");
            }
        }
    }

    // ================ Inner Class =================

    /**
     * Callback registered with the SmtpSharedState for the response to the STARTTLS command
     */
    class TLSResponseCallback implements SmtpSharedState.ResponseAction
    {
        private NodeTCPSession session;

        protected TLSResponseCallback( NodeTCPSession session )
        {
            this.session = session;
        }
        
        public void response(int code)
        {
            if (code < 300) {
                tlsStarting( session );
            } else {
                logger.debug("STARTTLS command rejected.  Do not go into passthru");
            }
        }
    }
}
