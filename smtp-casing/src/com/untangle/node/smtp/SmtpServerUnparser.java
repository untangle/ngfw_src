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
import com.untangle.node.token.UnparseResult;
import com.untangle.uvm.vnet.NodeTCPSession;

class SmtpServerUnparser extends SmtpUnparser
{
    private static final String STATE_KEY = "SMTP-client-parser-state";

    private final Logger logger = Logger.getLogger(SmtpServerUnparser.class);

    private class SmtpServerUnparserState
    {
        protected ByteBufferByteStuffer byteStuffer;
        protected MIMEAccumulator accumulator;
    }

    public SmtpServerUnparser()
    {
        super( true );
    }

    public void handleNewSession( NodeTCPSession session )
    {
        SmtpServerUnparserState state = new SmtpServerUnparserState();
        session.attach( STATE_KEY, state );
    }
    
    @Override
    protected UnparseResult doUnparse( NodeTCPSession session, Token token )
    {
        SmtpServerUnparserState state = (SmtpServerUnparserState) session.attachment( STATE_KEY );
        SmtpSharedState sharedState = (SmtpSharedState) session.globalAttachment( SHARED_STATE_KEY );
        
        // -----------------------------------------------------------
        if (token instanceof AUTHCommand) {
            logger.debug("Received AUTHCommand token");

            ByteBuffer buf = token.getBytes();

            AUTHCommand authCmd = (AUTHCommand) token;
            String mechName = authCmd.getMechanismName();

            if (! sharedState.openSASLExchange( mechName ) ) {
                logger.debug("Unable to find SASLObserver for \"" + mechName + "\"");
                declarePassthru( session );
            } else {
                logger.debug("Opening SASL Exchange");
                switch ( sharedState.getSASLObserver().initialClientResponse( authCmd.getInitialResponse() ) ) {
                    case EXCHANGE_COMPLETE:
                        logger.debug("SASL Exchange complete");
                        sharedState.closeSASLExchange();
                        break;
                    case IN_PROGRESS:
                        break;// Nothing interesting to do
                    case RECOMMEND_PASSTHRU:
                        logger.debug("Entering passthru on advice of SASLObserver");
                        declarePassthru( session );
                }
            }

            return new UnparseResult(buf);
        }

        // -----------------------------------------------------------
        if (token instanceof SASLExchangeToken) {
            logger.debug("Received SASLExchangeToken token");

            ByteBuffer buf = token.getBytes();

            if ( ! sharedState.isInSASLLogin() ) {
                logger.error("Received SASLExchangeToken without an open exchange");
            } else {
                switch ( sharedState.getSASLObserver().clientData( buf.duplicate() ) ) {
                    case EXCHANGE_COMPLETE:
                        logger.debug("SASL Exchange complete");
                        sharedState.closeSASLExchange();
                        break;
                    case IN_PROGRESS:
                        // Nothing to do
                        break;
                    case RECOMMEND_PASSTHRU:
                        logger.debug("Entering passthru on advice of SASLObserver");
                        declarePassthru( session );
                }
            }
            return new UnparseResult(buf);
        }

        // -----------------------------------------------------------
        if (token instanceof Command) {
            Command command = (Command) token;

            if (command instanceof UnparsableCommand) {
                logger.debug("Received UnparsableCommand to pass.  Register "
                        + "response action to know if there is a local parser error, or if "
                        + "this is an errant command");
                sharedState.commandReceived( command, new CommandParseErrorResponseCallback( session, command.getBytes() ) );
            } else if (command.getType() == CommandType.STARTTLS) {
                logger.debug("Saw STARTTLS command.  Enqueue response action to go into " + "passthru if accepted");
                sharedState.commandReceived( command, new TLSResponseCallback( session ) );
            } else {
                logger.debug("Send command to server: " + command.toDebugString());
                sharedState.commandReceived( command );
            }
            return new UnparseResult(token.getBytes());
        }

        // -----------------------------------------------------------
        if (token instanceof BeginMIMEToken) {
            logger.debug("Send BeginMIMEToken to server");
            sharedState.beginMsgTransmission();
            BeginMIMEToken bmt = (BeginMIMEToken) token;
            // Initialize the byte stuffer.
            state.byteStuffer = new ByteBufferByteStuffer();
            state.accumulator = bmt.getMIMEAccumulator();
            return new UnparseResult( bmt.toStuffedTCPStreamer( state.byteStuffer ) );
        }

        // -----------------------------------------------------------
        if (token instanceof CompleteMIMEToken) {
            logger.debug("Send CompleteMIMEToken to server");
            sharedState.beginMsgTransmission();
            return new UnparseResult(((CompleteMIMEToken) token).toStuffedTCPStreamer( true, session ));
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
                return new UnparseResult(sink == null ? new ByteBuffer[] { remainder } : new ByteBuffer[] { sink,
                        remainder });
            } else {
                if (sink != null) {
                    return new UnparseResult(sink);
                } else {
                    logger.debug("Continued token empty (return nothing)");
                    return UnparseResult.NONE;
                }
            }
        }
        // -----------------------------------------------------------
        if (token instanceof Chunk) {
            ByteBuffer buf = token.getBytes();
            logger.debug("Sending chunk (" + buf.remaining() + " bytes) to server");
            return new UnparseResult(buf);
        }

        // -----------------------------------------------------------
        if (token instanceof MetadataToken) {
            // Don't pass along metadata tokens
            return UnparseResult.NONE;
        }

        // Default (bad) case
        logger.error("Received unknown \"" + token.getClass().getName() + "\" token");
        return new UnparseResult(token.getBytes());
    }

    private void tlsStarting( NodeTCPSession session )
    {
        logger.debug("TLS Command accepted.  Enter passthru mode so as to not attempt to parse cyphertext");
        declarePassthru( session );// Inform the parser of this state
    }

    @Override
    public void handleFinalized( NodeTCPSession session )
    {
        SmtpServerUnparserState state = (SmtpServerUnparserState) session.attachment( STATE_KEY );

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
