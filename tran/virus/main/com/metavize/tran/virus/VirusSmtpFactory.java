/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: VirusSmtpFactory.java 194 2005-04-06 19:13:55Z rbscott $
 */
package com.metavize.tran.virus;

import static com.metavize.tran.util.Ascii.*;

import com.metavize.mvvm.MailSender;
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

public class VirusSmtpFactory
  implements TokenHandlerFactory {

  private static final String OUT_NOTIFY_SUB_TEMPLATE =
    "[VIRUS NOTIFICATION] re: $MIMEMessage:SUBJECT$";

  private static final String OUT_NOTIFY_BODY_TEMPLATE =
    "On $MIMEHeader:DATE$ a message from $MIMEMessage:FROM$ ($SMTPTransaction:FROM$) was received " + CRLF +
    "and found to contain the virus \"$VirusReport:VIRUS_NAME$\".  The infected portion of the email " + CRLF +
    "was removed";

  private static final String IN_NOTIFY_SUB_TEMPLATE = OUT_NOTIFY_SUB_TEMPLATE;
  private static final String IN_NOTIFY_BODY_TEMPLATE = OUT_NOTIFY_BODY_TEMPLATE;

  private final Logger m_logger =
    Logger.getLogger(VirusSmtpFactory.class);


  private SmtpNotifyMessageGenerator m_inNotifier;
  private SmtpNotifyMessageGenerator m_outNotifier;

  private final VirusTransformImpl m_virusImpl;
  private final MailExport m_mailExport;

  VirusSmtpFactory(VirusTransformImpl transform) {
    m_virusImpl = transform;
    Policy p = transform.getTid().getPolicy();
    m_mailExport = MailExportFactory.factory().getExport(p);

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
//    return new VirusSmtpHandler(session, m_virusImpl);

    boolean inbound = session.isInbound();

    VirusSMTPConfig virusConfig = inbound?
      m_virusImpl.getVirusSettings().getSMTPInbound():
      m_virusImpl.getVirusSettings().getSMTPOutbound();

    if(!virusConfig.getScan()) {
      m_logger.debug("Scanning disabled.  Return passthrough token handler");
      return Session.createPassthruSession(session);
    }

    SmtpNotifyMessageGenerator notifier =
      inbound?m_inNotifier:m_outNotifier;

    MailTransformSettings casingSettings = m_mailExport.getExportSettings();
    return new Session(session,
      new SmtpSessionHandler(
        session,
        inbound?casingSettings.getSmtpInboundTimeout():casingSettings.getSmtpOutboundTimeout(),
        inbound?casingSettings.getSmtpInboundTimeout():casingSettings.getSmtpOutboundTimeout(),
        m_virusImpl,
        virusConfig,
        notifier));


  }
}
