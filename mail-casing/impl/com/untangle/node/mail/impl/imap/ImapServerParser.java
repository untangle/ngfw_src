/**
 * $Id$
 */
package com.untangle.node.mail.impl.imap;

import static com.untangle.node.util.Ascii.OPEN_BRACE_B;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import com.untangle.node.mail.papi.AddressKind;
import com.untangle.node.mail.papi.ContinuedMIMEToken;
import com.untangle.node.mail.papi.MIMEAccumulator;
import com.untangle.node.mail.papi.MessageBoundaryScanner;
import com.untangle.node.mail.papi.MessageInfo;
import com.untangle.node.mail.papi.MessageInfoFactory;
import com.untangle.node.mail.papi.imap.BeginImapMIMEToken;
import com.untangle.node.mail.papi.imap.ImapChunk;
import com.untangle.node.mail.papi.imap.UnparsableMIMEChunk;
import com.untangle.node.mime.MIMEMessageHeaders;
import com.untangle.node.token.Chunk;
import com.untangle.node.token.ParseResult;
import com.untangle.node.token.PassThruToken;
import com.untangle.node.token.Token;
import com.untangle.node.util.UtLogger;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.vnet.NodeTCPSession;

/**
 * IMAP Server Parser
 */
public class ImapServerParser extends ImapParser
{
    /**
     * State of the parser (*outer* parser)
     */
    private enum ISPState {
        SCANNING,
        DRAINING_HEADERS,
        DRAINING_BODY,
        DRAINING_HOSED,
    };

    private final UtLogger m_logger =
        new UtLogger(ImapServerParser.class);

    private ImapBodyScanner m_msgBoundaryScanner;
    private ISPState m_state = ISPState.SCANNING;
    private MessageGrabber m_msgGrabber;

    ImapServerParser(NodeTCPSession session,
                     ImapCasing parent) {

        super(session, parent, false);
        lineBuffering(false); // XXX line buffering

        m_msgBoundaryScanner = new ImapBodyScanner();

        m_logger.debug("Created");
    }


