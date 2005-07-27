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

package com.metavize.tran.mail.impl.smtp;


import com.metavize.tran.mail.papi.smtp.ResponseParser;
import com.metavize.tran.mail.papi.smtp.Response;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;

import com.metavize.mvvm.*;
import com.metavize.mvvm.tapi.*;
import com.metavize.tran.token.*;
import com.metavize.tran.util.*;
import org.apache.log4j.Logger;

/**
 * ...name says it all...
 */
class SmtpServerParser
  extends SmtpParser {

  private final Logger m_logger = Logger.getLogger(SmtpServerParser.class);
  private ResponseParser m_parser = new ResponseParser();
  
  SmtpServerParser(TCPSession session,
    SmtpCasing parent) {
    super(session, parent, false);
    
    m_logger.debug("Created");
    lineBuffering(false);
  }
  
  public ParseResult parse(ByteBuffer buf) 
    throws ParseException {
    m_logger.debug("parse");

    //Trace stuff
    getSmtpCasing().traceParse(buf);
    
    //Check for passthru
    if(isPassthru()) {
      return new ParseResult(new Chunk(buf));
    }

    //Duplicate the buffer, so if we have an exception we can enter passthru
    //mode.    
    ByteBuffer dup = buf.duplicate();

        
    List<Token> toks = new ArrayList<Token>();
    boolean done = false;
    
    while(buf.hasRemaining() && ! done) {
      try {
        Response resp = m_parser.parse(buf);
        if(resp != null) {
          m_logger.debug("Adding response token with code " + resp.getCode());
          toks.add(resp);
          m_parser.reset();
        }
        else {
          done = true;
          m_logger.debug("Need more bytes for response");
        }
      }
      catch(Exception ex) {
        //TODO bscott recover anything trapped in the parser
        m_logger.error("Exception parsing server response", ex);
        m_parser = null;
        declarePassthru();
        toks.clear();//Could truncate some stuff, but I suspect we're already hosed anyway.
        toks.add(PassThruToken.PASSTHRU);
        toks.add(new Chunk(dup));
        return new ParseResult(toks);
      }
    }

    //Compact the buffer
    buf = compactIfNotEmpty(buf);
    
    if(buf == null) {
      m_logger.debug("returning ParseResult with " +
        toks.size() + " tokens and a null buffer");    
    }
    else {
      m_logger.debug("returning ParseResult with " +
        toks.size() + " tokens and a buffer with " + buf.remaining() + " remaining");
    }
    return new ParseResult(toks, buf);
  }
                  

}