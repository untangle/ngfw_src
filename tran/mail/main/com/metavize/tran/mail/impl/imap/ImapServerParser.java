/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.mail.impl.imap;

import java.nio.ByteBuffer;
import org.apache.log4j.Logger;
import com.metavize.tran.mail.papi.imap.IMAPTokenizer;
import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.tran.mime.MIMEPart;
import com.metavize.tran.mime.MIMEMessageHeaders;
import com.metavize.tran.token.AbstractParser;
import com.metavize.tran.token.TokenStreamer;
import com.metavize.tran.token.Chunk;
import com.metavize.tran.token.Token;
import com.metavize.tran.token.ParseException;
import com.metavize.tran.token.ParseResult;
import java.util.List;
import java.util.LinkedList;
import static com.metavize.tran.util.Ascii.*;
import com.metavize.tran.mail.papi.MessageInfo;
import com.metavize.tran.mail.papi.MessageBoundaryScanner;
import com.metavize.tran.mail.papi.MIMEAccumulator;
import com.metavize.tran.mail.papi.imap.ImapChunk;
import com.metavize.tran.mail.papi.imap.UnparsableMIMEChunk;
import com.metavize.tran.mail.papi.imap.BeginImapMIMEToken;
import com.metavize.tran.mail.papi.ContinuedMIMEToken;
import java.io.IOException;
import com.metavize.tran.mail.papi.AddressKind;
import com.metavize.tran.mail.papi.MessageInfoFactory;

/**
 * 'name says it all...
 */
