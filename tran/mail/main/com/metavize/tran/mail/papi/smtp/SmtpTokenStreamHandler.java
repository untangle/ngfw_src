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


/**
 * Note if the handler pukes in a horrible
 * way that you want all other
 * transforms to ignore this session, make sure to
 * pass-along Passthru tokens in each direction.
 * 
 */
public interface SmtpTokenStreamHandler {

  /**
   * Callback indicating that this conversation
   * is entering passthru mode.  Unlike some of the
   * other methods on this class, the caller should
   * <b>not</b> assume responsibility to re-passing
   * the Passthru token.  This has already been done
   * by the caller.
   * <br>
   * Implementations should pass along any queued
   * client/server data.  This will be the last
   * time the Handler is called for any token-handling
   * methods.
   */
  public void passthru(TokenResultBuilder resultBuilder);

  public void handleCommand(TokenResultBuilder resultBuilder,
    Command cmd);

  public void handleMAILCommand(TokenResultBuilder resultBuilder,
    MAILCommand cmd);

  public void handleRCPTCommand(TokenResultBuilder resultBuilder,
    RCPTCommand cmd);

  public void handleBeginMIME(TokenResultBuilder resultBuilder,
    BeginMIMEToken token);
    
  public void handleContinuedMIME(TokenResultBuilder resultBuilder,
    ContinuedMIMEToken token);
    
  public void handleResponse(TokenResultBuilder resultBuilder,
    Response resp);
    
  public void handleChunkForClient(TokenResultBuilder resultBuilder,
    Chunk chunk);

  public void handleChunkForServer(TokenResultBuilder resultBuilder,
    Chunk chunk);    
    


}