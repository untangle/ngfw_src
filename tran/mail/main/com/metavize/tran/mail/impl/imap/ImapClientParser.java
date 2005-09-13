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
import com.metavize.tran.mail.papi.imap.IMAPTokenizer;
import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.tran.token.TokenStreamer;
import com.metavize.tran.token.Chunk;
import com.metavize.tran.token.ParseResult;
import org.apache.log4j.Logger;

/**
 * 'name says it all...
 */
class ImapClientParser
  extends ImapParser {
  
  private final Logger m_logger =
    Logger.getLogger(ImapClientParser.class);

  private final IMAPTokenizer m_tokenizer;

  private int m_toSkip = 0;   

  ImapClientParser(TCPSession session,
    ImapCasing parent) {
    
    super(session, parent, true);
    lineBuffering(false);

    m_tokenizer = new IMAPTokenizer();    
    
    m_logger.debug("Created");
  }

  public ParseResult parse(ByteBuffer buf) {

    //TEMP - so folks don't hit my unfinished code by accident
//    if(System.currentTimeMillis() > 0) {
//      getImapCasing().traceParse(buf);
//      return new ParseResult(new Chunk(buf));
//    }
  
    getImapCasing().traceParse(buf);

    return new ParseResult(new Chunk(buf));
  }

  @Override  
  public TokenStreamer endSession() {
    m_logger.debug("End Session");
    return super.endSession();
  }

  public ParseResult parseEnd(ByteBuffer buf) {
    Chunk c = new Chunk(buf);
    return new ParseResult(c);
  }
}
