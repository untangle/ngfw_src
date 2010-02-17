/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.spam;

import org.apache.log4j.Logger;

import com.untangle.node.mail.papi.MailExport;
import com.untangle.node.mail.papi.MailExportFactory;
import com.untangle.node.mail.papi.MailNodeSettings;
import com.untangle.node.mail.papi.quarantine.QuarantineNodeView;
import com.untangle.node.mail.papi.safelist.SafelistNodeView;
import com.untangle.node.mail.papi.smtp.sapi.Session;
import com.untangle.node.token.TokenHandler;
import com.untangle.node.token.TokenHandlerFactory;
import com.untangle.uvm.policy.Policy;
import com.untangle.uvm.vnet.TCPNewSessionRequest;
import com.untangle.uvm.vnet.TCPSession;

public class SpamSmtpFactory implements TokenHandlerFactory
{
    private final Logger m_logger = Logger.getLogger(getClass());

    private MailExport m_mailExport;
    private QuarantineNodeView m_quarantine;
    private SafelistNodeView m_safelist;
    private SpamNodeImpl m_spamImpl;

    public SpamSmtpFactory(SpamNodeImpl impl) {
        Policy p = impl.getTid().getPolicy();
        m_mailExport = MailExportFactory.factory().getExport();
        m_quarantine = m_mailExport.getQuarantineNodeView();
        m_safelist = m_mailExport.getSafelistNodeView();
        m_spamImpl = impl;
    }

    public TokenHandler tokenHandler(TCPSession session) {
        SpamSettings spamSettings = m_spamImpl.getSpamSettings();
        SpamSmtpConfig spamConfig = spamSettings.getBaseSettings().getSmtpConfig();

        if(!spamConfig.getScan()) {
            m_logger.debug("Scanning disabled. Return passthrough token handler");
            return Session.createPassthruSession(session);
        }

        MailNodeSettings casingSettings = m_mailExport.getExportSettings();
        long timeout = casingSettings.getSmtpTimeout();
        boolean allowTLS = casingSettings.getSmtpAllowTLS();
        return new Session(session,
                           new SpamSmtpHandler(session, timeout, timeout, m_spamImpl,
                                               spamConfig, m_quarantine, m_safelist),
                           allowTLS);
    }

    public void handleNewSessionRequest(TCPNewSessionRequest tsr)
    {
        SpamSettings spamSettings = m_spamImpl.getSpamSettings();
        SpamSmtpConfig spamConfig = spamSettings.getBaseSettings().getSmtpConfig();

        // Note that we may *****NOT***** release the session here.  This is because
        // the mail casings currently assume that there will be at least one node
        // inline at all times.  The contained node's state machine handles some
        // of the casing's job currently. 10/06 jdi
        if(!spamConfig.getScan()) {
            return;
        }

        int activeCount = m_spamImpl.getScanner().getActiveScanCount();
        if (SpamLoadChecker.reject(activeCount, m_logger, spamConfig.getScanLimit(), spamConfig.getLoadLimit())) {
            m_logger.warn("Load too high, rejecting connection from: " + tsr.clientAddr());
            tsr.rejectReturnRst();
        }
    }
}
