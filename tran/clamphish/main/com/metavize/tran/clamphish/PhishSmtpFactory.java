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

package com.metavize.tran.clamphish;

import static com.metavize.tran.util.Ascii.*;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.policy.Policy;
import com.metavize.mvvm.tapi.*;
import com.metavize.tran.mail.*;
import com.metavize.tran.mail.papi.*;
import com.metavize.tran.mail.papi.smtp.*;
import com.metavize.tran.mail.papi.smtp.sapi.Session;
import com.metavize.tran.spam.SpamSMTPConfig;
import com.metavize.tran.token.TokenHandler;
import com.metavize.tran.token.TokenHandlerFactory;
import org.apache.log4j.Logger;


public class PhishSmtpFactory
  implements TokenHandlerFactory {

  private static final String OUT_NOTIFY_SUB_TEMPLATE =
    "[FRAUD NOTIFICATION] re: $MIMEMessage:SUBJECT$";

  private static final String OUT_NOTIFY_BODY_TEMPLATE =
      "On $MIMEHeader:DATE$ a message from $MIMEMessage:FROM$ ($SMTPTransaction:FROM$) was received " + CRLF +
    "and determined to be PHISH.  The details of the report are as follows:" + CRLF + CRLF +
    "$SPAMReport:FULL$";

  private static final String IN_NOTIFY_SUB_TEMPLATE = OUT_NOTIFY_SUB_TEMPLATE;
  private static final String IN_NOTIFY_BODY_TEMPLATE = OUT_NOTIFY_BODY_TEMPLATE;

  private MailExport m_mailExport;
  private ClamPhishTransform m_phishImpl;
  private static final Logger m_logger =
    Logger.getLogger(PhishSmtpFactory.class);

  private SmtpNotifyMessageGenerator m_inNotifier;
  private SmtpNotifyMessageGenerator m_outNotifier;

  public PhishSmtpFactory(ClamPhishTransform impl) {
    Policy p = impl.getTid().getPolicy();
    m_mailExport = MailExportFactory.factory().getExport(p);
    m_phishImpl = impl;

    m_inNotifier = new SmtpNotifyMessageGenerator(
      IN_NOTIFY_SUB_TEMPLATE,
      IN_NOTIFY_BODY_TEMPLATE,
      false);

    m_outNotifier = new SmtpNotifyMessageGenerator(
      OUT_NOTIFY_SUB_TEMPLATE,
      OUT_NOTIFY_BODY_TEMPLATE,
      false);
  }


  public TokenHandler tokenHandler(TCPSession session) {

      boolean inbound = session.isInbound();

    SpamSMTPConfig spamConfig = inbound?
      m_phishImpl.getSpamSettings().getSMTPInbound():
      m_phishImpl.getSpamSettings().getSMTPOutbound();

    if(!spamConfig.getScan()) {
      m_logger.debug("Scanning disabled.  Return passthrough token handler");
      return Session.createPassthruSession(session);
    }

    SmtpNotifyMessageGenerator notifier =
      inbound?m_inNotifier:m_outNotifier;

    MailTransformSettings casingSettings = m_mailExport.getExportSettings();
    return new Session(session,
      new PhishSmtpHandler(session,
        inbound?casingSettings.getSmtpInboundTimeout():casingSettings.getSmtpOutboundTimeout(),
        inbound?casingSettings.getSmtpInboundTimeout():casingSettings.getSmtpOutboundTimeout(),
        m_phishImpl,
        spamConfig,
        notifier));

  }
}
