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
import static com.metavize.tran.util.ASCIIUtil.bbToString;

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
import java.util.*;
import com.metavize.tran.mail.papi.MIMEAccumulator;

/**
 * ...name says it all...
 */
class SmtpServerUnparser
  extends SmtpUnparser {

  private final Logger m_logger = Logger.getLogger(SmtpServerUnparser.class);

  private ByteBufferByteStuffer m_byteStuffer;
  private MIMEAccumulator m_accumulator;

  SmtpServerUnparser(TCPSession session,
    SmtpCasing parent,
    CasingSessionTracker tracker) {
    super(session, parent, tracker, false);
    m_logger.debug("Created");
  }


  public UnparseResult unparse(Token token)
    throws UnparseException {
    
//    m_logger.debug("unparse token of type " + (token==null?"null":token.getClass().getName()));

    //-----------------------------------------------------------
    if(token instanceof PassThruToken) {
      m_logger.debug("Received PASSTHRU token");
      declarePassthru();//Inform the parser of this state
      return UnparseResult.NONE;
    }

    //-----------------------------------------------------------
    if(token instanceof Command) {
      Command command = (Command) token;

      if(command instanceof UnparsableCommand) {
        m_logger.debug("Received UnparsableCommand to pass.  Register " +
          "response action to know if there is a local parser error, or if " +
          "this is an errant command");
        getSessionTracker().commandReceived(command,
          new CommandParseErrorResponseCallback(command.getBytes()));
      }
      else if(command.getType() == Command.CommandType.STARTTLS) {
        m_logger.debug("Saw STARTTLS command.  Enqueue response action to go into " +
          "passthru if accepted");
        getSessionTracker().commandReceived(command, new TLSResponseCallback());
      }
      else {
        m_logger.debug("Send command to server: " +
          command.toDebugString());
        getSessionTracker().commandReceived(command);  
      }
      ByteBuffer buf = token.getBytes();
      getSmtpCasing().traceUnparse(buf);
      return new UnparseResult(buf);
    }

    //-----------------------------------------------------------
    if(token instanceof BeginMIMEToken) {
      m_logger.debug("Send BeginMIMEToken to server");
      getSessionTracker().beginMsgTransmission();
      BeginMIMEToken bmt = (BeginMIMEToken) token;
      //Initialize the byte stuffer.
      m_byteStuffer = new ByteBufferByteStuffer();
      m_accumulator = bmt.getMIMEAccumulator();
      return new UnparseResult(
        getSmtpCasing().wrapUnparseStreamerForTrace(
          bmt.toTCPStreamer(m_byteStuffer)));
    }

    //-----------------------------------------------------------
    if(token instanceof CompleteMIMEToken) {
      m_logger.debug("Send CompleteMIMEToken to server");
      getSessionTracker().beginMsgTransmission();
      return new UnparseResult(
        getSmtpCasing().wrapUnparseStreamerForTrace(
        ((CompleteMIMEToken) token).toTCPStreamer(getPipeline(), true)));
    }
    //-----------------------------------------------------------
    if(token instanceof ContinuedMIMEToken) {
      ContinuedMIMEToken continuedToken = (ContinuedMIMEToken) token;

      ByteBuffer sink = null;
      if(continuedToken.shouldUnparse()) {
        m_logger.debug("Sending continued MIME chunk to server");
//        continuedToken.getMIMEChunk().superDebugMe(m_logger, "[handleContinuedMIME()]");
        ByteBuffer buf = token.getBytes();
        sink = ByteBuffer.allocate(buf.remaining() + (m_byteStuffer.getLeftoverCount()*2));
        m_byteStuffer.transfer(buf, sink);
        m_logger.debug("After byte stuffing, wound up with: " + sink.remaining() + " bytes");
      }
      else {
        m_logger.debug("Continued MIME chunk should not go to server (already sent or empty)");
      }
      if(continuedToken.getMIMEChunk().isLast()) {
        m_logger.debug("Last MIME chunk");
        ByteBuffer remainder = m_byteStuffer.getLast(true);
        if(sink != null) {
          getSmtpCasing().traceUnparse(sink);
        }
        getSmtpCasing().traceUnparse(remainder);
        m_byteStuffer = null;
        m_accumulator.dispose();
        m_accumulator = null;
        return new UnparseResult(
          sink==null?
          new ByteBuffer[] {remainder}:
          new ByteBuffer[] {sink, remainder});
      }
      else {
        if(sink != null) {
          getSmtpCasing().traceUnparse(sink);
          return new UnparseResult(sink);
        }
        else {
          m_logger.debug("Continued token empty (return nothing)");
          return UnparseResult.NONE;
        }
      }
    }
    //-----------------------------------------------------------
    if(token instanceof Chunk) {
      ByteBuffer buf = token.getBytes();
      m_logger.debug("Sending chunk (" + buf.remaining() + " bytes) to server");
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
   * for the response to a command that could
   * not be parsed.
   */
  class CommandParseErrorResponseCallback
    implements CasingSessionTracker.ResponseAction {

    private String m_offendingCommand;

    CommandParseErrorResponseCallback(ByteBuffer bufWithOffendingLine) {
      m_offendingCommand = bbToString(bufWithOffendingLine);
    }
    
    public void response(int code) {
      if(code < 300) {
        m_logger.error("Parser could not parse command line \"" +
          m_offendingCommand + "\" yet accepted by server.  Parser error.  Enter passthru");
        declarePassthru();
      }
      else {
        m_logger.debug("Command \"" + m_offendingCommand + "\" unparsable, and rejected " +
          "by server.  Do not enter passthru (assume errant client)");
      }
    }    
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
