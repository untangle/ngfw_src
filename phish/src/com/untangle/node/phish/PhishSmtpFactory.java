/**
 * $Id$
 */
package com.untangle.node.phish;

import org.apache.log4j.Logger;

import com.untangle.node.smtp.MailExport;
import com.untangle.node.smtp.MailExportFactory;
import com.untangle.node.smtp.SmtpNodeSettings;
import com.untangle.node.smtp.quarantine.QuarantineNodeView;
import com.untangle.node.smtp.safelist.SafelistNodeView;
import com.untangle.node.spam.SpamLoadChecker;
import com.untangle.node.spam.SpamSmtpConfig;
import com.untangle.node.token.TokenHandler;
import com.untangle.node.token.TokenHandlerFactory;
import com.untangle.uvm.vnet.NodeTCPSession;
import com.untangle.uvm.vnet.TCPNewSessionRequest;

public class PhishSmtpFactory implements TokenHandlerFactory
{
    private MailExport m_mailExport;
    private PhishNode m_phishImpl;
    private QuarantineNodeView m_quarantine;
    private SafelistNodeView m_safelist;
    private final Logger m_logger = Logger.getLogger(getClass());

    public PhishSmtpFactory(PhishNode impl) {
        m_mailExport = MailExportFactory.factory().getExport();
        m_phishImpl = impl;
        m_quarantine = m_mailExport.getQuarantineNodeView();
        m_safelist = m_mailExport.getSafelistNodeView();
    }

    public TokenHandler tokenHandler(NodeTCPSession session)
    {
        SpamSmtpConfig spamConfig = m_phishImpl.getSettings().getSmtpConfig();

        SmtpNodeSettings casingSettings = m_mailExport.getExportSettings();
        
        return new PhishSmtpHandler(session, casingSettings.getSmtpTimeout(), casingSettings.getSmtpTimeout(),
                m_phishImpl, spamConfig, m_quarantine, m_safelist);
    }

    public void handleNewSessionRequest(TCPNewSessionRequest tsr)
    {
        SpamSmtpConfig spamConfig = m_phishImpl.getSettings().getSmtpConfig();

        // Note that we may *****NOT***** release the session here.
        // This is because the mail casings currently assume that
        // there will be at least one node inline at all times.  The
        // contained node's state machine handles some of the casing's
        // job currently. 10/06 jdi
        if(!spamConfig.getScan()) {
            return;
        }

        int activeCount = m_phishImpl.getScanner().getActiveScanCount();
        if (SpamLoadChecker.reject(activeCount, m_logger, spamConfig.getScanLimit(), spamConfig.getLoadLimit())) {
            m_logger.warn("Load too high, rejecting connection from " + tsr.getClientAddr());
            tsr.rejectReturnRst();
        }
    }
}
