/**
 * $Id$
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
import com.untangle.uvm.vnet.TCPNewSessionRequest;
import com.untangle.uvm.vnet.NodeTCPSession;

public class SpamSmtpFactory implements TokenHandlerFactory
{
    private final Logger m_logger = Logger.getLogger(getClass());

    private MailExport m_mailExport;
    private QuarantineNodeView m_quarantine;
    private SafelistNodeView m_safelist;
    private SpamNodeImpl m_spamImpl;

    public SpamSmtpFactory(SpamNodeImpl impl) 
    {
        m_mailExport = MailExportFactory.factory().getExport();
        m_quarantine = m_mailExport.getQuarantineNodeView();
        m_safelist = m_mailExport.getSafelistNodeView();
        m_spamImpl = impl;
    }

    public TokenHandler tokenHandler(NodeTCPSession session) {
        SpamSettings spamSettings = m_spamImpl.getSettings();
        SpamSmtpConfig spamConfig = spamSettings.getSmtpConfig();

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
        SpamSettings spamSettings = m_spamImpl.getSettings();
        SpamSmtpConfig spamConfig = spamSettings.getSmtpConfig();

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
