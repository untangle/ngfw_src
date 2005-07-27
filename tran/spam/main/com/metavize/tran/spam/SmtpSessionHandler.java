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

package com.metavize.tran.spam;
import org.apache.log4j.Logger;
import com.metavize.tran.mail.*;
import com.metavize.tran.mail.papi.*;
import com.metavize.tran.mail.papi.smtp.sapi.*;
import com.metavize.tran.mail.papi.smtp.*;
import com.metavize.tran.mime.*;
import com.metavize.tran.util.*;
import com.metavize.mvvm.tapi.*;
import com.metavize.mvvm.MvvmContextFactory;
import java.util.*;
import java.io.*;


/**
 * Protocol Handler which is called-back as scanable messages
 * are encountered.
 */
public class SmtpSessionHandler
  extends BufferingSessionHandler {

  //TODO bscott talk to Aaron/John re: threshold.  It was burned-into the last version.
  //     Do we normalize at each impl?  Should this be a property of each class of scanner?
  private static final float THRESHOLD = 5;

  private static final String SPAM_HEADER_NAME = "X-Spam-Flag";
  private static final LCString SPAM_HEADER_NAME_LC = new LCString(SPAM_HEADER_NAME);
  private static final String IS_SPAM_HEADER_VALUE = "YES";
  private static final String IS_HAM_HEADER_VALUE = "NO";
  
  private final Logger m_logger = Logger.getLogger(SmtpSessionHandler.class);
  private final Pipeline m_pipeline;
  private final MyFileFactory m_fileFactory = new MyFileFactory();  
  private static final Logger m_eventLogger = MvvmContextFactory
    .context().eventLogger();

  private static final int GIVEUP_SZ = 1 << 18;//256k

  private final long m_maxClientWait;
  private final long m_maxServerWait;
  private final SpamImpl m_spamImpl;
  private final SpamSMTPConfig m_config;

  private final WrappedMessageGenerator m_wrapper;

  public SmtpSessionHandler(TCPSession session,
    long maxClientWait,
    long maxSvrWait,
    SpamImpl impl,
    SpamSMTPConfig config,
    WrappedMessageGenerator wrapper) {
    
    m_logger.debug("Created with client wait " +
      maxClientWait + " and server wait " + maxSvrWait);
    m_maxClientWait = maxClientWait<=0?Integer.MAX_VALUE:maxClientWait;
    m_maxServerWait = maxSvrWait<=0?Integer.MAX_VALUE:maxSvrWait;
    m_spamImpl = impl;
    m_config = config;
    m_wrapper = wrapper;
    m_pipeline = MvvmContextFactory.context().
      pipelineFoundry().getPipeline(session.id());     
  }

  @Override
  public int getGiveupSz() {
    return GIVEUP_SZ;
  }
  
  @Override
  public long getMaxClientWait() {
    return m_maxClientWait;
  }
  
  @Override
  public long getMaxServerWait() {
    return m_maxServerWait;
  }
  
  @Override
  public boolean isBufferAndTrickle() {
    //TODO bscott Should we trickle?  Sounds complicated for little mails    
    return false;
  }
  
  @Override
  public BPMEvaluationResult blockPassOrModify(MIMEMessage msg,
    SmtpTransaction tx,
    MessageInfo msgInfo) {
    m_logger.debug("[handleMessageCanBlock]");

    //Scan the message
    SpamReport report = scan(msg);

    //Handle error case
    if(report == null) {
      m_logger.error("Error scanning message.  Assume pass");
      return PASS_MESSAGE;
    }

    //Mark headers regardless of other actions
    try {
      msg.getMMHeaders().removeHeaderFields(SPAM_HEADER_NAME_LC);
      msg.getMMHeaders().addHeaderField(SPAM_HEADER_NAME,
        report.isSpam()?IS_SPAM_HEADER_VALUE:IS_HAM_HEADER_VALUE);
    }
    catch(HeaderParseException shouldNotHappen) {
      m_logger.error(shouldNotHappen);
    }    
    
    if(report.isSpam()) {//BEGIN SPAM
      m_logger.debug("Spam found");

      SMTPSpamMessageAction action = m_config.getMsgAction();

      //Create an event for the reports      
      SpamSmtpEvent spamEvent = new SpamSmtpEvent(
        msgInfo,
        report.getScore(),
        report.isSpam(),//TODO bscott Isn't this redundant?  Don't we only log on negative cases?
        action,
        m_spamImpl.getScanner().getVendorName());
      m_eventLogger.info(spamEvent);

      if(action == SMTPSpamMessageAction.PASS) {
        m_logger.debug("Although SPAM detected, pass message as-per policy");
        return PASS_MESSAGE;
      }
      else if(action == SMTPSpamMessageAction.MARK) {
        m_logger.debug("Marking message as-per policy");
        MIMEMessage wrappedMsg = m_wrapper.wrap(msg, tx, report);
        return new BPMEvaluationResult(wrappedMsg);
      }
      else {//BLOCK
        m_logger.debug("Blocking SPAM message as-per policy");
        return BLOCK_MESSAGE;
      }
    }//ENDOF SPAM
    else {//BEGIN HAM
      m_logger.debug("Not spam");
      return PASS_MESSAGE;
    }//ENDOF HAM
  }

  
  @Override
  public BlockOrPassResult blockOrPass(MIMEMessage msg,
    SmtpTransaction tx,
    MessageInfo msgInfo) {
    m_logger.debug("[handleMessageCanNotBlock]");
    
    //Scan the message
    SpamReport report = scan(msg);

    //Handle error case
    if(report == null) {
      m_logger.error("Error scanning message.  Assume pass");
      return BlockOrPassResult.PASS;
    }
    if(report.isSpam()) {
      m_logger.debug("Spam");
      
      SMTPSpamMessageAction action = m_config.getMsgAction();

      //Create an event for the reports      
      SpamSmtpEvent spamEvent = new SpamSmtpEvent(
        msgInfo,
        report.getScore(),
        report.isSpam(),//TODO bscott Isn't this redundant?  Don't we only log on negative cases?
        action,
        m_spamImpl.getScanner().getVendorName());
      m_eventLogger.info(spamEvent);

      if(action == SMTPSpamMessageAction.PASS) {
        m_logger.debug("Although SPAM detected, pass message as-per policy");
        return BlockOrPassResult.PASS;
      }
      else if(action == SMTPSpamMessageAction.MARK) {
        m_logger.debug("Cannot mark at this time.  Simply pass");
        return BlockOrPassResult.PASS;
      }
      else {//BLOCK
        m_logger.debug("Blocking SPAM message as-per policy");
        return BlockOrPassResult.BLOCK;
      }      
    }
    else {
      m_logger.debug("Not Spam");
      return BlockOrPassResult.PASS;
    }
  }

  /**
   * Wrapper method around the real scanner, which
   * swallows exceptions and simply returns null
   */
  private SpamReport scan(MIMEMessage msg) {
  
    //Get the part as a file
    File f = null;
    try {
      f = msg.getContentAsFile(m_fileFactory, true);
    }
    catch(Exception ex) {
      m_logger.error("Exception writing MIME Message to file", ex);
      return null;
    }

    //Attempt scan
    try {
      SpamReport ret = m_spamImpl.getScanner().scanFile(f, THRESHOLD);
      if(ret == null || ret == SpamReport.ERROR) {
        m_logger.error("Received ERROR SpamReport");
        return null;
      }
      return ret;
    }
    catch(Exception ex) {
      m_logger.error("Exception scanning message");
      return null;
    }
    

/*
    //Fake Negative  
    m_logger.debug("Currently pretending this mail is not spam");
    ReportItem ri = new ReportItem(0,
      "FakeCategory");
    List<ReportItem> list = new ArrayList<ReportItem>();
    list.add(ri);
    return new SpamReport(list, THRESHOLD);
*/    
/*
    //Fake Positive  
    m_logger.debug("Currently pretending this mail is spam");
    ReportItem ri = new ReportItem(THRESHOLD + 1,
      "FakeCategory");
    List<ReportItem> list = new ArrayList<ReportItem>();
    list.add(ri);
    return new SpamReport(list, THRESHOLD);
*/    
  }

  //================= Inner Class =================

  /**
   * Acts as a FileFactory, to create temp files
   * when a MIME part needs to be decoded to disk.
   */
  private class MyFileFactory implements FileFactory {
    public File createFile(String name)
      throws IOException {
      return createFile();
    }

    public File createFile()
      throws IOException {
      return m_pipeline.mktemp();
    }
  }  
  
}