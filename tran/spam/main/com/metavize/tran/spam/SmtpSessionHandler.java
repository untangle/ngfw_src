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

import java.io.*;
import java.util.*;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.tapi.*;
import com.metavize.tran.mail.*;
import com.metavize.tran.mail.papi.*;
import com.metavize.tran.mail.papi.smtp.*;
import com.metavize.tran.mail.papi.smtp.sapi.*;
import com.metavize.tran.mime.*;
import com.metavize.tran.util.*;
import org.apache.log4j.Logger;
import com.metavize.mvvm.tran.Transform;
import static com.metavize.tran.util.Ascii.CRLF;
import com.metavize.tran.mime.LCString;
import java.net.InetAddress;


/**
 * Protocol Handler which is called-back as scanable messages
 * are encountered.
 */
public class SmtpSessionHandler
  extends BufferingSessionHandler {

  private final Logger m_logger = Logger.getLogger(SmtpSessionHandler.class);
  private final Pipeline m_pipeline;
  private final TempFileFactory m_fileFactory;
  private static final Logger m_eventLogger = MvvmContextFactory
    .context().eventLogger();

  private final SpamImpl m_spamImpl;
  private final SpamSMTPConfig m_config;
  private final SmtpNotifyMessageGenerator m_notifier;

  public SmtpSessionHandler(TCPSession session,
    long maxClientWait,
    long maxSvrWait,
    SpamImpl impl,
    SpamSMTPConfig config,
    SmtpNotifyMessageGenerator notifier) {

    super(config.getMsgSizeLimit(), maxClientWait, maxSvrWait, false);

    m_spamImpl = impl;
    m_config = config;
    m_notifier = notifier;
    m_pipeline = MvvmContextFactory.context().
      pipelineFoundry().getPipeline(session.id());
    m_fileFactory = new TempFileFactory(m_pipeline);
  }


  @Override
  public BPMEvaluationResult blockPassOrModify(MIMEMessage msg,
    SmtpTransaction tx,
    MessageInfo msgInfo) {
    m_logger.debug("[handleMessageCanBlock]");

    //I'm incrementing the count, even if the message is too big
    //or cannot be converted to file
    m_spamImpl.incrementScanCounter();

    //Scan the message
    File f = messageToFile(msg);
    if(f == null) {
      m_logger.error("Error writing to file.  Unable to scan.  Assume pass");
      m_spamImpl.incrementPassCounter();
      return PASS_MESSAGE;
    }

    if(f.length() > getGiveupSz()) {
      m_logger.debug("Message larger than " + getGiveupSz() + ".  Don't bother to scan");
      m_spamImpl.incrementPassCounter();
      return PASS_MESSAGE;
    }

    SMTPSpamMessageAction action = m_config.getMsgAction();

    SpamReport report = scanFile(f);

    //Handle error case
    if(report == null) {
      m_logger.error("Error scanning message.  Assume pass");
      m_spamImpl.incrementPassCounter();
      return PASS_MESSAGE;
    }


    //Create an event for the reports
    SpamSmtpEvent spamEvent = new SpamSmtpEvent(
      msgInfo,
      report.getScore(),
      report.isSpam(),
      report.isSpam()?action:SMTPSpamMessageAction.PASS,
      m_spamImpl.getScanner().getVendorName());
    m_eventLogger.info(spamEvent);

    //Mark headers regardless of other actions
    try {
      msg.getMMHeaders().removeHeaderFields(new LCString(m_config.getHeaderName()));
      msg.getMMHeaders().addHeaderField(m_config.getHeaderName(),
        m_config.getHeaderValue(report.isSpam()));
    }
    catch(HeaderParseException shouldNotHappen) {
      m_logger.error(shouldNotHappen);
    }

    if(report.isSpam()) {//BEGIN SPAM
      m_logger.debug("Spam found");

      //Perform notification (if we should)
      if(m_notifier.sendNotification(
        MvvmContextFactory.context().mailSender(),
        m_config.getNotifyAction(),
        msg,
        tx,
        tx, report)) {
        m_logger.debug("Notification handled without error");
      }
      else {
        m_logger.error("Error sending notification");
      }

      if(action == SMTPSpamMessageAction.PASS) {
        m_logger.debug("Although SPAM detected, pass message as-per policy");
        m_spamImpl.incrementPassCounter();
        return PASS_MESSAGE;
      }
      else if(action == SMTPSpamMessageAction.MARK) {
        m_logger.debug("Marking message as-per policy");
        m_spamImpl.incrementMarkCounter();
        MIMEMessage wrappedMsg = m_config.getMessageGenerator().wrap(msg, tx, report);
        return new BPMEvaluationResult(wrappedMsg);
      }
      else {//BLOCK
        m_logger.debug("Blocking SPAM message as-per policy");
        m_spamImpl.incrementBlockCounter();
        return BLOCK_MESSAGE;
      }
    }//ENDOF SPAM
    else {//BEGIN HAM
      m_logger.debug("Not spam");
      m_spamImpl.incrementPassCounter();
      return PASS_MESSAGE;
    }//ENDOF HAM
  }


  @Override
  public BlockOrPassResult blockOrPass(MIMEMessage msg,
    SmtpTransaction tx,
    MessageInfo msgInfo) {
    m_logger.debug("[handleMessageCanNotBlock]");

    m_spamImpl.incrementScanCounter();

    //Scan the message
    File f = messageToFile(msg);
    if(f == null) {
      m_logger.error("Error writing to file.  Unable to scan.  Assume pass");
      m_spamImpl.incrementPassCounter();
      return BlockOrPassResult.PASS;
    }

    if(f.length() > getGiveupSz()) {
      m_logger.debug("Message larger than " + getGiveupSz() + ".  Don't bother to scan");
      m_spamImpl.incrementPassCounter();
      return BlockOrPassResult.PASS;
    }

    SpamReport report = scanFile(f);

    //Handle error case
    if(report == null) {
      m_logger.error("Error scanning message.  Assume pass");
      m_spamImpl.incrementPassCounter();
      return BlockOrPassResult.PASS;
    }

    SMTPSpamMessageAction action = m_config.getMsgAction();
    
    //Check for the impossible-to-satisfy action of "REMOVE"
    if(action == SMTPSpamMessageAction.MARK) {
      //Change action now, as it'll make the event logs
      //more accurate
      m_logger.debug("Implicitly converting policy from \"MARK\"" +
        " to \"PASS\" as we have already begun to trickle");
      action = SMTPSpamMessageAction.PASS;
    }

    //Create an event for the reports
    SpamSmtpEvent spamEvent = new SpamSmtpEvent(
      msgInfo,
      report.getScore(),
      report.isSpam(),
      report.isSpam()?action:SMTPSpamMessageAction.PASS,
      m_spamImpl.getScanner().getVendorName());
    m_eventLogger.info(spamEvent);
    
    if(report.isSpam()) {
      m_logger.debug("Spam");

      if(action == SMTPSpamMessageAction.PASS) {
        m_logger.debug("Although SPAM detected, pass message as-per policy");
        m_spamImpl.incrementPassCounter();
        return BlockOrPassResult.PASS;
      }
      else if(action == SMTPSpamMessageAction.MARK) {
        m_logger.debug("Cannot mark at this time.  Simply pass");
        m_spamImpl.incrementPassCounter();
        return BlockOrPassResult.PASS;
      }
      else {//BLOCK
        m_logger.debug("Blocking SPAM message as-per policy");
        m_spamImpl.incrementBlockCounter();
        return BlockOrPassResult.BLOCK;
      }
    }
    else {
      m_logger.debug("Not Spam");
      m_spamImpl.incrementPassCounter();
      return BlockOrPassResult.PASS;
    }
  }

  /**
   * Wrapper that handles exceptions, and returns
   * null if there is a problem
   */
  private File messageToFile(MIMEMessage msg) {
  
    //Build the "fake" received header for SpamAssassin
    InetAddress clientAddr = getSession().getClientAddress();
    StringBuilder sb = new StringBuilder();
    sb.append("Received: ");
    sb.append("from ").append(getHELOEHLOName()).
      append(" (").append(clientAddr.getHostName()).
        append(" [").append(clientAddr.getHostAddress()).append("])").append(CRLF);
    sb.append("\tby mv-edgeguard; ").append(MIMEUtil.getRFC822Date());

    
    File ret = null;
    FileOutputStream fOut = null;
    try {
      ret = m_fileFactory.createFile("spamc_mv");
      fOut = new FileOutputStream(ret);
      BufferedOutputStream bOut = new BufferedOutputStream(fOut);
      MIMEOutputStream mimeOut = new MIMEOutputStream(bOut);
      mimeOut.writeLine(sb.toString());
      msg.writeTo(mimeOut);
      mimeOut.flush();
      bOut.flush();
      fOut.flush();
      fOut.close();
/*
      File copy = new File("TEMP_SPAM" + System.currentTimeMillis());
      FileOutputStream copyOut = new FileOutputStream(copy);
      byte[] buf = new byte[1024];
      FileInputStream copyIn = new FileInputStream(ret);
      int read = copyIn.read(buf);
      while(read > 0) {
        copyOut.write(buf, 0, read);
        read = copyIn.read(buf);
      }
      copyOut.flush();
      copyOut.close();
      copyIn.close();
*/      
      return ret;
    }
    catch(Exception ex) {
      try {fOut.close();}catch(Exception ignore){}
      try {ret.delete();}catch(Exception ignore){}
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
}
