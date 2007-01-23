/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.tran.clamphish;

import com.untangle.mvvm.tapi.TCPNewSessionRequest;
import com.untangle.mvvm.tapi.TCPSession;
import com.untangle.tran.mail.papi.MailExport;
import com.untangle.tran.mail.papi.MailExportFactory;
import com.untangle.tran.mail.papi.MailTransformSettings;
import com.untangle.tran.mail.papi.quarantine.QuarantineTransformView;
import com.untangle.tran.mail.papi.safelist.SafelistTransformView;
import com.untangle.tran.mail.papi.smtp.ScanLoadChecker;
import com.untangle.tran.mail.papi.smtp.sapi.Session;
import com.untangle.tran.spam.SpamSMTPConfig;
import com.untangle.tran.token.TokenHandler;
import com.untangle.tran.token.TokenHandlerFactory;
import org.apache.log4j.Logger;

public class PhishSmtpFactory
  implements TokenHandlerFactory {

  private MailExport m_mailExport;
  private ClamPhishTransform m_phishImpl;
  private QuarantineTransformView m_quarantine;
  private SafelistTransformView m_safelist;
  private final Logger m_logger = Logger.getLogger(getClass());

  public PhishSmtpFactory(ClamPhishTransform impl) {
    m_mailExport = MailExportFactory.factory().getExport();
    m_phishImpl = impl;
    m_quarantine = m_mailExport.getQuarantineTransformView();
    m_safelist = m_mailExport.getSafelistTransformView();
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
        spamConfig,
        m_quarantine,
        m_safelist));
  }

    public void handleNewSessionRequest(TCPNewSessionRequest tsr)
    {
        boolean inbound = tsr.isInbound();

        SpamSMTPConfig spamConfig = inbound?
            m_phishImpl.getSpamSettings().getSMTPInbound():
            m_phishImpl.getSpamSettings().getSMTPOutbound();

        // Note that we may *****NOT***** release the session here.  This is because
        // the mail casings currently assume that there will be at least one transform
        // inline at all times.  The contained transform's state machine handles some
        // of the casing's job currently. 10/06 jdi
        if(!spamConfig.getScan()) {
            return;
        }

        int activeCount = m_phishImpl.getScanner().getActiveScanCount();
        if (ScanLoadChecker.reject(activeCount, m_logger)) {
            m_logger.warn("Load too high, rejecting connection from " + tsr.clientAddr());
            tsr.rejectReturnRst();
        }
    }
}