    @Override
    protected ParseResult doParse(ByteBuffer buf) {

        List<Token> toks = new LinkedList<Token>();

        //Get SessionMonitor to inspect bytes.  Its OK that we do not
        //align on token boundaries (as we do with the ClientParser),
        //because the tokenizing for the message body will cause
        //the return (in the read buffer) of any word-fragments.
        if(getImapCasing().getSessionMonitor().bytesFromServer(buf.duplicate())) {
            m_logger.debug("Declare passthru as-per advice of SessionMonitor");
            declarePassthru();
            toks.add(PassThruToken.PASSTHRU);
            toks.add(new Chunk(buf));
            return new ParseResult(toks, null);
        }

        ByteBuffer dup = null;


        while(buf.hasRemaining()) {
            switch(m_state) {
                //===================================================
            case SCANNING:
                dup = buf.duplicate();
                if(m_msgBoundaryScanner.scanForMsgState(buf)) {
                    m_logger.debug("Found message boundary start. Octet count: ",
                                   m_msgBoundaryScanner.getMessageOctetCount());

                    //First off, we need to put into the returned token
                    //list a Chunk with the bytes which were NOT the
                    //message
                    dup.limit(buf.position());
                    rewindLiteral(dup);
                    m_logger.debug("Adding protocol chunk of length ", dup.remaining());
                    toks.add(new ImapChunk(dup));

                    //Open-up the message grabber
                    if(!openMessageGrabber(m_msgBoundaryScanner.getMessageOctetCount())) {
                        m_logger.warn("Message will be bypassed because of error opening accumulator");
                        changeParserState(ISPState.DRAINING_HOSED);
                    }
                    else {
                        changeParserState(ISPState.DRAINING_HEADERS);
                    }
                    break;
                }
                else {
                    m_logger.debug("We need more data in SCANNING state");
                    dup.limit(buf.position());
                    m_logger.debug("Adding protocol chunk of length ", dup.remaining());
                    toks.add(new ImapChunk(dup));
                    return returnWithCompactedBuffer(buf, toks);
                }

                //===================================================
            case DRAINING_HEADERS:
                //Determine how much of this buffer we will consider as
                //message data
                dup = getNextBodyChunkBuffer(buf);

                boolean foundEndOfHeaders =
                    m_msgGrabber.scanner.processHeaders(dup, 1024*4);//TODO bscott a real value here

                //Adjust buffers, such that the HeaderBytes are accounted-for
                //in "dup" and the original buffer is advanced past what we
                //accounted-for in "dup"
                int headersEnd = dup.position();
                dup.limit(headersEnd);
                dup.position(buf.position());
                buf.position(headersEnd);

                //Decrement the amount of message "read"
                m_msgGrabber.decrementMsgRemaining(dup.remaining());

                if(m_msgGrabber.scanner.isHeadersBlank()) {
                    m_logger.debug("Headers are blank. ",
                                   m_msgGrabber.getMsgRemaining(),
                                   " msg bytes remain");
                }
                else {
                    m_logger.debug("About to write the ",
                                   (foundEndOfHeaders?"last":"next"),
                                   " ",
                                   dup.remaining(),
                                   " header bytes to disk. ",
                                   m_msgGrabber.getMsgRemaining(),
                                   " msg bytes remain");
                }

                //Write what we have to disk.
                if(!m_msgGrabber.accumulator.addHeaderBytes(dup, foundEndOfHeaders)) {
                    m_logger.error("Unable to write header bytes to disk.  Punt on this message");

                    //Grab anything trapped thus far in the file (if we can
                    recoverTrappedHeaderBytes(toks);

                    //Add the chunk we could not write to file
                    toks.add(new UnparsableMIMEChunk(dup));

                    if(m_msgGrabber.hasMsgRemaining()) {
                        changeParserState(ISPState.DRAINING_HOSED);
                    }
                    else {
                        m_msgGrabber = null;
                        changeParserState(ISPState.SCANNING);
                    }
                    break;
                }

                if(foundEndOfHeaders) {//BEGIN End of Headers
                    MIMEMessageHeaders headers = m_msgGrabber.accumulator.parseHeaders();
                    if(headers == null) {//BEGIN Header PArse Error
                        m_logger.error("Unable to parse headers.  Pass accumulated ",
                                       "bytes as a normal chunk, and passthru rest of message");
                        //Grab anything trapped thus far in the file (if we can
                        recoverTrappedHeaderBytes(toks);

                        //Figure out next state (in case that was the end of the message as well)
                        if(m_msgGrabber.hasMsgRemaining()) {
                            changeParserState(ISPState.DRAINING_HOSED);
                        }
                        else {
                            m_msgGrabber = null;
                            changeParserState(ISPState.SCANNING);
                        }
                        break;
                    }//ENDOF Header PArse Error
                    else {//BEGIN Headers parsed
                        m_logger.debug("Adding the BeginMIMEToken");
                        toks.add(
                                 new BeginImapMIMEToken(
                                                        m_msgGrabber.accumulator,
                                                        createMessageInfo(headers),
                                                        m_msgGrabber.getTotalMessageLength())
                                 );
                        m_msgGrabber.noLongerAccumulatorMaster();
                        changeParserState(ISPState.DRAINING_BODY);

                        //Check for an empty body
                        if(m_msgGrabber.scanner.isEmptyMessage()) {
                            m_logger.debug("Message blank.  Complete message tokens.");
                            toks.add(new ContinuedMIMEToken(m_msgGrabber.accumulator.createChunk(null, true)));
                            changeParserState(ISPState.SCANNING);
                            m_msgGrabber = null;
                        }
                    }//ENDOF Headers parsed
                }//ENDOF End of Headers
                else {
                    m_logger.debug("Need more header bytes");
                    return returnWithCompactedBuffer(buf, toks);
                }
                break;

                //===================================================
            case DRAINING_HOSED:
                dup = getNextBodyChunkBuffer(buf);
                m_logger.debug("Adding passthru body chunk of length ", dup.remaining());
                toks.add(new UnparsableMIMEChunk(dup));

                //Advance the buf past what we just transferred
                buf.position(buf.position() + dup.remaining());

                m_msgGrabber.decrementMsgRemaining(dup.remaining());

                if(!m_msgGrabber.hasMsgRemaining()) {
                    m_logger.debug("Found message end");
                    changeParserState(ISPState.SCANNING);
                    m_msgGrabber.accumulator.dispose();//Redundant
                    m_msgGrabber = null;
                }
                break;

                //===================================================
            case DRAINING_BODY:

                MIMEAccumulator.MIMEChunk mimeChunk = null;

                if(m_msgGrabber.hasMsgRemaining()) {

                    dup = getNextBodyChunkBuffer(buf);
                    m_msgGrabber.decrementMsgRemaining(dup.remaining());

                    m_logger.debug("Next body chunk of length ",
                                   dup.remaining(), ", ", m_msgGrabber.getMsgRemaining(),
                                   " message bytes remaining");

                    buf.position(buf.position() + dup.remaining());

                    if(m_msgGrabber.hasMsgRemaining()) {
                        m_logger.debug("Adding continued body chunk of length ", dup.remaining());
                        mimeChunk = m_msgGrabber.accumulator.createChunk(dup.slice(), false);
                    }
                    else {
                        m_logger.debug("Adding final body chunk of length ", dup.remaining());
                        mimeChunk = m_msgGrabber.accumulator.createChunk(dup.slice(), true);
                        changeParserState(ISPState.SCANNING);
                        m_msgGrabber = null;
                    }
                }
                else {
                    m_logger.debug("Adding terminal chunk (no data)");
                    mimeChunk = m_msgGrabber.accumulator.createChunk(null, true);
                    changeParserState(ISPState.SCANNING);
                    m_msgGrabber = null;
                }
                toks.add(new ContinuedMIMEToken(mimeChunk));
                break;
            }
        }
        //The only way to get here is an empty buffer.  I think
        //this can only happen if the opening of a message is found at the end
        //of a packet (or a complete body was found in one packet.  Eitherway,
        //since the buffer is empty we don't worry about any remaining
        //chunks.
        m_logger.debug("Buffer empty.  Return tokens");
        return new ParseResult(toks);
    }