class ImapServerParser
  extends ImapParser {

  /**
   * State of the parser (*outer* parser)
   */
  private enum ISPState {
    SCANNING,
    DRAINING_HEADERS,
    DRAINING_BODY,
    DRAINING_HOSED,
  };
  
  private final Logger m_logger =
    Logger.getLogger(ImapServerParser.class);

  private final IMAPTokenizer m_tokenizer;
  private IMAPBodyScanner m_msgBoundaryScanner;
  private ISPState m_state = ISPState.SCANNING;
  private MessageGrabber m_msgGrabber;

    

  ImapServerParser(TCPSession session,
    ImapCasing parent) {
    
    super(session, parent, false);
    lineBuffering(false); // XXX line buffering

    m_tokenizer = new IMAPTokenizer();
    m_msgBoundaryScanner = new IMAPBodyScanner();
    
    m_logger.debug("Created");
  }


  public ParseResult parse(ByteBuffer buf) {


    //TEMP - so folks don't hit my unfinished code by accident
//    if(System.currentTimeMillis() > 0) {
//      getImapCasing().traceParse(buf);
//      return new ParseResult(new Chunk(buf));
//    }

    //do tracing stuff
    getImapCasing().traceParse(buf);

    //Check for passthru
    if(isPassthru()) {
      return new ParseResult(new Chunk(buf));
    }
  
    ByteBuffer dup = null;
    List<Token> toks = new LinkedList<Token>();

    while(buf.hasRemaining()) {
      switch(m_state) {
        //===================================================
        case SCANNING:
          dup = buf.duplicate();
          if(m_msgBoundaryScanner.scanForMsgState(buf)) {
            m_logger.debug("Found message boundary start. Octet count: " +
              m_msgBoundaryScanner.getMessageOctetCount());
            
            //First off, we need to put into the returned token
            //list a Chunk with the bytes which were NOT the
            //message
            dup.limit(buf.position());
            rewindLiteral(dup);
            m_logger.debug("Adding protocol chunk of length " + dup.remaining());
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
            m_logger.debug("Adding protocol chunk of length " + dup.remaining());
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
            m_logger.debug("Headers are blank. " +
              m_msgGrabber.getMsgRemaining() + " msg bytes remain");
          }
          else {
            m_logger.debug("About to write the " +
              (foundEndOfHeaders?"last":"next") + " " +
              dup.remaining() + " header bytes to disk. " +
              m_msgGrabber.getMsgRemaining() + " msg bytes remain");
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
              m_logger.error("Unable to parse headers.  Pass accumulated " +
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
          m_logger.debug("Adding passthru body chunk of length " + dup.remaining());
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
                        
            m_logger.debug("Next body chunk of length " +
              dup.remaining() + ", " + m_msgGrabber.getMsgRemaining() +
              " message bytes remaining");
          
            buf.position(buf.position() + dup.remaining());
            
            if(m_msgGrabber.hasMsgRemaining()) {
              m_logger.debug("Adding continued body chunk of length " + dup.remaining());
              mimeChunk = m_msgGrabber.accumulator.createChunk(dup.slice(), false);              
            }
            else {
              m_logger.debug("Adding final body chunk of length " + dup.remaining());
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
  public TokenStreamer endSession() {
    m_logger.debug("End Session");
    return super.endSession();
  }  

  public ParseResult parseEnd(ByteBuffer buf) {
    Chunk c = new Chunk(buf);

    m_logger.debug(this + " passing chunk of size: " + buf.remaining());
    return new ParseResult(c);
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
      m_logger.debug("Change state from \"" +
        m_state + "\" to \"" + state + "\"");
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
      m_logger.debug("Retreived " + trapped.remaining() + " bytes trapped in file");
      toks.add(new UnparsableMIMEChunk(trapped));
    }
  }
  
  /**
   * Helper method removing duplication in parse method
   */
  private ParseResult returnWithCompactedBuffer(ByteBuffer buf,
    List<Token> toks) {
    buf = compactIfNotEmpty(buf, m_tokenizer.getLongestWord());
    m_logger.debug("Returning " + toks.size() + " tokens and a " +
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
      m_logger.error("Exception creating MIME Accumulator", ex);
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
      getSession().id(),
      getSession().serverPort());

    m_logger.debug("Setting fake USER on MessageInfo");
    ret.addAddress(AddressKind.USER, "nobody@nowhere", null);

    return ret;
  }

  /**
   * Little class to associate all
   * state with grabbing a message
   */
  private class MessageGrabber {
  
    final MessageBoundaryScanner scanner;
    final MIMEAccumulator accumulator;
    private boolean m_isMasterOfAccumulator = true;
    private final int m_totalMsgLen;
    private int m_msgReadSoFar;

    MessageGrabber(int msgLength,
      MIMEAccumulator accumulator) {
      scanner = new MessageBoundaryScanner();
      this.accumulator = accumulator;
      this.m_totalMsgLen = msgLength;
      m_msgReadSoFar = 0;
      
    }
    int getTotalMessageLength() {
      return m_totalMsgLen;
    }
    boolean isAccumulatorHosed() {
      return accumulator == null;
    }
    boolean hasMsgRemaining() {
      return getMsgRemaining() > 0;
    }
    int getMsgRemaining() {
      return m_totalMsgLen - m_msgReadSoFar;
    }
    void decrementMsgRemaining(int amt) {
      m_msgReadSoFar+=amt;
    }
    boolean isMasterOfAccumulator() {
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



//================================
// Constants for
// IMAPBodyScanner inner
// class (since inner classes
// cannot have static finals)


  private static final int EOL__T = 0;
  private static final int FTCH_T = 1;
  private static final int BODY_T = 2;
  private static final int R22__T = 3;
  private static final int WORD_T = 4;
  private static final int QSTR_T = 5;
  private static final int LITR_T = 6;
  private static final int OB_X_T = 7;
  private static final int CB_X_T = 8;
  private static final int LT_X_T = 9;
  private static final int PARN_T = 10;
  private static final int DELM_T = 11;

  private static final int A01 = 0;
  private static final int A02 = 1;
  private static final int A03 = 2;
  private static final int A04 = 3;
  private static final int A05 = 4;
  private static final int A06 = 5;
  private static final int A07 = 6;
  private static final int A08 = 7;
  private static final int A09 = 8;
  private static final int A10 = 9;
  private static final int A11 = 10;
  private static final int A12 = 11;
  private static final int A13 = 12;
  private static final int A14 = 13;

  private static final int LF_NL_X_S = 0;
  private static final int SL_XXXX_S = 1;
  private static final int NL_XXXX_S = 2;
  private static final int F_XXXXX_S = 3;
  private static final int FOP_XXX_S = 4;
  private static final int F_B_XXX_S = 5;
  private static final int F_B_R_X_S = 6;
  private static final int F_B_R_R_S = 7;
  private static final int DRN_BDY_S = 8;


  private static final int[][] TRAN_TBL = {
            //EOL__T FTCH_T BODY_T R22__T WORD_T QSTR_T LITR_T OB_X_T CB_X_T LT_X_T PARN_T DELM_T
/*LF_NL_X_S*/ {A01,   A02,   A02,   A02,   A02,   A02,   A11,   A02,   A02,   A02,   A02,   A02},
/*SL_XXXX_S*/ {A14,   A14,   A14,   A14,   A14,   A14,   A14,   A14,   A14,   A14,   A14,   A14},
/*NL_XXXX_S*/ {A01,   A03,   A02,   A02,   A04,   A04,   A12,   A02,   A04,   A04,   A04,   A04},
/*F_XXXXX_S*/ {A01,   A02,   A02,   A02,   A02,   A02,   A13,   A02,   A02,   A02,   A05,   A02},
/*FOP_XXX_S*/ {A01,   A05,   A07,   A06,   A05,   A05,   A12,   A05,   A05,   A05,   A05,   A05},
/*F_B_XXX_S*/ {A01,   A02,   A02,   A02,   A02,   A02,   A13,   A08,   A02,   A02,   A02,   A02},
/*F_B_R_X_S*/ {A01,   A02,   A02,   A02,   A02,   A02,   A13,   A02,   A09,   A02,   A02,   A02},
/*F_B_R_R_S*/ {A01,   A02,   A02,   A02,   A02,   A02,   A10,   A02,   A02,   A02,   A02,   A02},
/*DRN_BDY_S*/ {A14,   A14,   A14,   A14,   A14,   A14,   A14,   A14,   A14,   A14,   A14,   A14}
    };

  


  private static final int MAX_TOKENS_BEFORE_FETCH = 6;
  private static final byte[] FETCH_BYTES = "fetch".getBytes();
  private static final byte[] BODY_BYTES = "body".getBytes();
  private static final byte[] RFC822_BYTES = "rfc822".getBytes();
  
  class IMAPBodyScanner {

    private int m_lineWordCount;
    private int m_toSkipLiteral;    
    private int m_state = NL_XXXX_S;
    private int m_msgLength = -1;
    private int m_pushedStateForLiteral = -1;
    private Logger m_logger =
      Logger.getLogger(ImapServerParser.IMAPBodyScanner.class);

    IMAPBodyScanner() {
      changeState(NL_XXXX_S);
    }

    private void changeState(int newState) {
      if(newState != m_state) {
        m_logger.debug("Change state from " +
          m_state + " to " + newState);
        m_state = newState;
      }
    }

    int getMessageOctetCount() {
      return m_msgLength;
    }

    /**
     * If true is returned, then the caller <b>must</b> make sure
     * to rewind the buffer such that the literal declaration is
     * not sent to the client as part of the stuff we were
     * scanning before we found the message.
     */
    boolean scanForMsgState(ByteBuffer buf) {

      //Reset the message length, as it never caries
      //over
      if(m_state == DRN_BDY_S) {
        m_msgLength = -1;
        changeState(LF_NL_X_S);
        m_lineWordCount = 0;
      }
      
      while(buf.hasRemaining()) {
        //Before we tokenize into a literal by-accident,
        //handle literal draining first
        if(m_state == SL_XXXX_S) {
          //Skipping literal
          int thisSkip = buf.remaining()>m_toSkipLiteral?
            m_toSkipLiteral:buf.remaining();
          m_logger.debug("Continuing to skip next: " + thisSkip + " bytes");
          buf.position(buf.position() + thisSkip);
          m_toSkipLiteral-=thisSkip;
          if(m_toSkipLiteral == 0) {
            if(m_pushedStateForLiteral == -1) {
              throw new RuntimeException("Draining literal without next state");
            }
            changeState(m_pushedStateForLiteral);
            m_pushedStateForLiteral = -1;
          }
          continue;
        }

        //From here, the states "SL_XXXX_S" and "DRN_BDY_S" are illegal
        
        //Now, get the next result
        switch(m_tokenizer.next(buf)) {
          case EXCEEDED_LONGEST_WORD:
            m_logger.debug("Exceeded Longest Word.  Skip past whole buffer");
            buf.position(buf.limit());
            return false;
          case NEED_MORE_DATA:
            m_logger.debug("Need more data");
            return false;
        }

        //Falling-out of that switch is equivilant
        //to the "HAVE_TOKEN:" case.  Now classify the token
        int tokenClass = -1;
        switch(m_tokenizer.getTokenType()) {
          case WORD:
            if(m_tokenizer.compareWordAgainst(buf, FETCH_BYTES, true)) {
              tokenClass = FTCH_T;
            }
            else if(m_tokenizer.compareWordAgainst(buf, BODY_BYTES, true)) {
              tokenClass = BODY_T;
            }
            else if(m_tokenizer.compareWordAgainst(buf, RFC822_BYTES, true)) {
              tokenClass = R22__T;
            }
            else {
              tokenClass = WORD_T;
            }
            break;
          case QSTRING:
            tokenClass = QSTR_T;
            break;
          case LITERAL:
            tokenClass = LITR_T;
            break;
          case CONTROL_CHAR:
            if(buf.get(m_tokenizer.getTokenStart()) == OPEN_BRACKET_B) {
              tokenClass = OB_X_T;
            }
            else if(buf.get(m_tokenizer.getTokenStart()) == CLOSE_BRACKET_B) {
              tokenClass = CB_X_T;
            }
            else if(buf.get(m_tokenizer.getTokenStart()) == LT_B) {
              tokenClass = LT_X_T;
            }
            else if(buf.get(m_tokenizer.getTokenStart()) == OPEN_PAREN_B) {
              tokenClass = PARN_T;
            }
            else {
              tokenClass = DELM_T;
            }
            break;
          case NEW_LINE:
            tokenClass = EOL__T;
            break;
          default:
            throw new RuntimeException("Unexpected token type: " + m_tokenizer.getTokenType());
        }


        //Now, index into our function table for what to do based
        //on current state and the token class
        switch(TRAN_TBL[m_state][tokenClass]) {
          case A01:
            changeState(NL_XXXX_S);
            m_lineWordCount = 0;
            break;
          case A02:
            changeState(LF_NL_X_S);
            m_lineWordCount = 0;
            break;
          case A03:
            if(m_lineWordCount > 1) {
              changeState(F_XXXXX_S);
              m_lineWordCount = 0;              
            }
            else {
              m_logger.debug("Odd.  Encountered \"FECTH\" as first word on line");
              m_lineWordCount++;              
            }
            break;
          case A04:
            if(++m_lineWordCount > MAX_TOKENS_BEFORE_FETCH) {
              changeState(LF_NL_X_S);
              m_lineWordCount = 0;              
            }          
            break;
          case A05:
            changeState(FOP_XXX_S);
            break;
          case A09://Duplicate actions
          case A06:
            changeState(F_B_R_R_S);
            break;
          case A07:
            changeState(F_B_XXX_S);
            break;
          case A08:
            changeState(F_B_R_X_S);
            break;
          case A10:
            m_logger.debug("Found body declaration");
            m_msgLength = m_tokenizer.getLiteralOctetCount();
            changeState(DRN_BDY_S);
            return true;
          case A11:
            m_pushedStateForLiteral = m_state;
            m_toSkipLiteral = m_tokenizer.getLiteralOctetCount();
            changeState(SL_XXXX_S);
            break;
          case A12:
            m_toSkipLiteral = m_tokenizer.getLiteralOctetCount();
            m_lineWordCount++;
            changeState(SL_XXXX_S);
            break;
          case A13:
            m_toSkipLiteral = m_tokenizer.getLiteralOctetCount();
            m_pushedStateForLiteral = LF_NL_X_S;
            changeState(SL_XXXX_S);
            break;
          case A14:
          default:
            throw new RuntimeException("Unknown action");
        }
      }
      return false;
    }
  }
}
