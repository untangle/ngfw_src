/**
 * $Id$
 */
package com.untangle.app.smtp;

import static com.untangle.uvm.util.BufferUtil.findCrLf;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import javax.mail.internet.InternetAddress;

import javax.mail.internet.InternetHeaders;
import org.apache.log4j.Logger;

import com.untangle.app.smtp.mime.HeaderNames;
import com.untangle.app.smtp.mime.MIMEAccumulator;
import com.untangle.app.smtp.mime.MIMEUtil;
import com.untangle.uvm.vnet.ChunkToken;
import com.untangle.uvm.vnet.Token;
import com.untangle.uvm.vnet.ReleaseToken;
import com.untangle.uvm.util.AsciiUtil;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.vnet.AppTCPSession;
import com.untangle.uvm.vnet.AbstractEventHandler;

/**
 * SMTP client parser
 */
class SmtpClientParserEventHandler extends AbstractEventHandler
{
    protected static final String SHARED_STATE_KEY = "SMTP-shared-state";

    private static final String CLIENT_PARSER_STATE_KEY = "SMTP-client-parser-state";

    private static final Logger logger = Logger.getLogger(SmtpClientParserEventHandler.class);

    private static final int MAX_COMMAND_LINE_SZ = 1024 * 2;

    private enum SmtpClientState { COMMAND, BODY, HEADERS };

    /**
     * SMTP parser event handler session state.
     */
    private class SmtpClientParserEventHandlerSessionState
    {
        protected SmtpClientState currentState = SmtpClientState.COMMAND;
        protected ScannerAndAccumulator sac;
    }

    /**
     * Initialize instance of SmtpClientParserEventHandler.
     * @return instance of SmtpClientParserEventHandler
     */
    public SmtpClientParserEventHandler()
    {
        super();
    }

    /**
     * Process new TCP session.
     * @param session AppTCPSession to handle.
     */
    @Override
    public void handleTCPNewSession( AppTCPSession session )
    {
        SmtpClientParserEventHandlerSessionState state = new SmtpClientParserEventHandlerSessionState();
        session.attach( CLIENT_PARSER_STATE_KEY, state );

        SmtpSharedState clientSideSharedState = new SmtpSharedState();
        session.attach( SHARED_STATE_KEY, clientSideSharedState );
    }

    /**
     * Process chunk of data in TCP session from client.
     * @param session AppTCPSession to handle.
     * @param data    ByteBuffer to process into.
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

        parse( session, data, false, false );
    }

    /**
     * Process chunk of data in TCP session from server.
     * @param session AppTCPSession to handle.
     * @param data    ByteBuffer to process into.
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
     * Process object TCP session from client.
     * @param session AppTcpSession to handle.
     * @param obj     Object to initialize from session.
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
     * Process object TCP session from server.
     * @param session AppTcpSession to handle.
     * @param obj     Object to initialize from session.
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

        logger.warn("Received object but expected data.");
        throw new RuntimeException("Received object but expected data.");
    }

    /**
     * Process end of TCP session from client.
     * @param session AppTcpSession to handle.
     * @param data    Data to process from session.
     */
    @Override
    public void handleTCPClientDataEnd( AppTCPSession session, ByteBuffer data )
    {
        parse( session, data, false, true);
    }

    /**
     * Process end of TCP session from server.
     * @param session AppTcpSession to handle.
     * @param data    Data to process from session.
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
     * Shutdown TCP client.
     * @param session AppTcpSession to shut down.
     */
    @Override
    public void handleTCPClientFIN( AppTCPSession session )
    {
        session.shutdownServer();
    }

    /**
     * Shutdown TCP server.
     * @param session AppTcpSession to shut down.
     */
    @Override
    public void handleTCPServerFIN( AppTCPSession session )
    {
        logger.warn("Received unexpected event.");
        throw new RuntimeException("Received unexpected event.");
    }

