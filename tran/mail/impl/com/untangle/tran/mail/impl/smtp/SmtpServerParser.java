/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.tran.mail.impl.smtp;

import com.untangle.tran.mail.papi.smtp.ResponseParser;
import com.untangle.tran.mail.papi.smtp.Response;
import com.untangle.tran.mail.papi.smtp.SASLExchangeToken;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import com.untangle.mvvm.tapi.TCPSession;
import com.untangle.tran.token.ParseResult;
import com.untangle.tran.token.PassThruToken;
import com.untangle.tran.token.Chunk;
import com.untangle.tran.token.Token;


/**
 * ...name says it all...
 */
class SmtpServerParser
  extends SmtpParser {

  private final Logger m_logger =
    Logger.getLogger(SmtpServerParser.class);

  SmtpServerParser(TCPSession session,
    SmtpCasing parent,
    CasingSessionTracker tracker) {
    super(session, parent, tracker, false);
    
    m_logger.debug("Created");
    lineBuffering(false);
  }
  
  @Override
  protected ParseResult doParse(ByteBuffer buf) {

    List<Token> toks = new ArrayList<Token>();
    boolean done = false;
    
    while(buf.hasRemaining() && ! done) {

      if(isPassthru()) {
        m_logger.debug("Passthru buffer (" + buf.remaining() + " bytes )");
        toks.add(new Chunk(buf));
        return new ParseResult(toks);
      }

      if(getSmtpCasing().isInSASLLogin()) {
        m_logger.debug("In SASL Exchange");
        ByteBuffer dup = buf.duplicate();
        switch(getSmtpCasing().getSASLObserver().serverData(buf)) {
          case EXCHANGE_COMPLETE:
            m_logger.debug("SASL Exchange complete");
            getSmtpCasing().closeSASLExchange();
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
        continue;
      }
    
      try {
        ByteBuffer dup = buf.duplicate();
        Response resp = new ResponseParser().parse(dup);
        if(resp != null) {
          buf.position(dup.position());
          getSessionTracker().responseReceived(resp);
          m_logger.debug("Received response: " +
            resp.toDebugString());
          toks.add(resp);
        }
        else {
          done = true;
          m_logger.debug("Need more bytes for response");
        }
      }
      catch(Exception ex) {
        m_logger.warn("Exception parsing server response", ex);
        declarePassthru();
        toks.add(PassThruToken.PASSTHRU);
        toks.add(new Chunk(buf));
        return new ParseResult(toks);
      }
    }

    //Compact the buffer
    buf = compactIfNotEmpty(buf, (1024*2));//TODO bscott a real value
    
    if(buf != null) {
      m_logger.debug("returning ParseResult with " +
        toks.size() + " tokens and a buffer with " + buf.remaining() + " remaining");
    }
    return new ParseResult(toks, buf);
  }
                  

}