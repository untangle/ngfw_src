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


public class SpamSmtpFactory
  implements TokenHandlerFactory {

  private static final String OUT_MOD_SUB_TEMPLATE =
    "[SPAM] $MIMEMessage:SUBJECT$";

  private static final String OUT_MOD_BODY_TEMPLATE =
    "The attached message from $MIMEMessage:FROM$ ($SMTPTransaction:FROM$) was determined " + CRLF +
    "to be SPAM based on a score of $SPAMReport:SCORE$ where anything above $SPAMReport:THRESHOLD$" + CRLF +
    "is SPAM.  The details of the report are as follows:" + CRLF + CRLF +
    "$SPAMReport:FULL$";
    
  private static final String IN_MOD_SUB_TEMPLATE = OUT_MOD_SUB_TEMPLATE;
  private static final String IN_MOD_BODY_TEMPLATE = OUT_MOD_BODY_TEMPLATE;
  
  private MailExport m_mailExport;
  private SpamImpl m_spamImpl;
  private static final Logger m_logger =
    Logger.getLogger(SpamSmtpFactory.class);

  //TODO bscott These should be paramaterized in the Config objects
  private WrappedMessageGenerator m_inWrapper =
    new WrappedMessageGenerator(IN_MOD_SUB_TEMPLATE, IN_MOD_BODY_TEMPLATE);

  private WrappedMessageGenerator m_outWrapper =
    new WrappedMessageGenerator(OUT_MOD_SUB_TEMPLATE, OUT_MOD_BODY_TEMPLATE);


  public SpamSmtpFactory(SpamImpl impl) {
    m_mailExport = MailExportFactory.getExport();
    m_spamImpl = impl;
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

    MailTransformSettings casingSettings = m_mailExport.getExportSettings();
    return new Session(session,
      new SmtpSessionHandler(session,
        inbound?casingSettings.getSmtpInboundTimeout():casingSettings.getSmtpOutboundTimeout(),
        inbound?casingSettings.getSmtpInboundTimeout():casingSettings.getSmtpOutboundTimeout(),
        m_spamImpl,
        spamConfig,
        msgWrapper));

  }
}
