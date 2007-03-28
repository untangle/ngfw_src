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

package com.untangle.tran.spam;

import com.untangle.mvvm.policy.Policy;
import com.untangle.mvvm.tapi.TCPNewSessionRequest;
import com.untangle.mvvm.tapi.TCPSession;
import com.untangle.tran.mail.papi.MailExport;
import com.untangle.tran.mail.papi.MailExportFactory;
import com.untangle.tran.mail.papi.MailTransformSettings;
import com.untangle.tran.mail.papi.quarantine.QuarantineTransformView;
import com.untangle.tran.mail.papi.safelist.SafelistTransformView;
import com.untangle.tran.mail.papi.smtp.ScanLoadChecker;
import com.untangle.tran.mail.papi.smtp.sapi.Session;
import com.untangle.tran.token.TokenHandler;
import com.untangle.tran.token.TokenHandlerFactory;
import org.apache.log4j.Logger;

public class SpamSmtpFactory
  implements TokenHandlerFactory {

  private final Logger m_logger = Logger.getLogger(getClass());

  private MailExport m_mailExport;
  private QuarantineTransformView m_quarantine;
  private SafelistTransformView m_safelist;
  private SpamImpl m_spamImpl;

  public SpamSmtpFactory(SpamImpl impl) {
    Policy p = impl.getTid().getPolicy();
    m_mailExport = MailExportFactory.factory().getExport();
    m_quarantine = m_mailExport.getQuarantineTransformView();
    m_safelist = m_mailExport.getSafelistTransformView();
    m_spamImpl = impl;
  }

    public TokenHandler tokenHandler(TCPSession session) {
        boolean inbound = session.isInbound();

        SpamSettings spamSettings = m_spamImpl.getSpamSettings();
        SpamSMTPConfig spamConfig = inbound ?
            spamSettings.getSMTPInbound() : spamSettings.getSMTPOutbound();

        if(!spamConfig.getScan()) {
            m_logger.debug("Scanning disabled. Return passthrough token handler");
            return Session.createPassthruSession(session);
        }

        MailTransformSettings casingSettings = m_mailExport.getExportSettings();
        long timeout = inbound ?
            casingSettings.getSmtpInboundTimeout() :
            casingSettings.getSmtpOutboundTimeout();
        return new Session(session,
          new SmtpSessionHandler(session, timeout, timeout, m_spamImpl,
                                 spamConfig, m_quarantine, m_safelist));
    }

    public void handleNewSessionRequest(TCPNewSessionRequest tsr)
    {
        boolean inbound = tsr.isInbound();

        SpamSettings spamSettings = m_spamImpl.getSpamSettings();
        SpamSMTPConfig spamConfig = inbound ?
            spamSettings.getSMTPInbound() : spamSettings.getSMTPOutbound();

        // Note that we may *****NOT***** release the session here.  This is because
        // the mail casings currently assume that there will be at least one transform
        // inline at all times.  The contained transform's state machine handles some
        // of the casing's job currently. 10/06 jdi
        if(!spamConfig.getScan()) {
            return;
        }

        int activeCount = m_spamImpl.getScanner().getActiveScanCount();
        if (ScanLoadChecker.reject(activeCount, m_logger)) {
            m_logger.warn("Load too high, rejecting connection from: " + tsr.clientAddr());
            tsr.rejectReturnRst();
        }
    }
}
