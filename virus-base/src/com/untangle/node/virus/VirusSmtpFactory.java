/**
 * $Id$
 */
package com.untangle.node.virus;

import org.apache.log4j.Logger;

import com.untangle.node.smtp.MailExport;
import com.untangle.node.smtp.MailExportFactory;
import com.untangle.node.smtp.SmtpNodeSettings;
import com.untangle.node.smtp.sapi.Session;
import com.untangle.node.token.TokenHandler;
import com.untangle.node.token.TokenHandlerFactory;
import com.untangle.uvm.vnet.TCPNewSessionRequest;
import com.untangle.uvm.vnet.NodeTCPSession;

public class VirusSmtpFactory implements TokenHandlerFactory
{
    private final Logger m_logger = Logger.getLogger(VirusSmtpFactory.class);

    private final VirusNodeImpl m_virusImpl;
    private final MailExport m_mailExport;

    public VirusSmtpFactory(VirusNodeImpl node)
    {
        m_virusImpl = node;
        m_mailExport = MailExportFactory.factory().getExport();
    }

    public TokenHandler tokenHandler(NodeTCPSession session)
    {
        if(!m_virusImpl.getSettings().getScanSmtp()) {
            m_logger.debug("Scanning disabled.  Return passthrough token handler");
            return Session.createPassthruSession(session);
        }

        SmtpNodeSettings casingSettings = m_mailExport.getExportSettings();
        return new Session(session,
                           new SmtpSessionHandler(session,
                                                  casingSettings.getSmtpTimeout(),
                                                  casingSettings.getSmtpTimeout(),
                                                  m_virusImpl),
                           casingSettings.getSmtpAllowTLS());
    }

    public void handleNewSessionRequest(TCPNewSessionRequest tsr)
    {
    }
}
