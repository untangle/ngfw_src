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

public class VirusSmtpFactory
  implements TokenHandlerFactory {


  private static final String OUT_MOD_SUB_TEMPLATE =
    "[VIRUS] $MIMEMessage:SUBJECT$";

  private static final String OUT_MOD_BODY_TEMPLATE =
    "The attached message from $MIMEMessage:FROM$ ($SMTPTransaction:FROM$) was found to contain " + CRLF +
    "the virus \"$VirusReport:VIRUS_NAME$\".  The infected portion of the attached email was removed" + CRLF;
    
  private static final String IN_MOD_SUB_TEMPLATE = OUT_MOD_SUB_TEMPLATE;
  private static final String IN_MOD_BODY_TEMPLATE = OUT_MOD_BODY_TEMPLATE;
  

  private final Logger m_logger =
    Logger.getLogger(VirusSmtpFactory.class);

  //TODO bscott These should be paramaterized in the Config objects
  private WrappedMessageGenerator m_inWrapper =
    new WrappedMessageGenerator(IN_MOD_SUB_TEMPLATE, IN_MOD_BODY_TEMPLATE);

  private WrappedMessageGenerator m_outWrapper =
    new WrappedMessageGenerator(OUT_MOD_SUB_TEMPLATE, OUT_MOD_BODY_TEMPLATE);  

  private final VirusTransformImpl m_virusImpl;
  private final MailExport m_mailExport;  

  VirusSmtpFactory(VirusTransformImpl transform) {
    m_virusImpl = transform;
    m_mailExport = MailExportFactory.getExport();
  }

  public TokenHandler tokenHandler(TCPSession session) {
//    return new VirusSmtpHandler(session, m_virusImpl);

    boolean inbound = session.direction() == IPSessionDesc.INBOUND;

    VirusSMTPConfig virusConfig = inbound?
      m_virusImpl.getVirusSettings().getSMTPInbound():
      m_virusImpl.getVirusSettings().getSMTPOutbound();

    if(!virusConfig.getScan()) {
      m_logger.debug("Scanning disabled.  Return passthrough token handler");
      return new SmtpTokenStream(session);
    }

    WrappedMessageGenerator msgWrapper =
      inbound?m_inWrapper:m_outWrapper;

    MailTransformSettings casingSettings = m_mailExport.getExportSettings();
    return new Session(session,
      new SmtpSessionHandler(
        session,
        inbound?casingSettings.getSmtpInboundTimeout():casingSettings.getSmtpOutboundTimeout(),
        inbound?casingSettings.getSmtpInboundTimeout():casingSettings.getSmtpOutboundTimeout(),
        m_virusImpl,
        virusConfig,
        msgWrapper));

    
  }
}
