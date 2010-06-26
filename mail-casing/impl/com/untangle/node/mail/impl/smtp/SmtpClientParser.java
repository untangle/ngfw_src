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

import static com.untangle.node.util.BufferUtil.findCrLf;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.untangle.node.mail.papi.AddressKind;
import com.untangle.node.mail.papi.BeginMIMEToken;
import com.untangle.node.mail.papi.ContinuedMIMEToken;
import com.untangle.node.mail.papi.FatalMailParseException;
import com.untangle.node.mail.papi.MIMEAccumulator;
import com.untangle.node.mail.papi.MessageBoundaryScanner;
import com.untangle.node.mail.papi.MessageInfo;
import com.untangle.node.mail.papi.MessageInfoFactory;
import com.untangle.node.mail.papi.smtp.AUTHCommand;
import com.untangle.node.mail.papi.smtp.Command;
import com.untangle.node.mail.papi.smtp.CommandParser;
import com.untangle.node.mail.papi.smtp.SASLExchangeToken;
import com.untangle.node.mail.papi.smtp.SmtpTransaction;
import com.untangle.node.mail.papi.smtp.UnparsableCommand;
import com.untangle.node.mime.EmailAddress;
import com.untangle.node.mime.MIMEMessageHeaders;
import com.untangle.node.token.Chunk;
import com.untangle.node.token.ParseException;
import com.untangle.node.token.ParseResult;
import com.untangle.node.token.PassThruToken;
import com.untangle.node.token.Token;
import com.untangle.node.util.ASCIIUtil;
import com.untangle.uvm.vnet.TCPSession;






/**
 * ...name says it all...
 */
class SmtpClientParser extends SmtpParser
{
    private final Logger m_logger = Logger.getLogger(SmtpClientParser.class);

    private static final int MAX_COMMAND_LINE_SZ = 1024*2;

    private enum SmtpClientState {
        COMMAND,
        HEADERS,
        BODY
    };

    //Transient
    private SmtpClientState m_state = SmtpClientState.COMMAND;
    private ScannerAndAccumulator m_sac;


    SmtpClientParser(TCPSession session, SmtpCasing parent, CasingSessionTracker tracker)
    {
        super(session, parent, tracker, true);

        m_logger.debug("Created");
        lineBuffering(false);
    }



