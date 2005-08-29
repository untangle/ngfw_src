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

import com.metavize.mvvm.tapi.*;
import com.metavize.tran.util.Template;
import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.tran.token.TokenHandler;
import com.metavize.tran.token.TokenHandlerFactory;
import com.metavize.tran.mail.*;
import com.metavize.tran.mail.papi.*;
import com.metavize.tran.mail.papi.smtp.*;
import com.metavize.tran.mail.papi.smtp.sapi.Session;
import com.metavize.tran.mail.papi.smtp.sapi.SimpleSessionHandler;
import static com.metavize.tran.util.Ascii.*;
import org.apache.log4j.Logger;
import com.metavize.mvvm.MailSender;
import com.metavize.mvvm.MvvmContextFactory;

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

  //TODO bscott These should be paramaterized in the Config objects
  private WrappedMessageGenerator m_inWrapper =
    new WrappedMessageGenerator(VirusSettings.IN_MOD_SUB_TEMPLATE, VirusSettings.IN_MOD_BODY_SMTP_TEMPLATE);

  private WrappedMessageGenerator m_outWrapper =
    new WrappedMessageGenerator(VirusSettings.OUT_MOD_SUB_TEMPLATE, VirusSettings.OUT_MOD_BODY_SMTP_TEMPLATE);

  private SmtpNotifyMessageGenerator m_inNotifier;
  private SmtpNotifyMessageGenerator m_outNotifier;    

  private final VirusTransformImpl m_virusImpl;
  private final MailExport m_mailExport;  

  VirusSmtpFactory(VirusTransformImpl transform) {
    m_virusImpl = transform;
    m_mailExport = MailExportFactory.getExport();

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

    boolean inbound = session.direction() == IPSessionDesc.INBOUND;

    VirusSMTPConfig virusConfig = inbound?
      m_virusImpl.getVirusSettings().getSMTPInbound():
      m_virusImpl.getVirusSettings().getSMTPOutbound();

    if(!virusConfig.getScan()) {
      m_logger.debug("Scanning disabled.  Return passthrough token handler");
      return Session.createPassthruSession(session);
    }

    WrappedMessageGenerator msgWrapper =
      inbound?m_inWrapper:m_outWrapper;

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
        msgWrapper,
        notifier));

    
  }
}