    /**
     * Process TCP sessionin finalized state.
     * @param session AppTcpSession to shut down.
     */
    @Override
    public void handleTCPFinalized( AppTCPSession session )
    {
        SmtpClientParserEventHandlerSessionState state = (SmtpClientParserEventHandlerSessionState) session.attachment( CLIENT_PARSER_STATE_KEY );

        if ( state.sac != null ) {
            logger.debug("Unexpected finalized in state " + state.currentState);
            state.sac.accumulator.dispose();
            state.sac = null;
        }
    }
    
    /**
     * Handle AppTcpSession.
     * @param session AppTCPSession to handle.
     * @param data    Data to parse.
     * @param s2c     If true, server to client.  Client to server otherwise.
     * @param last    If true, last part of session.
     */
    private void parse( AppTCPSession session, ByteBuffer data, boolean s2c, boolean last )
    {
        ByteBuffer buf = data;
        ByteBuffer dup = buf.duplicate();
        try {
            if (last) {
                parseEnd( session, buf );
            } else {
                parse( session, buf );
            }
        } catch (Throwable exn) {
            String sessionEndpoints = "[" +
                session.getProtocol() + " : " + 
                session.getClientAddr() + ":" + session.getClientPort() + " -> " +
                session.getServerAddr() + ":" + session.getServerPort() + "]";

            session.release();

            if ( s2c ) {
                session.sendObjectToClient( new ReleaseToken() );
                session.sendDataToClient( dup );
            } else {
                session.sendObjectToServer( new ReleaseToken() );
                session.sendDataToServer( dup );
            }
            return;
        }
    }
    
    /**
     * Perform parsing of session.
     * @param session AppTCPSession to process.
     * @param buf     ByteBuffer to process.
     */
    @SuppressWarnings("fallthrough")
    protected void doParse( AppTCPSession session, ByteBuffer buf )
    {
        SmtpClientParserEventHandlerSessionState state = (SmtpClientParserEventHandlerSessionState) session.attachment( CLIENT_PARSER_STATE_KEY );
        SmtpSharedState clientSideSharedState = (SmtpSharedState) session.attachment( SHARED_STATE_KEY );

        // ===============================================
        // In general, there are a lot of helper functions called which return true/false. 
        // Most of these operate on the ScannerAndAccumulator data member. If false
        // is returned from these methods, this method performs cleanup and enters passthru mode.
        //

        List<Token> toks = new LinkedList<>();
        boolean done = false;

        while (!done && buf.hasRemaining()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Draining tokens from buffer (" + toks.size() + " tokens so far)");
            }

            // Re-check passthru, in case we hit it while looping in
            // this method.
            if ( isPassthru( session ) ) {
                if (buf.hasRemaining()) {
                    toks.add(new ChunkToken(buf));
                }
                for ( Token tok : toks )
                    session.sendObjectToServer( tok );
                return;
            }

            switch ( state.currentState ) {

                // ==================================================
            case COMMAND:

                if ( clientSideSharedState.isInSASLLogin() ) {
                    logger.debug("In SASL Exchange");
                    SmtpSASLObserver observer = clientSideSharedState.getSASLObserver();
                    ByteBuffer dup = buf.duplicate();
                    switch (observer.clientData(buf)) {
                    case EXCHANGE_COMPLETE:
                        logger.debug("SASL Exchange complete");
                        clientSideSharedState.closeSASLExchange();
                        // fallthrough ?? XXX
                    case IN_PROGRESS:
                        // There should not be any extra bytes left with "in progress"
                        dup.limit(buf.position());
                        toks.add(new SASLExchangeToken(dup));
                        break;
                    case RECOMMEND_PASSTHRU:
                        logger.debug("Entering passthru on advice of SASLObserver");
                        declarePassthru( session );
                        toks.add( PassThruToken.PASSTHRU );
                        toks.add( new ChunkToken( dup.slice() ) );
                        buf.position( buf.limit() );
                        for ( Token tok : toks )
                            session.sendObjectToServer( tok );
                        return;
                    }
                    break;
                }

                if (findCrLf(buf) >= 0) {// BEGIN Complete Command
                    // Parse the next command. If there is a parse error, pass along the original chunk
                    ByteBuffer dup = buf.duplicate();
                    Command cmd = null;
                    try {
                        cmd = CommandParser.parse(buf);
                    } catch (Exception pe) {
                        // Duplicate the bad buffer
                        dup.limit(findCrLf(dup) + 2);
                        ByteBuffer badBuf = ByteBuffer.allocate(dup.remaining());
                        badBuf.put(dup);
                        badBuf.flip();
                        // Position the "real" buffer beyond the bad point.
                        buf.position(dup.position());

                        logger.warn("Exception parsing command line \"" + AsciiUtil.bbToString(badBuf) + "\".  Pass to server and monitor response", pe);

                        cmd = new UnparsableCommand(badBuf);

                        clientSideSharedState.commandReceived(cmd, new CommandParseErrorResponseCallback( session, badBuf ));

                        toks.add(cmd);
                        break;
                    }

                    // If we're here, we have a legitimate command
                    toks.add(cmd);

                    if (cmd.getType() == CommandType.AUTH) {
                        logger.debug("Received an AUTH command (hiding details for privacy reasons)");
                        AUTHCommand authCmd = (AUTHCommand) cmd;
                        String mechName = authCmd.getMechanismName();
                        if ( ! clientSideSharedState.openSASLExchange(mechName) ) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Unable to find SASLObserver for \"" + mechName + "\"");
                            }
                            declarePassthru( session );
                            toks.add( PassThruToken.PASSTHRU );
                            toks.add(  new ChunkToken( buf ) );

                            for ( Token tok : toks )
                                session.sendObjectToServer( tok );
                            return;
                        } else {
                            logger.debug("Opening SASL Exchange");
                        }

                        switch ( clientSideSharedState.getSASLObserver().initialClientResponse(authCmd.getInitialResponse()) ) {
                        case EXCHANGE_COMPLETE:
                            logger.debug("SASL Exchange complete");
                            clientSideSharedState.closeSASLExchange();
                            break;
                        case IN_PROGRESS:
                            break;// Nothing interesting to do
                        case RECOMMEND_PASSTHRU:
                            logger.debug("Entering passthru on advice of SASLObserver");
                            declarePassthru( session );
                            toks.add(PassThruToken.PASSTHRU);
                            toks.add(new ChunkToken(buf));

                            for ( Token tok : toks )
                                session.sendObjectToServer( tok );
                            return;
                        }
                        break;
                    } else {
                        // This is broken off so we don't put folks
                        // passwords into the log
                        if (logger.isDebugEnabled()) {
                            logger.debug("Received command: " + cmd.toDebugString());
                        }
                    }

