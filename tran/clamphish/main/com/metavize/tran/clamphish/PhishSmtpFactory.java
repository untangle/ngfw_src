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

import com.metavize.tran.token.TokenHandlerFactory;
import com.metavize.tran.mail.papi.MailExport;
import com.metavize.tran.mail.papi.MailExportFactory;
import com.metavize.tran.mail.papi.MailTransformSettings;
import org.apache.log4j.Logger;
import com.metavize.mvvm.policy.Policy;
import com.metavize.tran.token.TokenHandler;
import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.tran.mail.papi.smtp.sapi.Session;
import com.metavize.tran.spam.SpamSMTPConfig;


public class PhishSmtpFactory
  implements TokenHandlerFactory {

  private MailExport m_mailExport;
  private ClamPhishTransform m_phishImpl;
  private static final Logger m_logger =
    Logger.getLogger(PhishSmtpFactory.class);

  public PhishSmtpFactory(ClamPhishTransform impl) {
    Policy p = impl.getTid().getPolicy();
    m_mailExport = MailExportFactory.factory().getExport(p);
    m_phishImpl = impl;
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

    MailTransformSettings casingSettings = m_mailExport.getExportSettings();
    return new Session(session,
      new PhishSmtpHandler(session,
        inbound?casingSettings.getSmtpInboundTimeout():casingSettings.getSmtpOutboundTimeout(),
        inbound?casingSettings.getSmtpInboundTimeout():casingSettings.getSmtpOutboundTimeout(),
        m_phishImpl,
        spamConfig));

  }
}
