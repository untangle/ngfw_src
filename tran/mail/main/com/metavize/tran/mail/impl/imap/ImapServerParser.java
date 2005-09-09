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

import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.tran.token.AbstractParser;
import com.metavize.tran.token.Chunk;
import com.metavize.tran.token.Token;
import com.metavize.tran.token.ParseException;
import com.metavize.tran.token.ParseResult;
import org.apache.log4j.Logger;

import com.metavize.tran.mime.*;

import com.metavize.tran.mail.papi.*;
import com.metavize.tran.mail.papi.imap.*;

import static com.metavize.tran.util.ASCIIUtil.*;
import static com.metavize.tran.util.Ascii.*;

import com.metavize.tran.mail.papi.imap.*;

import java.io.*;

import java.util.*;

class ImapServerParser
  extends ImapParser {

  //Fetch pattern variants

  //Basic:
  //<new line> [WORD|+|*] NNN "FETCH" "(" stuff ")"
  //
  //We care about some of the "stuff", such as:
  //
  //BODY[]
  //RFC822
  //If we see BODY[xxx], punt
  //If we see BODY[]<nnn>, punt (cause the "<nnn>" is a partial fetch)

  //States
  //LOOKING_FOR_NEW_LINE
  //SKIPPING_LITERAL (requires a count of the literal being skipped w/ progress)
  //NEW_LINE (just saw a new line, or the begining of the conversation)
  //NL_WORD (<new line> [WORD|+|*])
  //NL_WORD_WORD
  //FETCH_RESP 
  //FETCH_OPEN_P (if not an open paren, punt)
  //

  private enum ISPState {
    SCANNING,
    DRAINING_MSG
  };
  
  private final Logger m_logger =
    Logger.getLogger(ImapServerParser.class);

  private final IMAPTokenizer m_tokenizer;
  private IMAPBodyScanner m_msgBoundaryScanner;
  private ISPState m_state = ISPState.SCANNING;
  private int m_msgRemaining = 0;//@depricated
  private MessageGrabber m_msgGrabber;

    
  // constructors -----------------------------------------------------------

  ImapServerParser(TCPSession session,
    ImapCasing parent) {
    
    super(session, parent, false);
    lineBuffering(false); // XXX line buffering

    m_tokenizer = new IMAPTokenizer();
    m_msgBoundaryScanner = new IMAPBodyScanner();
    
    m_logger.debug("Created");
  }

  // Parser methods ---------------------------------------------------------

  private void rewindLiteral(ByteBuffer buf) {
    for(int i = buf.limit()-1; i>=buf.position(); i--) {
      if(buf.get(i) == OPEN_BRACE_B) {
        buf.limit(i);
        return;
      }    
    }
    throw new RuntimeException("No \"{\" found to rewind-to");
  }

  public ParseResult parse(ByteBuffer buf) {

    //TEMP - so folks don't hit my unfinished code by accident
    if(System.currentTimeMillis() > 0) {
      return new ParseResult(new Chunk(buf));
    }      
  
  
    m_logger.debug("====== parse =====");
    m_logger.debug("BEGIN ORIG");
    m_logger.debug(bbToString(buf));
    m_logger.debug("ENDOF ORIG");
  
    if(isPassthru()) {
      return new ParseResult(new Chunk(buf));
    }
  
    ByteBuffer dup = buf.duplicate();
    List<Token> toks = new LinkedList<Token>();

    //TODO bscott remove debugging "duplicate" calls to "dup"
    while(buf.hasRemaining()) {
      switch(m_state) {
        case SCANNING:
          if(m_msgBoundaryScanner.scanForMsgState(buf)) {
            m_logger.debug("Found message boundary start.");
            
            //First off, we need to put into the returned token
            //list a Chunk with the bytes which were NOT the
            //message
            dup.limit(buf.position());
            rewindLiteral(dup);
            m_logger.debug("Adding protocol chunk of length " + dup.remaining());
            toks.add(new Chunk(dup.slice()));
            
            m_msgRemaining = m_msgBoundaryScanner.getMessageOctetCount();

            //This is a simulation of stuff that'll be in place later.
            toks.add(new Chunk(ByteBuffer.wrap(("{" + m_msgRemaining + "}\r\n").getBytes())));

            //Replace the dup with stuff we haven't sent.
            dup = buf.duplicate();
            m_state = ISPState.DRAINING_MSG;
            continue;
          }
          else {
            m_logger.debug("We need more data in SCANNING state");
            dup.limit(buf.position());
            m_logger.debug("Adding protocol chunk of length " + dup.remaining());
            toks.add(new Chunk(dup.slice()));
            buf = compactIfNotEmpty(buf, m_tokenizer.getLongestWord());
            m_logger.debug("Returning " + toks.size() + " tokens and a " +
              (buf==null?"null buffer":"buffer at position " + buf.position()));
            return new ParseResult(toks, buf);
          }
        case DRAINING_MSG:
          int msgBytesInThisBuffer = m_msgRemaining > buf.remaining()?
            buf.remaining():m_msgRemaining;
          
          dup.limit(dup.position() + msgBytesInThisBuffer);
          m_logger.debug("Adding body chunk of length " + dup.remaining());
          toks.add(new Chunk(dup.slice()));

          //Assign the duplicate starting from the bytes we have not sent
          //as tokens.  Also, advance the buf past what we sent
          buf.position(buf.position() + msgBytesInThisBuffer);          
          dup = buf.duplicate();
                    
          m_msgRemaining-=msgBytesInThisBuffer;
          
          if(m_msgRemaining <=0) {
            m_logger.debug("Found message end");
            m_state = ISPState.SCANNING;
            m_msgRemaining = 0;
          }
          break;
      }
    }
    //The only way to get here is an empty buffer.  I think
    //this can only happen if the opening of a message is found at the end
    //of a packet (or a complete body was found in one packet.  Eitherway,
    //since the buffer is empty we don't worry about any remaining
    //chunks.
    m_logger.debug("Must have ended a message or msg opening at the end of a packet");
    return new ParseResult(toks);
  }

  public ParseResult parseEnd(ByteBuffer buf) {
    Chunk c = new Chunk(buf);

    m_logger.debug(this + " passing chunk of size: " + buf.remaining());
    return new ParseResult(c);
  }

  private boolean openMessageGrabber(int totalMsgLen) {
    try {
      m_msgGrabber = new MessageGrabber(totalMsgLen,
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
    final int totalMsgLen;
    private int m_msgReadSoFar;

    MessageGrabber(int msgLength,
      MIMEAccumulator accumulator) {
      scanner = new MessageBoundaryScanner();
      this.accumulator = accumulator;
      this.totalMsgLen = msgLength;
      m_msgReadSoFar = 0;
      
    }
    boolean hasMsgRemaining() {
      return getMsgRemaining() > 0;
    }
    int getMsgRemaining() {
      return totalMsgLen - m_msgReadSoFar;
    }
    void decrementMsgRemaining(int amt) {
      m_msgReadSoFar-=amt;
    }
    boolean isMasterOfAccumulator() {
      return m_isMasterOfAccumulator;
    }
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


    IMAPBodyScanner() {
      changeState(NL_XXXX_S);
    }

    private void changeState(int newState) {
      m_logger.debug("[$IMAPBodyScanner] Change state from " +
        m_state + " to " + newState);
      m_state = newState;
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

      m_logger.debug("BEGIN scanForMsgState");
    
      //Reset the message length, as it never caries
      //over
      if(m_state == DRN_BDY_S) {
        m_msgLength = -1;
        changeState(LF_NL_X_S);
        m_lineWordCount = 0;
      }
      
      while(buf.hasRemaining()) {
        m_logger.debug("scanForMsgState (loop.  State " + m_state + ")");
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
