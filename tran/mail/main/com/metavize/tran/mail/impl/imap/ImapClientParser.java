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
import com.metavize.tran.token.ParseException;
import com.metavize.tran.token.ParseResult;
import org.apache.log4j.Logger;

import static com.metavize.tran.util.ASCIIUtil.*;

import com.metavize.tran.mail.papi.imap.*;

public class ImapClientParser
  extends ImapParser {
  
  private final Logger m_logger =
    Logger.getLogger(ImapClientParser.class);

  private final IMAPTokenizer m_tokenizer;

  private int m_toSkip = 0;   

  ImapClientParser(TCPSession session,
    ImapCasing parent) {
    
    super(session, parent, true);
    lineBuffering(false); // XXX line buffering

    m_tokenizer = new IMAPTokenizer();    
    
    m_logger.debug("Created");
  }

  // Parser methods ---------------------------------------------------------

  public ParseResult parse(ByteBuffer buf) {

    return new ParseResult(new Chunk(buf));

    /*
  
    ByteBuffer dup = buf.duplicate();
    m_logger.debug("====== parse =====");
    m_logger.debug("BEGIN ORIG");
    m_logger.debug(bbToString(buf));
    m_logger.debug("ENDOF ORIG");

    
    m_logger.debug("BEGIN TOKENS");
    while(buf.hasRemaining()) {
      if(m_toSkip > 0) {
        int thisSkip = buf.remaining()>m_toSkip?
          m_toSkip:buf.remaining();
        m_logger.debug("Continuing to skip next: " + thisSkip + " bytes");
        buf.position(buf.position() + thisSkip);
        m_toSkip-=thisSkip;
      }
      switch(m_tokenizer.next(buf)) {
        case HAVE_TOKEN:
          m_logger.debug("NEXT: (token) " + m_tokenizer.tokenToStringDebug(buf));
          if(m_tokenizer.getTokenType() == IMAPTokenizer.IMAPTT.LITERAL) {
            m_toSkip+=m_tokenizer.getLiteralOctetCount();
          }          
          break;
        case EXCEEDED_LONGEST_WORD:
          m_logger.debug("NEXT: Exceeded Longest Word");
          return new ParseResult(new Chunk(dup));
        case NEED_MORE_DATA:
          m_logger.debug("NEXT: Need more data");
          break;
      }
    }
    m_logger.debug("ENDOF TOKENS");
    
    
    
    buf = compactIfNotEmpty(buf, m_tokenizer.getLongestWord());

    if(buf == null) {
      m_logger.debug("returning ParseResult with " +
        "no extra bytes buffer");
    }
    else {
      m_logger.debug("returning ParseResult with " +
        " with " +
        buf.remaining() + " remaining (" +
        buf.position() + " to be seen on next invocation)");
    }
    return new ParseResult(new Chunk(dup), buf);
    */
  }

  public ParseResult parseEnd(ByteBuffer buf) {
    Chunk c = new Chunk(buf);

    m_logger.debug(this + " passing chunk of size: " + buf.remaining());
    return new ParseResult(c);
  }
}
