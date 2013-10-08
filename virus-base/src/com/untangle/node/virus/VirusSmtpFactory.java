/**
 * $Id$
 */
package com.untangle.node.virus;

import org.apache.log4j.Logger;

import com.untangle.node.smtp.MailExport;
import com.untangle.node.smtp.MailExportFactory;
import com.untangle.node.smtp.SmtpNodeSettings;
import com.untangle.node.token.TokenHandler;
import com.untangle.node.token.TokenHandlerFactory;
import com.untangle.uvm.vnet.NodeTCPSession;
import com.untangle.uvm.vnet.TCPNewSessionRequest;

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

        SmtpNodeSettings casingSettings = m_mailExport.getExportSettings();
        return new SmtpSessionHandler(session,
                casingSettings.getSmtpTimeout(),
                casingSettings.getSmtpTimeout(),
                m_virusImpl);
    }

    public void handleNewSessionRequest(TCPNewSessionRequest tsr)
    {
    }
}
