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

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.tran.mail.papi.WrappedMessageGenerator;
import com.metavize.tran.mail.papi.MessageInfo;
import com.metavize.tran.mail.papi.imap.BufferingImapTokenStreamHandler;
import com.metavize.tran.mime.MIMEMessage;
import com.metavize.tran.mime.MIMEPart;
import com.metavize.tran.mime.MIMEUtil;
import com.metavize.tran.mime.LCString;
import com.metavize.tran.mime.HeaderParseException;
import com.metavize.tran.util.TempFileFactory;
import java.io.File;
import org.apache.log4j.Logger;

public class SpamImapHandler
  extends BufferingImapTokenStreamHandler {

  private static final String SPAM_HEADER_NAME = "X-Spam-Flag";
  private static final LCString SPAM_HEADER_NAME_LC = new LCString(SPAM_HEADER_NAME);
  private static final String IS_SPAM_HEADER_VALUE = "YES";
  private static final String IS_HAM_HEADER_VALUE = "NO";  

  private final Logger m_logger =
    Logger.getLogger(SpamImapHandler.class);

  private static final Logger m_eventLogger = MvvmContextFactory
    .context().eventLogger();    

  private final SpamImpl m_spamImpl;
  private final SpamIMAPConfig m_config;
  private final WrappedMessageGenerator m_wrapper;
  private final TempFileFactory m_fileFactory;
  
  public SpamImapHandler(TCPSession session,
    long maxClientWait,
    long maxSvrWait,
    SpamImpl impl,
    SpamIMAPConfig config,
    WrappedMessageGenerator wrapper) {

    super(maxClientWait, maxSvrWait, config.getMsgSizeLimit());

    m_spamImpl = impl;
    m_config = config;
    m_wrapper = wrapper;
    m_fileFactory = new TempFileFactory(MvvmContextFactory.context().
      pipelineFoundry().getPipeline(session.id()));    

  }

  @Override
  public HandleMailResult handleMessage(MIMEMessage msg,
    MessageInfo msgInfo) {
    m_logger.debug("[handleMessage]");

    //I'm incrementing the count, even if the message is too big
    //or cannot be converted to file
    m_spamImpl.incrementScanCounter();

    //Scan the message
    File f = messageToFile(msg);
    if(f == null) {
      m_logger.error("Error writing to file.  Unable to scan.  Assume pass");
      m_spamImpl.incrementPassCounter();
      return HandleMailResult.forPassMessage();
    }

    if(f.length() > m_config.getMsgSizeLimit()) {
      m_logger.debug("Message larger than " + m_config.getMsgSizeLimit() + ".  Don't bother to scan");
      m_spamImpl.incrementPassCounter();
      return HandleMailResult.forPassMessage();
    }

    SpamMessageAction action = m_config.getMsgAction();

    SpamReport report = scanFile(f);

    //Handle error case
    if(report == null) {
      m_logger.error("Error scanning message.  Assume pass");
      m_spamImpl.incrementPassCounter();
      return HandleMailResult.forPassMessage();
    }


    //Create an event for the reports
    SpamLogEvent spamEvent = new SpamLogEvent(
      msgInfo,
      report.getScore(),
      report.isSpam(),
      report.isSpam()?action:SpamMessageAction.PASS,
      m_spamImpl.getScanner().getVendorName());
    m_eventLogger.info(spamEvent);

    //Mark headers regardless of other actions
    try {
      msg.getMMHeaders().removeHeaderFields(spamHeaderNameLC());
      msg.getMMHeaders().addHeaderField(spamHeaderName(),
        report.isSpam()?IS_SPAM_HEADER_VALUE:IS_HAM_HEADER_VALUE);
    }
    catch(HeaderParseException shouldNotHappen) {
      m_logger.error(shouldNotHappen);
    }

    if(report.isSpam()) {//BEGIN SPAM
      m_logger.debug("Spam found");

      if(action == SpamMessageAction.PASS) {
        m_logger.debug("Although SPAM detected, pass message as-per policy");
        m_spamImpl.incrementPassCounter();
        return HandleMailResult.forPassMessage();
      }
      else {
        m_logger.debug("Marking message as-per policy");
        m_spamImpl.incrementMarkCounter();
        MIMEMessage wrappedMsg = m_wrapper.wrap(msg, report);
        return HandleMailResult.forReplaceMessage(wrappedMsg);
      }
    }//ENDOF SPAM
    else {//BEGIN HAM
      m_logger.debug("Not spam");
      m_spamImpl.incrementPassCounter();
      return HandleMailResult.forPassMessage();
    }//ENDOF HAM
  }

  // A method so it can be overriden
  protected String spamHeaderName() {
    return SPAM_HEADER_NAME;
  }
  protected LCString spamHeaderNameLC() {
    return SPAM_HEADER_NAME_LC;
  }

  /**
   * Wrapper that handles exceptions, and returns
   * null if there is a problem
   */
  private File messageToFile(MIMEMessage msg) {
    //Get the part as a file
    try {
      return msg.toFile(m_fileFactory);
    }
    catch(Exception ex) {
      m_logger.error("Exception writing MIME Message to file", ex);
      return null;
    }
  }

  /**
   * Wrapper method around the real scanner, which
   * swallows exceptions and simply returns null
   */
  private SpamReport scanFile(File f) {
    //Attempt scan
    try {
      SpamReport ret = m_spamImpl.getScanner().scanFile(f, m_config.getStrength()/10.0f);
      if(ret == null) {
        m_logger.error("Received ERROR SpamReport");
        return null;
      }
      return ret;
    }
    catch(Exception ex) {
      m_logger.error("Exception scanning message", ex);
      return null;
    }
  }
  
}
