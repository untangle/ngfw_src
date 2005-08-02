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

import com.metavize.mvvm.tapi.*;
import com.metavize.tran.token.TokenHandler;
import com.metavize.tran.token.TokenHandlerFactory;
import org.apache.log4j.Logger;

import com.metavize.tran.mail.papi.*;
import com.metavize.tran.mail.papi.smtp.*;
import com.metavize.tran.mail.papi.smtp.sapi.Session;
import com.metavize.tran.mail.papi.smtp.sapi.SimpleSessionHandler;
import com.metavize.tran.mail.*;
import com.metavize.tran.util.Template;
import static com.metavize.tran.util.Ascii.*;
import com.metavize.mvvm.MailSender;
import com.metavize.mvvm.MvvmContextFactory;


public class SpamSmtpFactory
  implements TokenHandlerFactory {

  //==================================
  // Note that there are a lot
  // of literals burned-into the
  // code.  In a later version,
  // these should be moved to
  // user-controled params in the
  // database
  //==================================

  //TODO bscott These should be paramaterized in the Config objects

  //TODO bscott why the heck would someone want to be notified of spam?
  //     isn't that spam itself?
    
  private static final String OUT_NOTIFY_SUB_TEMPLATE =
    "[SPAM NOTIFICATION] re: $MIMEMessage:SUBJECT$";
    
  private static final String OUT_NOTIFY_BODY_TEMPLATE =
    "On $MIMEHeader:DATE$ a message from $MIMEMessage:FROM$ ($SMTPTransaction:FROM$) was received " + CRLF +
    "and determined to be spam based on a score of $SPAMReport:SCORE$ (where anything " + CRLF +
    "above $SPAMReport:THRESHOLD$ is SPAM).  The details of the report are as follows:" + CRLF + CRLF +
    "$SPAMReport:FULL$";

  private static final String IN_NOTIFY_SUB_TEMPLATE = OUT_NOTIFY_SUB_TEMPLATE;
  private static final String IN_NOTIFY_BODY_TEMPLATE = OUT_NOTIFY_BODY_TEMPLATE;
  
  private MailExport m_mailExport;
  private SpamImpl m_spamImpl;
  private static final Logger m_logger =
    Logger.getLogger(SpamSmtpFactory.class);

  private WrappedMessageGenerator m_inWrapper =
    new WrappedMessageGenerator(SpamSettings.IN_MOD_SUB_TEMPLATE, SpamSettings.IN_MOD_BODY_SMTP_TEMPLATE);

  private WrappedMessageGenerator m_outWrapper =
    new WrappedMessageGenerator(SpamSettings.OUT_MOD_SUB_TEMPLATE, SpamSettings.OUT_MOD_BODY_SMTP_TEMPLATE);

  private SmtpNotifyMessageGenerator m_inNotifier;
  private SmtpNotifyMessageGenerator m_outNotifier;

  public SpamSmtpFactory(SpamImpl impl) {
    m_mailExport = MailExportFactory.getExport();
    m_spamImpl = impl;
    MailSender mailSender = MvvmContextFactory.context().mailSender();
    
    m_inNotifier = new SmtpNotifyMessageGenerator(
      IN_NOTIFY_SUB_TEMPLATE,
      IN_NOTIFY_BODY_TEMPLATE,
      false,
      mailSender);

    m_outNotifier = new SmtpNotifyMessageGenerator(
      OUT_NOTIFY_SUB_TEMPLATE,
      OUT_NOTIFY_BODY_TEMPLATE,
      false,
      mailSender);      
  }


  public TokenHandler tokenHandler(TCPSession session) {

    boolean inbound = session.direction() == IPSessionDesc.INBOUND;

    SpamSMTPConfig spamConfig = inbound?
      m_spamImpl.getSpamSettings().getSMTPInbound():
      m_spamImpl.getSpamSettings().getSMTPOutbound();

    if(!spamConfig.getScan()) {
      m_logger.debug("Scanning disabled.  Return passthrough token handler");
      return new SmtpTokenStream(session);
    }

    WrappedMessageGenerator msgWrapper =
      inbound?m_inWrapper:m_outWrapper;

    SmtpNotifyMessageGenerator notifier =
      inbound?m_inNotifier:m_outNotifier;

    MailTransformSettings casingSettings = m_mailExport.getExportSettings();
    return new Session(session,
      new SmtpSessionHandler(session,
        inbound?casingSettings.getSmtpInboundTimeout():casingSettings.getSmtpOutboundTimeout(),
        inbound?casingSettings.getSmtpInboundTimeout():casingSettings.getSmtpOutboundTimeout(),
        m_spamImpl,
        spamConfig,
        msgWrapper,
        notifier));

  }
}