    @Override
    public void handleFinalized() {
        super.handleFinalized();
        if(m_msgGrabber != null) {
            m_logger.debug("Unexpected finalized in state " + m_state);
            if(m_msgGrabber.accumulator != null) {
                m_msgGrabber.accumulator.dispose();
            }
        }
    }


    /**
     * Helper which rewinds the buffer to the position *just before*
     * a literal
     */
    private void rewindLiteral(ByteBuffer buf) {
        for(int i = buf.limit()-1; i>=buf.position(); i--) {
            if(buf.get(i) == OPEN_BRACE_B) {
                buf.limit(i);
                return;
            }
        }
        throw new RuntimeException("No \"{\" found to rewind-to");
    }

    private void changeParserState(ISPState state) {
        if(m_state != state) {
            m_logger.debug("Change state from \"",
                           m_state,
                           "\" to \"", state, "\"");
            m_state = state;
        }

    }

    /**
     * Helper method which duplicates buf with as-many body bytes as are appropriate
     * based on the number of bytes remaining and the size of buf.  Does <b>not</b>
     * advance "buf" to account for the transferred bytes.
     */
    private ByteBuffer getNextBodyChunkBuffer(ByteBuffer buf) {
        ByteBuffer dup = buf.duplicate();
        dup.limit(dup.position() +
                  (m_msgGrabber.getMsgRemaining() > buf.remaining()?
                   buf.remaining():
                   m_msgGrabber.getMsgRemaining()));
        return dup;
    }

    /**
     * Re-used logic broken-out to simplify "parse" method
     */
    private void recoverTrappedHeaderBytes(List<Token> toks) {
        //Grab anything trapped thus far in the file (if we can
        ByteBuffer trapped = m_msgGrabber.accumulator.drainFileToByteBuffer();
        m_logger.debug("Close accumulator");
        m_msgGrabber.accumulator.dispose();

        if(trapped == null) {
            m_logger.debug("Could not recover buffered header bytes");
        }
        else {
            m_logger.debug("Retreived ", trapped.remaining(), " bytes trapped in file");
            toks.add(new UnparsableMIMEChunk(trapped));
        }
    }

    /**
     * Helper method removing duplication in parse method
     */
    private ParseResult returnWithCompactedBuffer(ByteBuffer buf,
                                                  List<Token> toks) {
        buf = compactIfNotEmpty(buf, m_msgBoundaryScanner.getLongestWord());
        m_logger.debug("Returning ", toks.size(), " tokens and a ",
                       (buf==null?"null buffer":"buffer at position " + buf.position()));
        return new ParseResult(toks, buf);
    }

    /**
     * Open the MessageGrabber.  False is returned if the underlying
     * MIMEAccumulator cannot be opened (however the message grabber
     * is still created, to track the length of message consumed).
     */
    private boolean openMessageGrabber(int totalMsgLen) {
        try {
            m_msgGrabber = new MessageGrabber(totalMsgLen,
                                              new MIMEAccumulator(getPipeline()));
            return true;
        }
        catch(IOException ex) {
            m_logger.error(ex, "Exception creating MIME Accumulator");
            m_msgGrabber = new MessageGrabber(totalMsgLen,
                                              null);
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
                                                             getSession().sessionEvent(),
                                                             getSession().getServerPort());

        UvmContextFactory.context().logEvent(ret);
        

        String username = getImapCasing().getSessionMonitor().getUserName();
        if(username == null) {
            //      username = "UNKNOWN";
            m_logger.debug("Unable to determine client login name");
        }
        else {
            ret.addAddress(AddressKind.USER, username, null);
        }

        return ret;
    }

    /**
     * Little class to associate all
     * state with grabbing a message
     */
    private class MessageGrabber 
    {
        final MessageBoundaryScanner scanner;
        final MIMEAccumulator accumulator;
        private boolean m_isMasterOfAccumulator = true;
        private final int m_totalMsgLen;
        private int m_msgReadSoFar;

        MessageGrabber(int msgLength, MIMEAccumulator accumulator) 
        {
            scanner = new MessageBoundaryScanner();
            this.accumulator = accumulator;
            this.m_totalMsgLen = msgLength;
            m_msgReadSoFar = 0;

        }
        
        int getTotalMessageLength() 
        {
            return m_totalMsgLen;
        }
       
        @SuppressWarnings("unused")
		boolean isAccumulatorHosed() 
        {
            return accumulator == null;
        }
       
        boolean hasMsgRemaining() 
        {
            return getMsgRemaining() > 0;
        }
        
        int getMsgRemaining() 
        {
            return m_totalMsgLen - m_msgReadSoFar;
        }
        
        void decrementMsgRemaining(int amt) 
        {
            m_msgReadSoFar+=amt;
        }
        
        @SuppressWarnings("unused")
		boolean isMasterOfAccumulator() 
        {
            return m_isMasterOfAccumulator;
        }
        /**
         * Called when we have passed-along the accumulator
         * (in a "BeginMIMEToken").
         */
        void noLongerAccumulatorMaster() {
            m_isMasterOfAccumulator = false;
        }
    }
}
