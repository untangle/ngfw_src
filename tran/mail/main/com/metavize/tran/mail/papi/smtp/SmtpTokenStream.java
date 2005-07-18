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

package com.metavize.tran.mail.papi.smtp;

import com.metavize.mvvm.tapi.*;
import com.metavize.tran.token.*;
import org.apache.log4j.Logger;
import com.metavize.mvvm.tapi.Pipeline;
import com.metavize.mvvm.MvvmContextFactory;

public final class SmtpTokenStream
  extends AbstractTokenHandler {

  private final Logger m_logger = Logger.getLogger(SmtpTokenStream.class);
  private static final NOOPHandler NOOP_HANDLER = new NOOPHandler();

  private SmtpTokenStreamHandler m_handler;
  private boolean m_passthru = false;
  private final Pipeline m_pipeline;

  public SmtpTokenStream(TCPSession session) {
    this(session, null);
  }
  public SmtpTokenStream(TCPSession session, SmtpTokenStreamHandler handler) {
    super(session);
    setHandler(handler);
    m_pipeline = MvvmContextFactory.context().pipelineFoundry().getPipeline(session.id());
  }  

  public SmtpTokenStreamHandler getHandler() {
    return m_handler;
  }
  public void setHandler(SmtpTokenStreamHandler handler) {
    m_handler = handler==null?
      NOOP_HANDLER:
      handler;
  }
  //FROM Client
  public TokenResult handleClientToken(Token token) 
    throws TokenException { 
    m_logger.debug("[handleClientToken] Called with token type \"" +
      token.getClass().getName() + "\"");

    //Check for passthrough
    if(m_passthru) {
      m_logger.debug("[handleClientToken] (In passthru)");
      return new TokenResult(new Token[] {token}, null);
    }

    TokenResultBuilder trb = new TokenResultBuilder(m_pipeline);

    //Passthru
    if(token instanceof PassThruToken) {
      m_logger.debug("[handleClientToken] Entering Passthru");
      m_passthru = true;
      trb.addTokenForServer(token);
      m_handler.passthru(trb);
      return trb.getTokenResult();
    }
    else if(token instanceof MAILCommand) {
      m_handler.handleMAILCommand(trb, (MAILCommand) token);
      return trb.getTokenResult();
    }
    else if(token instanceof RCPTCommand) {
      m_handler.handleRCPTCommand(trb, (RCPTCommand) token);
      return trb.getTokenResult();      
    }
    else if(token instanceof Command) {
      m_handler.handleCommand(trb, (Command) token);
      return trb.getTokenResult();      
    }
    else if(token instanceof BeginMIMEToken) {
      m_handler.handleBeginMIME(trb, (BeginMIMEToken) token);
      return trb.getTokenResult();      
    }
    else if(token instanceof ContinuedMIMEToken) {
      m_handler.handleContinuedMIME(trb, (ContinuedMIMEToken) token);
      return trb.getTokenResult();      
    }
    else if(token instanceof Chunk) {
      m_handler.handleChunkForServer(trb, (Chunk) token);
      return trb.getTokenResult();      
    }
    m_logger.error("Unexpected Token of type \"" +
      token.getClass().getName() + "\".  Pass it along");
    return new TokenResult(new Token[] {token}, null);
  }
   
  //FROM Server
  public TokenResult handleServerToken(Token token) 
    throws TokenException {
    m_logger.debug("[handleServerToken] Called with token type \"" +
      token.getClass().getName() + "\"");

    if(m_passthru) {
      m_logger.debug("[handleServerToken] (In passthru)");
      return new TokenResult(null, new Token[] {token});
    }
    
    TokenResultBuilder trb = new TokenResultBuilder(m_pipeline);

    //Passthru
    if(token instanceof PassThruToken) {
      m_logger.debug("[handleServerToken] Entering Passthru");
      m_passthru = true;
      trb.addTokenForClient(token);
      m_handler.passthru(trb);
      return trb.getTokenResult();
    }
    else if(token instanceof Response) {
      m_handler.handleResponse(trb, (Response) token);
      return trb.getTokenResult();      
    }
    else if(token instanceof Chunk) {
      m_handler.handleChunkForClient(trb, (Chunk) token);
      return trb.getTokenResult();      
    }
    m_logger.error("Unexpected Token of type \"" +
      token.getClass().getName() + "\".  Pass it along");
    return new TokenResult(null, new Token[] {token});
  }

  /**
   * As its name implies, does nothing except pass stuff through
   */
  private static class NOOPHandler implements SmtpTokenStreamHandler {

    public void passthru(TokenResultBuilder resultBuilder) {
      //Nothing to do
    }
  
    public void handleCommand(TokenResultBuilder resultBuilder,
      Command cmd) {
      resultBuilder.addTokenForServer(cmd);
    }
  
    public void handleMAILCommand(TokenResultBuilder resultBuilder,
      MAILCommand cmd) {
      resultBuilder.addTokenForServer(cmd);
    }
  
    public void handleRCPTCommand(TokenResultBuilder resultBuilder,
      RCPTCommand cmd) {
      resultBuilder.addTokenForServer(cmd);
    }
  
    public void handleBeginMIME(TokenResultBuilder resultBuilder,
      BeginMIMEToken token) {
      resultBuilder.addTokenForServer(token);
    }
      
    public void handleContinuedMIME(TokenResultBuilder resultBuilder,
      ContinuedMIMEToken token) {
      resultBuilder.addTokenForServer(token);
    }
      
    public void handleResponse(TokenResultBuilder resultBuilder,
      Response resp) {
      resultBuilder.addTokenForClient(resp);
    }
      
    public void handleChunkForClient(TokenResultBuilder resultBuilder,
      Chunk chunk) {
      resultBuilder.addTokenForClient(chunk);
    }
  
    public void handleChunkForServer(TokenResultBuilder resultBuilder,
      Chunk chunk) {
      resultBuilder.addTokenForServer(chunk);
    }
  }      
  
}