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

import static com.metavize.tran.util.Ascii.*;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.policy.Policy;
import com.metavize.mvvm.tapi.*;
import com.metavize.tran.mail.*;
import com.metavize.tran.mail.papi.*;
import com.metavize.tran.mail.papi.smtp.*;
import com.metavize.tran.mail.papi.smtp.sapi.Session;
import com.metavize.tran.token.TokenHandler;
import com.metavize.tran.token.TokenHandlerFactory;
import org.apache.log4j.Logger;


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

  private SmtpNotifyMessageGenerator m_inNotifier;
  private SmtpNotifyMessageGenerator m_outNotifier;

  public SpamSmtpFactory(SpamImpl impl) {
    Policy p = impl.getTid().getPolicy();
    m_mailExport = MailExportFactory.factory().getExport(p);
    m_spamImpl = impl;

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
      m_spamImpl.getSpamSettings().getSMTPInbound():
      m_spamImpl.getSpamSettings().getSMTPOutbound();

    if(!spamConfig.getScan()) {
      m_logger.debug("Scanning disabled.  Return passthrough token handler");
      return Session.createPassthruSession(session);
    }

    SmtpNotifyMessageGenerator notifier =
      inbound?m_inNotifier:m_outNotifier;

    MailTransformSettings casingSettings = m_mailExport.getExportSettings();
    return new Session(session,
      new SmtpSessionHandler(session,
        inbound?casingSettings.getSmtpInboundTimeout():casingSettings.getSmtpOutboundTimeout(),
        inbound?casingSettings.getSmtpInboundTimeout():casingSettings.getSmtpOutboundTimeout(),
        m_spamImpl,
        spamConfig,
        notifier));

  }
}
