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

import static com.metavize.tran.util.Ascii.*;
import static com.metavize.tran.util.BufferUtil.*;

import com.metavize.tran.mail.papi.smtp.*;

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


public class SmtpServerParser
  extends AbstractParser {

  private final Pipeline pipeline;
  private final Logger m_logger = Logger.getLogger(SmtpServerParser.class);

  private final SmtpCasing m_parentCasing;
  private ResponseParser m_parser = new ResponseParser();
  
  public SmtpServerParser(TCPSession session,
    SmtpCasing parent) {
    super(session, false);
    m_logger.debug("Created");
    m_parentCasing = parent;
    lineBuffering(false);
    pipeline = MvvmContextFactory.context().
      pipelineFoundry().getPipeline(session.id());
  }
  
  public ParseResult parse(ByteBuffer buf) 
    throws ParseException {
    m_logger.debug("**DEBUG*** Parse");

    ByteBuffer dup = buf.duplicate();
    
    m_parentCasing.traceParse(buf);
    
    List<Token> toks = new ArrayList<Token>();

    boolean done = false;
    
    while(buf.hasRemaining() && ! done) {
      try {
        Response resp = m_parser.parse(buf);
        if(resp != null) {
          m_logger.debug("Adding response token");
          toks.add(resp);
          m_parser.reset();
        }
        else {
          done = true;
          m_logger.debug("Need more bytes for response");
        }
      }
      catch(Exception ex) {
        //TODO bscott figure out how to recover
        //TODO bscott recover anything trapped in the parser
        m_logger.error("Exception parsing server response", ex);
        m_parser = null;
        //TODO bscott Go into passthru mode?
        return new ParseResult(new Chunk(dup));
      }
    }
    
    buf.compact();
    if(buf.position() > 0 && buf.remaining() < 1024/*TODO bscott a real value*/) {
      ByteBuffer b = ByteBuffer.allocate(buf.capacity() + 1024);
      buf.flip();
      b.put(buf);
      buf = b;
    }
    else {
      if(!buf.hasRemaining()) {
        buf = null;
      }
    }
    m_logger.debug("returning ParseResult with " +
      toks.size() + " tokens and a " + (buf==null?"null":"") + " buffer");
    return new ParseResult(toks, buf);      
  }
                  
  public ParseResult parseEnd(ByteBuffer chunk)
    throws ParseException {
    if (chunk.hasRemaining()) {
      m_logger.warn("data trapped in read buffer: "
        + AsciiCharBuffer.wrap(chunk));
    }
    return new ParseResult();       
  }
  public TokenStreamer endSession() {
    m_logger.debug("End Session");
    m_parentCasing.endSession(true);
    return null;
  }
}