    @Override
    @SuppressWarnings("fallthrough")
    protected ParseResult doParse(ByteBuffer buf) throws FatalMailParseException {

        //===============================================
        // This method is very procedural, to make
        // cleanup after errors easier.  In general,
        // there are a lot of helper functions called
        // which return true/false.  Most of these operate
        // on the ScannerAndAccumulator data member.  If false
        // is returned from these methods, this method
        // performs cleanup and enters passthru mode.
        //   -wrs 7/05
        //

        List<Token> toks = new LinkedList<Token>();
        boolean done = false;

        while(!done && buf.hasRemaining()) {
            if (m_logger.isDebugEnabled()) {
                m_logger.debug("Draining tokens from buffer (" + toks.size() +
                               " tokens so far)");
            }

            //Re-check passthru, in case we hit it while looping in
            //this method.
            if(isPassthru()) {
                if(buf.hasRemaining()) {
                    toks.add(new Chunk(buf));
                }
                return new ParseResult(toks);
            }

            switch(m_state) {

                //==================================================
            case COMMAND:

                if(getSmtpCasing().isInSASLLogin()) {
                    m_logger.debug("In SASL Exchange");
                    SmtpSASLObserver observer =
                        getSmtpCasing().getSASLObserver();
                    ByteBuffer dup = buf.duplicate();
                    switch(observer.clientData(buf)) {
                    case EXCHANGE_COMPLETE:
                        m_logger.debug("SASL Exchange complete");
                        getSmtpCasing().closeSASLExchange();
                        //fallthrough ?? XXX
                    case IN_PROGRESS:
                        //There should not be any extra bytes
                        //left with "in progress", but what the hell
                        dup.limit(buf.position());
                        toks.add(new SASLExchangeToken(dup));
                        break;
                    case RECOMMEND_PASSTHRU:
                        m_logger.debug("Entering passthru on advice of SASLObserver");
                        declarePassthru();
                        toks.add(PassThruToken.PASSTHRU);
                        toks.add(new Chunk(dup.slice()));
                        buf.position(buf.limit());
                        return new ParseResult(toks);
                    }
                    break;
                }

                if(findCrLf(buf) >= 0) {//BEGIN Complete Command
                    //Parse the next command.  If there is a parse error,
                    //pass along the original chunk
                    ByteBuffer dup = buf.duplicate();
                    Command cmd = null;
                    try {
                        cmd = CommandParser.parse(buf);
                    } catch(ParseException pe) {
                        //Duplicate the bad buffer
                        dup.limit(findCrLf(dup) + 2);
                        ByteBuffer badBuf = ByteBuffer.allocate(dup.remaining());
                        badBuf.put(dup);
                        badBuf.flip();
                        //Position the "real" buffer beyond the bad point.
                        buf.position(dup.position());

                        m_logger.warn("Exception parsing command line \"" +
                                      ASCIIUtil.bbToString(badBuf) + "\".  Pass to server and monitor response", pe);

                        cmd = new UnparsableCommand(badBuf);

                        getSessionTracker().commandReceived(cmd,
                                                            new CommandParseErrorResponseCallback(badBuf));

                        toks.add(cmd);
                        break;
                    }

                    //If we're here, we have a legitimate command
                    toks.add(cmd);


                    if(cmd.getType() == Command.CommandType.AUTH) {
                        m_logger.debug("Received an AUTH command (hiding details for privacy reasons)");
                        AUTHCommand authCmd = (AUTHCommand) cmd;
                        String mechName = authCmd.getMechanismName();
                        if(!getSmtpCasing().openSASLExchange(mechName)) {
                            if (m_logger.isDebugEnabled()) {
                                m_logger.debug("Unable to find SASLObserver for \"" +
                                               mechName + "\"");
                            }
                            declarePassthru();
                            toks.add(PassThruToken.PASSTHRU);
                            toks.add(new Chunk(buf));
                            return new ParseResult(toks, null);
                        }
                        else {
                            m_logger.debug("Opening SASL Exchange");
                        }

                        switch(getSmtpCasing().getSASLObserver().initialClientResponse(
                                                                                       authCmd.getInitialResponse())) {
                        case EXCHANGE_COMPLETE:
                            m_logger.debug("SASL Exchange complete");
                            getSmtpCasing().closeSASLExchange();
                            break;
                        case IN_PROGRESS:
                            break;//Nothing interesting to do
                        case RECOMMEND_PASSTHRU:
                            m_logger.debug("Entering passthru on advice of SASLObserver");
                            declarePassthru();
                            toks.add(PassThruToken.PASSTHRU);
                            toks.add(new Chunk(buf));
                            return new ParseResult(toks);
                        }
                        break;
                    }
                    else {
                        //This is broken off so we don't put folks
                        //passwords into the log
                        if (m_logger.isDebugEnabled()) {
                            m_logger.debug("Received command: " + cmd.toDebugString());
                        }
                    }

                    if(cmd.getType() == Command.CommandType.STARTTLS) {
                        m_logger.debug("Enqueue observer for response to STARTTLS, " +
                                       "to go into passthru if accepted");
                        getSessionTracker().commandReceived(cmd, new TLSResponseCallback());
                    }
                    else if(cmd.getType() == Command.CommandType.DATA) {
                        m_logger.debug("entering data transmission (DATA)");
                        if(!openSAC()) {
                            //Error opening the temp file.  The
                            //error has been reported and the temp file
                            //cleaned-up
                            m_logger.debug("Declare passthru as we cannot buffer MIME");
                            declarePassthru();
                            toks.add(PassThruToken.PASSTHRU);
                            toks.add(new Chunk(buf));
                            return new ParseResult(toks, null);
                        }
                        m_logger.debug("Change state to " +
                                       SmtpClientState.HEADERS + ".  Enqueue response handler in case DATA " +
                                       "command rejected (returning us to " + SmtpClientState.COMMAND + ")");
                        getSessionTracker().commandReceived(cmd, new DATAResponseCallback(m_sac));
                        changeState(SmtpClientState.HEADERS);
                        //Go back and start evaluating the header bytes.
                    }
                    else {
                        getSessionTracker().commandReceived(cmd);
                    }
                }//ENDOF Complete Command
                else {//BEGIN Not complete Command
                    //Check for attack
                    if(buf.remaining() > MAX_COMMAND_LINE_SZ) {
                        m_logger.debug("Line longer than " + MAX_COMMAND_LINE_SZ + " received without new line. " +
                                       "Assume tunneling (permitted) and declare passthru");
                        declarePassthru();
                        toks.add(PassThruToken.PASSTHRU);
                        toks.add(new Chunk(buf));
                        return new ParseResult(toks, null);
                    }
                    m_logger.debug("Command line does not end with CRLF.  Need more bytes");
                    done = true;
                }//ENDOF Not complete Command
                break;

                //==================================================
            case HEADERS:
                //Duplicate the buffer, in case we have a problem
                ByteBuffer dup = buf.duplicate();
                boolean endOfHeaders =
                    m_sac.scanner.processHeaders(buf, 1024*4);//TODO bscott a real value here

                //If we're here, we didn't get a line which was too long.  Write
                //what we have to disk.
                ByteBuffer dup2 = dup.duplicate();
                dup2.limit(buf.position());

                if(m_sac.scanner.isHeadersBlank()) {
                    m_logger.debug("Headers are blank");
                }
                else {
                    m_logger.debug("About to write the " +
                                   (endOfHeaders?"last":"next") + " " +
                                   dup2.remaining() + " header bytes to disk");
                }

                if(!m_sac.accumulator.addHeaderBytes(dup2, endOfHeaders)) {
                    m_logger.error("Unable to write header bytes to disk.  Enter passthru");
                    //TODO bscott THis still needs to be more gracefully unwound
                    //     in the stateful nodes
                    puntDuringHeaders(toks, dup);
                    return new ParseResult(toks, null);
                }

                if(endOfHeaders) {//BEGIN End of Headers
                    MIMEMessageHeaders headers = m_sac.accumulator.parseHeaders();
                    if(headers == null) {//BEGIN Header PArse Error
                        m_logger.error("Unable to parse headers.  This will be caught downstream");
                    }//ENDOF Header PArse Error

                    m_logger.debug("Adding the BeginMIMEToken");
                    getSessionTracker().beginMsgTransmission();
                    toks.add(new BeginMIMEToken(m_sac.accumulator,
                                                createMessageInfo(headers)));
                    m_sac.noLongerAccumulatorMaster();
                    changeState(SmtpClientState.BODY);
                    if(m_sac.scanner.isEmptyMessage()) {
                        m_logger.debug("Message blank.  Skip to reading commands");
                        toks.add(new ContinuedMIMEToken(m_sac.accumulator.createChunk(null, true)));
                        changeState(SmtpClientState.COMMAND);
                        m_sac = null;
                    }

                }//ENDOF End of Headers
                else {
                    m_logger.debug("Need more header bytes");
                    done = true;
                }
                break;

                //==================================================
            case BODY:
                ByteBuffer bodyBuf = ByteBuffer.allocate(buf.remaining());
                boolean bodyEnd = m_sac.scanner.processBody(buf, bodyBuf);
                bodyBuf.flip();
                MIMEAccumulator.MIMEChunk mimeChunk = null;
                if(bodyEnd) {
                    m_logger.debug("Found end of body");
                    mimeChunk = m_sac.accumulator.createChunk(bodyBuf, true);
                    m_logger.debug("Adding last MIME token with length: " + mimeChunk.getData().remaining());
                    m_sac = null;
                    changeState(SmtpClientState.COMMAND);
                }
                else {
                    mimeChunk = m_sac.accumulator.createChunk(bodyBuf, false);
                    m_logger.debug("Adding continued MIME token with length: " + mimeChunk.getData().remaining());
                    done = true;
                }
                toks.add(new ContinuedMIMEToken(mimeChunk));
                break;
            }
        }

        //Compact the buffer
        buf = compactIfNotEmpty(buf, MAX_COMMAND_LINE_SZ);

        if(buf == null) {
            m_logger.debug("returning ParseResult with " +
                           toks.size() + " tokens and a null buffer");
        }
        else {
            m_logger.debug("returning ParseResult with " +
                           toks.size() + " tokens and a buffer with " +
                           buf.remaining() + " remaining (" +
                           buf.position() + " to be seen on next invocation)");
        }
        return new ParseResult(toks, buf);
    }

