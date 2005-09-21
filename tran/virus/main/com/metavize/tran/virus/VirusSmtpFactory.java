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

  private final Logger m_logger =
    Logger.getLogger(VirusSmtpFactory.class);

  private final VirusTransformImpl m_virusImpl;
  private final MailExport m_mailExport;

  VirusSmtpFactory(VirusTransformImpl transform) {
    m_virusImpl = transform;
    Policy p = transform.getTid().getPolicy();
    m_mailExport = MailExportFactory.factory().getExport(p);
  }

  public TokenHandler tokenHandler(TCPSession session) {

    boolean inbound = session.isInbound();

    VirusSMTPConfig virusConfig = inbound?
      m_virusImpl.getVirusSettings().getSMTPInbound():
      m_virusImpl.getVirusSettings().getSMTPOutbound();

    if(!virusConfig.getScan()) {
      m_logger.debug("Scanning disabled.  Return passthrough token handler");
      return Session.createPassthruSession(session);
    }

    MailTransformSettings casingSettings = m_mailExport.getExportSettings();
    return new Session(session,
      new SmtpSessionHandler(
        session,
        inbound?casingSettings.getSmtpInboundTimeout():casingSettings.getSmtpOutboundTimeout(),
        inbound?casingSettings.getSmtpInboundTimeout():casingSettings.getSmtpOutboundTimeout(),
        m_virusImpl,
        virusConfig));


  }
}
