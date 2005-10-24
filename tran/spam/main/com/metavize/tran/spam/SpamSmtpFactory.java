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

import com.metavize.mvvm.policy.Policy;
import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.tran.mail.papi.MailExport;
import com.metavize.tran.mail.papi.MailExportFactory;
import com.metavize.tran.mail.papi.MailTransformSettings;
import com.metavize.tran.mail.papi.smtp.sapi.Session;
import com.metavize.tran.token.TokenHandler;
import com.metavize.tran.token.TokenHandlerFactory;
import com.metavize.tran.mail.papi.quarantine.QuarantineTransformView;
import com.metavize.tran.mail.papi.safelist.SafelistTransformView;
import org.apache.log4j.Logger;


public class SpamSmtpFactory
  implements TokenHandlerFactory {

  private MailExport m_mailExport;
  private QuarantineTransformView m_quarantine;
  private SafelistTransformView m_safelist;
  private SpamImpl m_spamImpl;
  private static final Logger m_logger =
    Logger.getLogger(SpamSmtpFactory.class);

  public SpamSmtpFactory(SpamImpl impl) {
    Policy p = impl.getTid().getPolicy();
    m_mailExport = MailExportFactory.factory().getExport();
    m_quarantine = m_mailExport.getQuarantineTransformView();
    m_safelist = m_mailExport.getSafelistTransformView();
    m_spamImpl = impl;
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

    MailTransformSettings casingSettings = m_mailExport.getExportSettings();
    return new Session(session,
      new SmtpSessionHandler(session,
        inbound?casingSettings.getSmtpInboundTimeout():casingSettings.getSmtpOutboundTimeout(),
        inbound?casingSettings.getSmtpInboundTimeout():casingSettings.getSmtpOutboundTimeout(),
        m_spamImpl,
        spamConfig,
        m_quarantine,
        m_safelist));

  }
}
