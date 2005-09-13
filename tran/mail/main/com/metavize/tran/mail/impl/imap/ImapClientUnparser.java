/**
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
import com.metavize.mvvm.tapi.event.TCPStreamer;
import com.metavize.tran.token.Chunk;
import com.metavize.tran.token.Token;
import com.metavize.tran.token.UnparseResult;
import com.metavize.tran.token.PassThruToken;
import com.metavize.tran.mail.papi.ContinuedMIMEToken;
import com.metavize.tran.mail.papi.imap.BeginImapMIMEToken;
import com.metavize.tran.mail.papi.imap.CompleteImapMIMEToken;
import com.metavize.tran.mail.papi.imap.UnparsableMIMEChunk;
import com.metavize.tran.mail.papi.imap.ImapChunk;

import org.apache.log4j.Logger;

/**
 * ...name says it all...
 */
class ImapClientUnparser
  extends ImapUnparser {

  private final Logger m_logger =
    Logger.getLogger(ImapClientUnparser.class);

  ImapClientUnparser(TCPSession session,
    ImapCasing parent) {
    super(session, parent, true);
    m_logger.debug("Created");
  }


  public UnparseResult unparse(Token token) {

    //TEMP so folks don't hit my test code
//    if(System.currentTimeMillis() > 0) {
//      getImapCasing().traceUnparse(((Chunk)token).getBytes());
//      return new UnparseResult(((Chunk)token).getBytes());
//    }

    if(token instanceof PassThruToken) {
      m_logger.debug("Received PASSTHRU token");
      declarePassthru();//Inform the parser of this state
      return UnparseResult.NONE;
    }
    if(token instanceof UnparsableMIMEChunk) {
      m_logger.debug("Unparsing UnparsableMIMEChunk");
      ByteBuffer buf = ((UnparsableMIMEChunk)token).getBytes();
      getImapCasing().traceUnparse(buf);
      return new UnparseResult(buf);
    }    
    if(token instanceof ImapChunk) {
      m_logger.debug("Unparsing ImapChunk");
      ByteBuffer buf = ((ImapChunk)token).getBytes();
      getImapCasing().traceUnparse(buf);
      return new UnparseResult(buf);      
    }
    if(token instanceof Chunk) {
      m_logger.debug("Unparsing Chunk");
      ByteBuffer buf = ((Chunk)token).getBytes();
      getImapCasing().traceUnparse(buf);
      return new UnparseResult(buf);      
    }
    if(token instanceof BeginImapMIMEToken) {
      m_logger.debug("Unparsing BeginImapMIMEToken");
      return new UnparseResult(
        getImapCasing().wrapUnparseStreamerForTrace(
          ((BeginImapMIMEToken) token).toImapTCPStreamer(true)));
    }
    if(token instanceof ContinuedMIMEToken) {
      ContinuedMIMEToken cmt = (ContinuedMIMEToken) token;
      if(cmt.shouldUnparse()) {
        m_logger.debug("Unparsing ContinuedMIMEToken (" +
          (cmt.isLast()?"last)":"not last)"));
        ByteBuffer buf = cmt.getBytes();
        getImapCasing().traceUnparse(buf);
        return new UnparseResult(buf);
      }
      else {
        m_logger.debug("Unparsing ContinuedMIMEToken (" +
          (cmt.isLast()?"last":"not last") + ") with nothing to unparse");
        return UnparseResult.NONE;
      }
    }
    if(token instanceof CompleteImapMIMEToken) {
      m_logger.debug("Unparsing CompleteImapMIMEToken");
      return new UnparseResult(
        getImapCasing().wrapUnparseStreamerForTrace(
          ((CompleteImapMIMEToken) token).toImapTCPStreamer(getPipeline(),
            true)
          ));
    }
    m_logger.warn("Unknown token type: " + token.getClass().getName());
    return new UnparseResult(token.getBytes());

  }

  @Override
  public TCPStreamer endSession() {
    m_logger.debug("End Session");
    return super.endSession();
  }

  @Override
  public void handleFinalized() {
    m_logger.debug("[handleFinalized()]");
  }
}