                    if (cmd.getType() == CommandType.STARTTLS) {
                        if (session.globalAttachment(AppTCPSession.KEY_SSL_INSPECTOR_SESSION_INSPECT) != null) {
                            logger.debug("Skipping STARTTLS passthru because the SSL Inspector is active");
                        }
                        else {
                            logger.debug("Enqueue observer for response to STARTTLS, " + "to go into passthru if accepted");
                            clientSideSharedState.commandReceived( cmd, new TLSResponseCallback( session ) );
                        }
                    } else if (cmd.getType() == CommandType.DATA) {
                        logger.debug("entering data transmission (DATA)");
                        if ( ! openSAC( session ) ) {
                            // Error opening the temp file. The
                            // error has been reported and the temp file
                            // cleaned-up
                            logger.debug("Declare passthru as we cannot buffer MIME");
                            declarePassthru( session );
                            toks.add(PassThruToken.PASSTHRU);
                            toks.add(new ChunkToken(buf));
                            for ( Token tok : toks )
                                session.sendObjectToServer( tok );
                            return;
                        }
                        logger.debug("Change state to " + SmtpClientState.HEADERS
                                     + ".  Enqueue response handler in case DATA "
                                     + "command rejected (returning us to " + SmtpClientState.COMMAND + ")");
                        clientSideSharedState.commandReceived(cmd, new DATAResponseCallback( session, state.sac ));
                        state.currentState = SmtpClientState.HEADERS;
                        // Go back and start evaluating the header bytes.
                    } else {
                        clientSideSharedState.commandReceived(cmd);
                    }
                }// ENDOF Complete Command
                else {// BEGIN Not complete Command
                    // Check for attack
                    if (buf.remaining() > MAX_COMMAND_LINE_SZ) {
                        logger.debug("Line longer than " + MAX_COMMAND_LINE_SZ + " received without new line. "
                                     + "Assume tunneling (permitted) and declare passthru");
                        declarePassthru( session );
                        toks.add(PassThruToken.PASSTHRU);
                        toks.add(new ChunkToken(buf));
                        for ( Token tok : toks )
                            session.sendObjectToServer( tok );
                        return;
                    }
                    logger.debug("Command line does not end with CRLF.  Need more bytes");
                    done = true;
                }// ENDOF Not complete Command
                break;

                // ==================================================
            case HEADERS:
                // Duplicate the buffer, in case we have a problem
                ByteBuffer dup = buf.duplicate();
                boolean endOfHeaders = state.sac.scanner.processHeaders(buf, 1024 * 4);

                // If we're here, we didn't get a line which was too long.
                // Write what we have to disk.
                ByteBuffer dup2 = dup.duplicate();
                dup2.limit(buf.position());

                if ( state.sac.scanner.isHeadersBlank() ) {
                    logger.debug("Headers are blank");
                } else {
                    logger.debug("About to write the " + (endOfHeaders ? "last" : "next") + " "
                                 + dup2.remaining() + " header bytes to disk");
                }

                if (! state.sac.accumulator.addHeaderBytes(dup2, endOfHeaders) ) {
                    logger.error("Unable to write header bytes to disk.  Enter passthru");
                    puntDuringHeaders( session, toks, dup );
                    for ( Token tok : toks )
                        session.sendObjectToServer( tok );
                    return;
                }

                if (endOfHeaders) {// BEGIN End of Headers
                    InternetHeaders headers = state.sac.accumulator.parseHeaders();
                    if (headers == null) {// BEGIN Header PArse Error
                        logger.error("Unable to parse headers.  This will be caught downstream");
                    }// ENDOF Header PArse Error

                    logger.debug("Adding the BeginMIMEToken");
                    clientSideSharedState.beginMsgTransmission();
                    toks.add(new BeginMIMEToken( state.sac.accumulator, createSmtpMessageEvent( session, headers )) );
                    state.sac.noLongerAccumulatorMaster();
                    state.currentState = SmtpClientState.BODY;
                    if ( state.sac.scanner.isEmptyMessage() ) {
                        logger.debug("Message blank.  Skip to reading commands");
                        toks.add( new ContinuedMIMEToken( state.sac.accumulator.createChunkToken(null, true) ) );
                        state.currentState = SmtpClientState.COMMAND;
                        state.sac = null;
                    }

                }// ENDOF End of Headers
                else {
                    logger.debug("Need more header bytes");
                    done = true;
                }
                break;

                // ==================================================
            case BODY:
                ByteBuffer bodyBuf = ByteBuffer.allocate(buf.remaining());
                boolean bodyEnd = state.sac.scanner.processBody(buf, bodyBuf);
                bodyBuf.flip();
                MIMEAccumulator.MIMEChunkToken mimeChunkToken = null;
                if (bodyEnd) {
                    logger.debug("Found end of body");
                    mimeChunkToken = state.sac.accumulator.createChunkToken(bodyBuf, true);
                    logger.debug("Adding last MIME token with length: " + mimeChunkToken.getData().remaining());
                    state.sac = null;
                    state.currentState = SmtpClientState.COMMAND;
                } else {
                    mimeChunkToken = state.sac.accumulator.createChunkToken(bodyBuf, false);
                    logger.debug("Adding continued MIME token with length: " + mimeChunkToken.getData().remaining());
                    done = true;
                }
                toks.add(new ContinuedMIMEToken(mimeChunkToken));
                break;
            }
        }

        // Compact the buffer
        buf = compactIfNotEmpty(buf, MAX_COMMAND_LINE_SZ);

        if (buf == null) {
            logger.debug("sending " + toks.size() + " tokens and setting a null buffer");
        } else {
            logger.debug("sending " + toks.size() + " tokens and setting a buffer with " + buf.remaining() + " remaining (" + buf.position() + " to be seen on next invocation)");
        }
        for ( Token tok : toks )
            session.sendObjectToServer( tok );
        session.setClientBuffer( buf );
        return;
    }

    /**
     * Parse session.
     * @param session AppTCPSession to parse.
     * @param buf     ByteBuffer to parse.
     */
    public void parse( AppTCPSession session, ByteBuffer buf )
    {
        try {
            if ( isPassthru( session ) ) {
                session.sendObjectToServer( new ChunkToken(buf) );
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
     * Handle end of session.
     * @param session AppTcpSession to parse.
     * @param buf     ByteBuffer to parse.
     */
    public final void parseEnd( AppTCPSession session, ByteBuffer buf )
    {
        if ( buf.hasRemaining() ) {
            session.sendObjectToServer( new ChunkToken(buf) );
            return;
        }
        return;
    }

    /**
     * Is the casing currently in passthru mode
     * @param session AppTCPSession to check.
     * @return true if passthru, otherwise false.
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

    /**
     * Callback if TLS starts
     * @param session AppTCPSession to set.
     */
    private void tlsStarting( AppTCPSession session )
    {
        logger.debug("TLS Command accepted.  Enter passthru mode so as to not attempt to parse cyphertext");
        declarePassthru( session );// Inform the unparser of this state
    }

    /**
     * Helper which compacts (and possibly expands) the buffer if anything remains. Otherwise, just returns null.
     * @param buf ByteBuffer to compact,
     * @param maxSz Maximum size to compact
     * @return ByteBuffer of compacted.
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
    
    // ================ Inner Class =================

    /**
     * Callback registered with the SmtpSharedState for the response to the DATA command
     */
    class DATAResponseCallback implements SmtpSharedState.ResponseAction
    {
        private AppTCPSession session;
        private ScannerAndAccumulator targetSAC;

        /**
         * Initialize DATAResponseCallback from session.
         * @param  session AppTcpSession to use.
         * @param  sac     ScannerAndAccumulator to use.
         * @return         Instance of DATAResponseCallback
         */
        public DATAResponseCallback( AppTCPSession session, ScannerAndAccumulator sac)
        {
            this.session = session;
            this.targetSAC = sac;
        }

        /**
         * Handle response.
         * @param code SMTP code to process.
         */
        public void response(int code)
        {
            SmtpClientParserEventHandlerSessionState state = (SmtpClientParserEventHandlerSessionState) session.attachment( CLIENT_PARSER_STATE_KEY );

            if (code < 400) {
                logger.debug("DATA command accepted");
            } else {
                logger.debug("DATA command rejected");
                if ( state.sac != null && targetSAC == state.sac && state.sac.isMasterOfAccumulator() ) {

                    state.sac.accumulator.dispose();
                    state.sac = null;
                    state.currentState = SmtpClientState.COMMAND;
                } else {
                    logger.debug("DATA command rejected, yet we have moved on to a new transaction");
                }
            }
        }
    }

    /**
     * Callback registered with the SmtpSharedState for the response to the STARTTLS command
     */
    class TLSResponseCallback implements SmtpSharedState.ResponseAction
    {
        private AppTCPSession session;

        /**
         * Initialize TLSResponseCallback.
         * @param  session AppTCPSession to use.
         * @return         Instance of TLSResponseCallback.
         */
        protected TLSResponseCallback( AppTCPSession session )
        {
            this.session = session;
        }

        /**
         * Handle response.
         * @param code SMTP code to process.
         */
        public void response( int code )
        {
            if (code < 300) {
                tlsStarting( session );
            } else {
                logger.debug("STARTTLS command rejected.  Do not go into passthru");
            }
        }
    }

    /**
     * Callback registered with the SmtpSharedState for the response to a command we could not parse. If the
     * response can be parsed, and it is an error, we do not go into passthru. If the response is positive, then we go
     * into passthru.
     */
    class CommandParseErrorResponseCallback implements SmtpSharedState.ResponseAction
    {
        private AppTCPSession session;
        private String offendingCommand;

        /**
         * Initialize CommandParseErrorResponseCallback
         * @param session AppTCPSession to use.
         * @param bufWithOffendingLine ByteBuffer with offending line.
         * @return Instance of CommandParseErrorResponseCallback.
         */
        CommandParseErrorResponseCallback( AppTCPSession session, ByteBuffer bufWithOffendingLine )
        {
            this.session = session;
            offendingCommand = AsciiUtil.bbToString(bufWithOffendingLine);
        }

        /**
         * Handle SMTP response.
         * @param code SMTP code to process.
         */
        public void response(int code)
        {
            if (code < 300) {
                logger.error("Could not parse command line \"" + offendingCommand
                             + "\" yet accepted by server.  Parser error.  Enter passthru");
                declarePassthru( session );
            } else {
                logger.debug("Command \"" + offendingCommand + "\" unparsable, and rejected "
                             + "by server.  Do not enter passthru (assume errant client)");
            }
        }
    }

    /**
     * Open the MIMEAccumulator and Scanner (ScannerAndAccumulator). If there was an error, the ScannerAndAccumulator is
     * not set as a data member and any files/streams are cleaned-up.
     *
     * @param session AppTCPSession to process.
     * @return false if there was an error creating the file.
     */
    private boolean openSAC( AppTCPSession session )
    {
        SmtpClientParserEventHandlerSessionState state = (SmtpClientParserEventHandlerSessionState) session.attachment( CLIENT_PARSER_STATE_KEY );

        try {
            state.sac = new ScannerAndAccumulator( new MIMEAccumulator( session ) );
            return true;
        } catch (IOException ex) {
            logger.error("Exception creating MIME Accumulator", ex);
            return false;
        }
    }

    /**
     * Add recipients ot header
     * @param  rcpts         Array of recipient addresses.
     * @param  ret           SmtpMessageEvent to process.
     * @param  recipientType AddressKind of recipients.
     * @return               true of has recipient.
     */
    private boolean addRecipientsFromHeader(String[] rcpts, SmtpMessageEvent ret, AddressKind recipientType)
    {
        boolean hasRecipient = false;
        if (rcpts != null) {
            try {
                for (String addr : rcpts) {
                    InternetAddress[] iaList = InternetAddress.parseHeader(addr, false);
                    for (InternetAddress ia : iaList) {
                        ret.addAddress(recipientType, ia.getAddress(), ia.getPersonal());
                        hasRecipient = true;
                    }
                }
            } catch (Exception e) {
                ret.addAddress(recipientType, "Illegal_address", "");
                logger.error(e);
            }
        }
        return hasRecipient;
    }
    
    /**
     * Helper method to break-out the creation of a SmtpMessageEvent
     * @param session AppTCPSession to handle.
     * @param headers InternetHeaders to handle.
     * @return SmtpMessageEvent.
     */
    private SmtpMessageEvent createSmtpMessageEvent( AppTCPSession session, InternetHeaders headers )
    {
        SmtpClientParserEventHandlerSessionState state = (SmtpClientParserEventHandlerSessionState) session.attachment( CLIENT_PARSER_STATE_KEY );
        SmtpSharedState clientSideSharedState = (SmtpSharedState) session.attachment( SHARED_STATE_KEY );

        if (headers == null) {
            return new SmtpMessageEvent( session.sessionEvent(), "" );
        }

        SmtpMessageEvent ret = new SmtpMessageEvent( session.sessionEvent(),  "" );

        ret.setSubject(headers.getHeader(HeaderNames.SUBJECT, ""));
        // Drain all TO and CC
        String[] toRcpts = headers.getHeader(HeaderNames.TO);
        String[] ccRcpts = headers.getHeader(HeaderNames.CC);

        boolean hasFrom = false;
        boolean hasTo = addRecipientsFromHeader(toRcpts, ret, AddressKind.TO);
        hasTo = hasTo || addRecipientsFromHeader(ccRcpts, ret, AddressKind.CC);

        try {
            // Drain FROM
            String from = headers.getHeader(HeaderNames.FROM, "");
            if (from != null) {
                InternetAddress ia = new InternetAddress(from);
                ret.addAddress(AddressKind.FROM, ia.getAddress(), ia.getPersonal());
                hasFrom = true;
            }
        } catch (Exception e) {
            ret.addAddress(AddressKind.FROM, "Illegal_address", "");
            logger.error(e);
        }
        UvmContextFactory.context().logEvent(ret);

        // Add anyone from the transaction
        SmtpTransaction smtpTx = clientSideSharedState.getCurrentTransaction();
        if (smtpTx == null) {
            logger.error("Transaction tracker returned null for current transaction");
        } else {
            // Transfer the FROM
            if (smtpTx.getFrom() != null && !MIMEUtil.isNullAddress(smtpTx.getFrom())) {
                ret.addAddress(AddressKind.ENVELOPE_FROM, smtpTx.getFrom().getAddress(), smtpTx.getFrom().getPersonal());
                if (!hasFrom) {
                    // needed in order to show up in logs even if the headers do not contain "FROM"
                    ret.addAddress(AddressKind.FROM, smtpTx.getFrom().getAddress(), smtpTx.getFrom().getPersonal());
                }
            }
            List<InternetAddress> txRcpts = smtpTx.getRecipients(false);
            for (InternetAddress addr : txRcpts) {
                if (MIMEUtil.isNullAddress(addr)) {
                    continue;
                }
                ret.addAddress(AddressKind.ENVELOPE_TO, addr.getAddress(), addr.getPersonal());
                if (!hasTo) {
                    // needed in order to show up in logs even if the headers do not contain "TO" or "CC"
                    ret.addAddress(AddressKind.TO, addr.getAddress(), addr.getPersonal());
                }
            }
        }
        return ret;
    }

    /**
     * This code was moved-out of the "parse" method as it was repeated a few times.
     * @param session AppTCPSession to handle.
     * @param toks List of Token to handle.
     * @param buf ByteBuffer to handle. 
     */
    private void puntDuringHeaders( AppTCPSession session, List<Token> toks, ByteBuffer buf )
    {
        SmtpClientParserEventHandlerSessionState state = (SmtpClientParserEventHandlerSessionState) session.attachment( CLIENT_PARSER_STATE_KEY );

        // Get any bytes trapped in the file
        ByteBuffer trapped = state.sac.accumulator.drainFileToByteBuffer();
        if (trapped == null) {
            logger.debug("Could not recover buffered header bytes");
        } else {
            logger.debug("Retreived " + trapped.remaining() + " bytes trapped in file");
        }
        // Nuke the accumulator
        state.sac.accumulator.dispose();
        state.sac = null;
        // Passthru
        declarePassthru( session );
        toks.add( PassThruToken.PASSTHRU );
        if (trapped != null && trapped.remaining() > 0) {
            toks.add(new ChunkToken(trapped));
        }
        toks.add(new ChunkToken(buf));
    }

    /**
     * Little class to associate the MIMEAccumulator and the boundary scanner as-one.
     */
    private class ScannerAndAccumulator
    {
        final MessageBoundaryScanner scanner;
        final MIMEAccumulator accumulator;
        private boolean isMasterOfAccumulator = true;

        /**
         * Initialize ScannerAndAccumulator
         * @param accumulator MIMEAccumulator to handle.
         */
        ScannerAndAccumulator(MIMEAccumulator accumulator)
        {
            scanner = new MessageBoundaryScanner();
            this.accumulator = accumulator;
        }

        /**
         * Determine if this is a master of accumulator.
         * @return true of master of accumulator.  False of not.
         */
        boolean isMasterOfAccumulator()
        {
            return isMasterOfAccumulator;
        }

        /**
         * Disable accumlator master.
         */
        void noLongerAccumulatorMaster()
        {
            isMasterOfAccumulator = false;
        }
    }
}