    @Override
    public void handleFinalized() {
        super.handleFinalized();
        if(m_sac != null) {
            m_logger.debug("Unexpected finalized in state " + m_state);
            m_sac.accumulator.dispose();
            m_sac = null;
        }
    }

    private void changeState(SmtpClientState newState) {
        m_logger.debug("Change state " +
                       m_state + "->" + newState);
        m_state = newState;
    }


    /**
     * Callback if TLS starts
     */
    private void tlsStarting() {
        m_logger.debug("TLS Command accepted.  Enter passthru mode so as to not attempt to parse cyphertext");
        declarePassthru();//Inform the unparser of this state
    }

    //================ Inner Class =================

    /**
     * Callback registered with the CasingSessionTracker
     * for the response to the DATA command
     */
    class DATAResponseCallback
        implements CasingSessionTracker.ResponseAction {

        private ScannerAndAccumulator m_targetSAC;

        DATAResponseCallback(ScannerAndAccumulator sac) {
            m_targetSAC = sac;
        }

        public void response(int code) {
            if(code < 400) {
                m_logger.debug("DATA command accepted");
            }
            else {
                m_logger.debug("DATA command rejected");
                if(m_sac != null &&
                   m_targetSAC == m_sac &&
                   m_sac.isMasterOfAccumulator()) {

                    m_sac.accumulator.dispose();
                    m_sac = null;
                    changeState(SmtpClientState.COMMAND);
                }
                else {
                    m_logger.debug("DATA command rejected, yet we have moved on to a new transaction");
                }
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

    //================ Inner Class =================

    /**
     * Callback registered with the CasingSessionTracker
     * for the response to a command we could not parse.
     * If the response can be parsed, and it is an error,
     * we do not go into passthru.  If the response is positive,
     * then we go into passthru.
     */
    class CommandParseErrorResponseCallback
        implements CasingSessionTracker.ResponseAction {

        private String m_offendingCommand;

        CommandParseErrorResponseCallback(ByteBuffer bufWithOffendingLine) {
            m_offendingCommand = ASCIIUtil.bbToString(bufWithOffendingLine);
        }

        public void response(int code) {
            if(code < 300) {
                m_logger.error("Could not parse command line \"" +
                               m_offendingCommand + "\" yet accepted by server.  Parser error.  Enter passthru");
                declarePassthru();
            }
            else {
                m_logger.debug("Command \"" + m_offendingCommand + "\" unparsable, and rejected " +
                               "by server.  Do not enter passthru (assume errant client)");
            }
        }
    }

    /**
     * Open the MIMEAccumulator and Scanner (ScannerAndAccumulator).
     * If there was an error,
     * the ScannerAndAccumulator is not
     * set as a data member and any files/streams
     * are cleaned-up.
     *
     * @return false if there was an error creating the file.
     */
    private boolean openSAC() {
        try {
            m_sac = new ScannerAndAccumulator(
                                              new MIMEAccumulator(getPipeline()));
            return true;
        }
        catch(IOException ex) {
            m_logger.error("Exception creating MIME Accumulator", ex);
            return false;
        }
    }


    /**
     * Helper method to break-out the
     * creation of a MessageInfo
     */
    private MessageInfo createMessageInfo(MIMEMessageHeaders headers) {

        if(headers == null) {
            headers = new MIMEMessageHeaders();
        }

        MessageInfo ret = MessageInfoFactory.fromMIMEMessage(headers,
                                                             getSession().pipelineEndpoints(),
                                                             getSession().serverPort());
        //Add anyone from the transaction
        SmtpTransaction smtpTx = getSessionTracker().getCurrentTransaction();
        if(smtpTx == null) {
            m_logger.error("Transaction tracker returned null for current transaction");
        }
        else {
            //Transfer the FROM
            if(smtpTx.getFrom() != null && !smtpTx.getFrom().isNullAddress()) {
                ret.addAddress(AddressKind.ENVELOPE_FROM,
                               smtpTx.getFrom().getAddress(),
                               smtpTx.getFrom().getPersonal());
            }
            List<EmailAddress> txRcpts = smtpTx.getRecipients(false);
            for(EmailAddress addr : txRcpts) {
                if(addr.isNullAddress()) {
                    continue;
                }
                ret.addAddress(AddressKind.ENVELOPE_TO, addr.getAddress(), addr.getPersonal());
            }
        }
        return ret;
    }

    /**
     * This code was moved-out of the "parse" method as
     * it was repeated a few times.
     */
    private void puntDuringHeaders(List<Token> toks,
                                   ByteBuffer buf) {
        //Get any bytes trapped in the file
        ByteBuffer trapped = m_sac.accumulator.drainFileToByteBuffer();
        if(trapped == null) {
            m_logger.debug("Could not recover buffered header bytes");
        }
        else {
            m_logger.debug("Retreived " + trapped.remaining() + " bytes trapped in file");
        }
        //Nuke the accumulator
        m_sac.accumulator.dispose();
        m_sac = null;
        //Passthru
        declarePassthru();
        toks.add(PassThruToken.PASSTHRU);
        if(trapped != null && trapped.remaining() > 0) {
            toks.add(new Chunk(trapped));
        }
        toks.add(new Chunk(buf));
    }

    //=========================== Inner Class ===========================

    /**
     * Little class to associate the
     * MIMEAccumulator and the boundary scanner
     * as-one.
     */
    private class ScannerAndAccumulator {

        final MessageBoundaryScanner scanner;
        final MIMEAccumulator accumulator;
        private boolean m_isMasterOfAccumulator = true;

        ScannerAndAccumulator(MIMEAccumulator accumulator) {
            scanner = new MessageBoundaryScanner();
            this.accumulator = accumulator;
        }
        boolean isMasterOfAccumulator() {
            return m_isMasterOfAccumulator;
        }
        void noLongerAccumulatorMaster() {
            m_isMasterOfAccumulator = false;
        }
    }
}
