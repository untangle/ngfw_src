/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.mail.impl.smtp;

import static com.untangle.node.util.ASCIIUtil.bbToString;

import java.nio.ByteBuffer;

import org.apache.log4j.Logger;

import com.untangle.node.mail.papi.BeginMIMEToken;
import com.untangle.node.mail.papi.ByteBufferByteStuffer;
import com.untangle.node.mail.papi.CompleteMIMEToken;
import com.untangle.node.mail.papi.ContinuedMIMEToken;
import com.untangle.node.mail.papi.MIMEAccumulator;
import com.untangle.node.mail.papi.smtp.AUTHCommand;
import com.untangle.node.mail.papi.smtp.Command;
import com.untangle.node.mail.papi.smtp.SASLExchangeToken;
import com.untangle.node.mail.papi.smtp.UnparsableCommand;
import com.untangle.node.token.Chunk;
import com.untangle.node.token.MetadataToken;
import com.untangle.node.token.Token;
import com.untangle.node.token.UnparseResult;
import com.untangle.uvm.vnet.NodeTCPSession;


/**
 * ...name says it all...
 */
class SmtpServerUnparser
    extends SmtpUnparser {

    private final Logger m_logger =
        Logger.getLogger(SmtpServerUnparser.class);

    private ByteBufferByteStuffer m_byteStuffer;
    private MIMEAccumulator m_accumulator;

    SmtpServerUnparser(NodeTCPSession session,
                       SmtpCasing parent,
                       CasingSessionTracker tracker) {
        super(session, parent, tracker, false);
        m_logger.debug("Created");
    }


    @Override
    protected UnparseResult doUnparse(Token token) {

        //-----------------------------------------------------------
        if(token instanceof AUTHCommand) {
            m_logger.debug("Received AUTHCommand token");

            ByteBuffer buf = token.getBytes();

            AUTHCommand authCmd = (AUTHCommand) token;
            String mechName = authCmd.getMechanismName();

            if(!getSmtpCasing().openSASLExchange(mechName)) {
                m_logger.debug("Unable to find SASLObserver for \"" +
                               mechName + "\"");
                declarePassthru();
            }
            else {
                m_logger.debug("Opening SASL Exchange");
                switch(getSmtpCasing().getSASLObserver().initialClientResponse(authCmd.getInitialResponse())) {
                case EXCHANGE_COMPLETE:
                    m_logger.debug("SASL Exchange complete");
                    getSmtpCasing().closeSASLExchange();
                    break;
                case IN_PROGRESS:
                    break;//Nothing interesting to do
                case RECOMMEND_PASSTHRU:
                    m_logger.debug("Entering passthru on advice of SASLObserver");
                    declarePassthru();
                }
            }

            return new UnparseResult(buf);
        }

        //-----------------------------------------------------------
        if(token instanceof SASLExchangeToken) {
            m_logger.debug("Received SASLExchangeToken token");

            ByteBuffer buf = token.getBytes();

            if(!getSmtpCasing().isInSASLLogin()) {
                m_logger.error("Received SASLExchangeToken without an open exchange");
            }
            else {
                switch(getSmtpCasing().getSASLObserver().clientData(buf.duplicate())) {
                case EXCHANGE_COMPLETE:
                    m_logger.debug("SASL Exchange complete");
                    getSmtpCasing().closeSASLExchange();
                    break;
                case IN_PROGRESS:
                    //Nothing to do
                    break;
                case RECOMMEND_PASSTHRU:
                    m_logger.debug("Entering passthru on advice of SASLObserver");
                    declarePassthru();
                }
            }
            return new UnparseResult(buf);
        }

        //-----------------------------------------------------------
        if(token instanceof Command) {
            Command command = (Command) token;

            if(command instanceof UnparsableCommand) {
                m_logger.debug("Received UnparsableCommand to pass.  Register " +
                               "response action to know if there is a local parser error, or if " +
                               "this is an errant command");
                getSessionTracker().commandReceived(command,
                                                    new CommandParseErrorResponseCallback(command.getBytes()));
            }
            else if(command.getType() == Command.CommandType.STARTTLS) {
                m_logger.debug("Saw STARTTLS command.  Enqueue response action to go into " +
                               "passthru if accepted");
                getSessionTracker().commandReceived(command, new TLSResponseCallback());
            }
            else {
                m_logger.debug("Send command to server: " +
                               command.toDebugString());
                getSessionTracker().commandReceived(command);
            }
            return new UnparseResult(token.getBytes());
        }

        //-----------------------------------------------------------
        if(token instanceof BeginMIMEToken) {
            m_logger.debug("Send BeginMIMEToken to server");
            getSessionTracker().beginMsgTransmission();
            BeginMIMEToken bmt = (BeginMIMEToken) token;
            //Initialize the byte stuffer.
            m_byteStuffer = new ByteBufferByteStuffer();
            m_accumulator = bmt.getMIMEAccumulator();
            return new UnparseResult(
                                     bmt.toStuffedTCPStreamer(m_byteStuffer));
        }

        //-----------------------------------------------------------
        if(token instanceof CompleteMIMEToken) {
            m_logger.debug("Send CompleteMIMEToken to server");
            getSessionTracker().beginMsgTransmission();
            return new UnparseResult(
                                     ((CompleteMIMEToken) token).toStuffedTCPStreamer(getPipeline(), true));
        }
        //-----------------------------------------------------------
        if(token instanceof ContinuedMIMEToken) {
            ContinuedMIMEToken continuedToken = (ContinuedMIMEToken) token;

            ByteBuffer sink = null;
            if(continuedToken.shouldUnparse()) {
                m_logger.debug("Sending continued MIME chunk to server");
                ByteBuffer buf = token.getBytes();
                sink = ByteBuffer.allocate(buf.remaining() + (m_byteStuffer.getLeftoverCount()*2));
                m_byteStuffer.transfer(buf, sink);
                m_logger.debug("After byte stuffing, wound up with: " + sink.remaining() + " bytes");
            }
            else {
                m_logger.debug("Continued MIME chunk should not go to server (already sent or empty)");
            }
            if(continuedToken.getMIMEChunk().isLast()) {
                m_logger.debug("Last MIME chunk");
                ByteBuffer remainder = m_byteStuffer.getLast(true);
                m_byteStuffer = null;
                m_accumulator.dispose();
                m_accumulator = null;
                return new UnparseResult(
                                         sink==null?
                                         new ByteBuffer[] {remainder}:
                                         new ByteBuffer[] {sink, remainder});
            }
            else {
                if(sink != null) {
                    return new UnparseResult(sink);
                }
                else {
                    m_logger.debug("Continued token empty (return nothing)");
                    return UnparseResult.NONE;
                }
            }
        }
        //-----------------------------------------------------------
        if(token instanceof Chunk) {
            ByteBuffer buf = token.getBytes();
            m_logger.debug("Sending chunk (" + buf.remaining() + " bytes) to server");
            return new UnparseResult(buf);
        }

        //-----------------------------------------------------------
        if(token instanceof MetadataToken) {
            //Don't pass along metadata tokens
            return UnparseResult.NONE;
        }

        //Default (bad) case
        m_logger.error("Received unknown \"" + token.getClass().getName() + "\" token");
        return new UnparseResult(token.getBytes());
    }

    private void tlsStarting() {
        m_logger.debug("TLS Command accepted.  Enter passthru mode so as to not attempt to parse cyphertext");
        declarePassthru();//Inform the parser of this state
    }

    @Override
    public void handleFinalized() {
        super.handleFinalized();
        if(m_accumulator != null) {
            m_accumulator.dispose();
            m_accumulator = null;
        }
    }


    //================ Inner Class =================

    /**
     * Callback registered with the CasingSessionTracker
     * for the response to a command that could
     * not be parsed.
     */
    class CommandParseErrorResponseCallback
        implements CasingSessionTracker.ResponseAction {

        private String m_offendingCommand;

        CommandParseErrorResponseCallback(ByteBuffer bufWithOffendingLine) {
            m_offendingCommand = bbToString(bufWithOffendingLine);
        }

        public void response(int code) {
            if(code < 300) {
                m_logger.error("Parser could not parse command line \"" +
                               m_offendingCommand + "\" yet accepted by server.  Parser error.  Enter passthru");
                declarePassthru();
            }
            else {
                m_logger.debug("Command \"" + m_offendingCommand + "\" unparsable, and rejected " +
                               "by server.  Do not enter passthru (assume errant client)");
            }
        }
    }


    //================ Inner Class =================

    /**
     * Callback registered with the CasingSessionTracker
     * for the response to the STARTTLS command
     */
    class TLSResponseCallback
        implements CasingSessionTracker.ResponseAction {
        public void response(int code) {
            if(code < 300) {
                tlsStarting();
            }
            else {
                m_logger.debug("STARTTLS command rejected.  Do not go into passthru");
            }
        }
    }
}
