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

import java.io.*;

import java.nio.ByteBuffer;

import com.metavize.mvvm.*;
import com.metavize.mvvm.tapi.*;
import com.metavize.mvvm.tapi.event.TCPStreamer;
import com.metavize.tran.mail.papi.ByteBufferByteStuffer;
import com.metavize.tran.mail.papi.smtp.*;
import com.metavize.tran.token.*;
import com.metavize.tran.util.*;
import org.apache.log4j.Logger;

/**
 * ...name says it all...
 */
class SmtpServerUnparser
  extends SmtpUnparser {

  private final Logger m_logger = Logger.getLogger(SmtpServerUnparser.class);

  private ByteBufferByteStuffer m_byteStuffer;

  SmtpServerUnparser(TCPSession session,
    SmtpCasing parent,
    CasingSessionTracker tracker) {
    super(session, parent, tracker, false);
    m_logger.debug("Created");
  }


  public UnparseResult unparse(Token token)
    throws UnparseException {
    
    m_logger.debug("unparse token of type " + (token==null?"null":token.getClass().getName()));

    //-----------------------------------------------------------
    if(token instanceof PassThruToken) {
      m_logger.debug("Received PASSTHRU token");
      declarePassthru();//Inform the parser of this state
      return UnparseResult.NONE;
    }

    //-----------------------------------------------------------
    if(token instanceof MAILCommand) {
      MAILCommand mc = (MAILCommand) token;
      getSessionTracker().commandReceived(mc);      
      m_logger.debug("Received MAIL Commandfor address \"" +
        mc.getAddress() + "\"");
    }

    //-----------------------------------------------------------
    if(token instanceof RCPTCommand) {
      RCPTCommand mc = (RCPTCommand) token;
      getSessionTracker().commandReceived(mc);
      m_logger.debug("Received RCPT Commandfor address \"" +
        mc.getAddress() + "\"");
    }

    //-----------------------------------------------------------
    if(token instanceof Command) {
      Command command = (Command) token;
      m_logger.debug("Received Command \"" +
        command.getType() + "\" to pass");
      if(command.getType() == Command.CommandType.STARTTLS) {
        m_logger.debug("Saw STARTTLS command.  Enqueue response action to go into " +
          "passthru if accepted");
        getSessionTracker().commandReceived(command, new TLSResponseCallback());
      }
      else {
        getSessionTracker().commandReceived(command);  
      }
      ByteBuffer buf = token.getBytes();
      getSmtpCasing().traceUnparse(buf);
      return new UnparseResult(buf);
    }

    //-----------------------------------------------------------
    if(token instanceof BeginMIMEToken) {
      m_logger.debug("Received BeginMIMEToken to pass");
      getSessionTracker().beginMsgTransmission();
      ByteBuffer buf = token.getBytes();
      getSmtpCasing().traceUnparse(buf);
      m_byteStuffer = new ByteBufferByteStuffer();
      m_byteStuffer.advancePastHeaders();
      return new UnparseResult(buf);
    }

    //-----------------------------------------------------------
    if(token instanceof CompleteMIMEToken) {
      m_logger.debug("Received CompleteMIMEToken to pass");
      //TODO bscott Fix this hack once we talk to Aaron
      getSessionTracker().beginMsgTransmission();
      try {
        CompleteMIMEToken cmt = (CompleteMIMEToken) token;
        
        ByteBuffer tempBuf = ((CompleteMIMEToken) token).getMessage().toByteBuffer();
        m_logger.debug("About to byte stuff buffer of length: " + tempBuf.remaining());
        ByteBufferByteStuffer tempBBBS = new ByteBufferByteStuffer();
        ByteBuffer b1 = ByteBuffer.allocate(tempBuf.remaining());
        tempBBBS.transfer(tempBuf, b1);
        ByteBuffer b2 = tempBBBS.getLast(true);
  
        ByteBuffer finalBuf = ByteBuffer.allocate(b1.remaining() + b2.remaining());
        finalBuf.put(b1).put(b2);
        finalBuf.flip();
        m_logger.debug("Returning buffer of length: " + finalBuf.remaining());
        return new UnparseResult(finalBuf);
      }
      catch(IOException ex) {
        m_logger.error(ex);
        return null;//new UnparseResult();
      }
/*     
      return new UnparseResult(((CompleteMIMEToken) token).toTCPStreamer(m_pipeline));
*/       
    }    
    //-----------------------------------------------------------
    if(token instanceof ContinuedMIMEToken) {
      boolean last = ((ContinuedMIMEToken) token).isLast();
      ByteBuffer buf = token.getBytes();
      m_logger.debug("Received ContinuedMIMEToken (" +
        buf.remaining() + " bytes) to pass (" +
        (last?"":"not ") + "last)");

      ByteBuffer sink = ByteBuffer.allocate(buf.remaining());
      m_byteStuffer.transfer(buf, sink);
      m_logger.debug("After byte stuffing, wound up with: " + sink.remaining() + " bytes");
      if(last) {
        ByteBuffer remainder = m_byteStuffer.getLast(true);
        getSmtpCasing().traceUnparse(sink);
        getSmtpCasing().traceUnparse(remainder);
        return new UnparseResult(new ByteBuffer[] {sink, remainder});
      }
      else {
        getSmtpCasing().traceUnparse(sink);
        return new UnparseResult(sink);
      }
    }
    //-----------------------------------------------------------
    if(token instanceof Chunk) {
      ByteBuffer buf = token.getBytes();
      m_logger.debug("Received Chunk to pass (" + buf.remaining() + ")");
      getSmtpCasing().traceUnparse(buf);
      return new UnparseResult(buf);
    }

    //-----------------------------------------------------------
    if(token instanceof MetadataToken) {
      //Don't pass along metadata tokens
      return UnparseResult.NONE;
    }     

    //Default (bad) case
    m_logger.error("Received unknown \"" + token.getClass().getName() + "\" token");
    return new UnparseResult(token.getBytes());
  }

  private void tlsStarting() {
    m_logger.debug("TLS Command accepted.  Enter passthru mode so as to not attempt to parse cyphertext");
    declarePassthru();//Inform the parser of this state
  }


  //================ Inner Class =================

  /**
   * Callback registered with the CasingSessionTracker
   * for the response to the STARTTLS command
   */  
  class TLSResponseCallback
    implements CasingSessionTracker.ResponseAction {
    public void response(int code) {
      if(code < 300) {
        tlsStarting();
      }
      else {
        m_logger.debug("STARTTLS command rejected.  Do not go into passthru");
      }      
    }    
  }
}
