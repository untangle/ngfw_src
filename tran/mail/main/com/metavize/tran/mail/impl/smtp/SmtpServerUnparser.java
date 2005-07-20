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

import com.metavize.tran.mail.papi.ByteBufferByteStuffer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.LinkedList;
import java.util.List;

import com.metavize.mvvm.*;
import com.metavize.mvvm.tapi.*;
import com.metavize.tran.token.*;
import com.metavize.tran.util.*;
import org.apache.log4j.Logger;

public class SmtpServerUnparser
  extends AbstractUnparser {

  private final Logger m_logger = Logger.getLogger(SmtpServerUnparser.class);
  private final Pipeline m_pipeline;

  private final SmtpCasing m_parentCasing;

  private ByteBufferByteStuffer m_byteStuffer;
  
  public SmtpServerUnparser(TCPSession session,
    SmtpCasing parent) {
    super(session, false);
    m_logger.debug("Created");
    m_parentCasing = parent;
    m_pipeline = MvvmContextFactory.context().
      pipelineFoundry().getPipeline(session.id());    
  }


  public UnparseResult unparse(Token token)
    throws UnparseException {
    m_logger.debug("**DEBUG*** unparse");
     
    if(token instanceof PassThruToken) {
      m_logger.error("Received PASSTHRU token");
      return new UnparseResult(token.getBytes());
    }
    if(token instanceof MAILCommand) {
      MAILCommand mc = (MAILCommand) token;
      m_logger.debug("Received MAIL Commandfor address \"" +
        mc.getAddress() + "\"");
    }
    if(token instanceof RCPTCommand) {
      RCPTCommand mc = (RCPTCommand) token;
      m_logger.debug("Received RCPT Commandfor address \"" +
        mc.getAddress() + "\"");
    }  
    if(token instanceof Command) {
      m_logger.debug("Received Command \"" +
        (((Command) token).getType()) + "\" to pass");
      
      ByteBuffer buf = token.getBytes();
      m_parentCasing.traceUnparse(buf);
      return new UnparseResult(buf);      
    }
    if(token instanceof BeginMIMEToken) {
      m_logger.debug("Received BeginMIMEToken to pass");
      ByteBuffer buf = token.getBytes();
      m_parentCasing.traceUnparse(buf);
      m_byteStuffer = new ByteBufferByteStuffer();
      m_byteStuffer.advancePastHeaders();
      return new UnparseResult(buf);      
    }
    if(token instanceof CompleteMIMEToken) {
      m_logger.debug("Received CompleteMIMEToken to pass");
      //TODO bscott What about my nifty trace stuff?
      return new UnparseResult(((CompleteMIMEToken) token).toTokenStreamer(m_pipeline));      
    }    
    if(token instanceof ContinuedMIMEToken) {
      boolean last = ((ContinuedMIMEToken) token).isLast();
      ByteBuffer buf = token.getBytes();
      m_logger.debug("Received ContinuedMIMEToken (" +
        buf.remaining() + " bytes) to pass (" +
        (last?"":"not ") + "last)");

      ByteBuffer sink = ByteBuffer.allocate(buf.remaining());
      m_byteStuffer.transfer(buf, sink);
      sink.flip();
      m_logger.debug("After stuffing, wound up with: " + sink.remaining() + " bytes");
      if(last) {
        ByteBuffer remainder = m_byteStuffer.getLast(true);
        m_parentCasing.traceUnparse(sink);
        m_parentCasing.traceUnparse(remainder);
        return new UnparseResult(new ByteBuffer[] {sink, remainder});
      }
      else {
        m_parentCasing.traceUnparse(sink);
        return new UnparseResult(sink); 
      }
    }
    if(token instanceof Chunk) {
      ByteBuffer buf = token.getBytes();
      m_logger.debug("Received Chunk to pass (" + buf.remaining() + ")");
      m_parentCasing.traceUnparse(buf);
      return new UnparseResult(buf);
    }
    m_logger.debug("Received unknown \"" + token.getClass().getName() + "\" token");
    return new UnparseResult(token.getBytes());

  }
  
  public TokenStreamer endSession() {
    m_logger.debug("End Session");
    m_parentCasing.endSession(false);
    return null;
  }                           
}