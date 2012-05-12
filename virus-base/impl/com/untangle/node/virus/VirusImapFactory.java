/**
 * $Id$
 */
package com.untangle.node.virus;

import org.apache.log4j.Logger;

import com.untangle.node.mail.papi.MailExport;
import com.untangle.node.mail.papi.MailExportFactory;
import com.untangle.node.mail.papi.imap.ImapTokenStream;
import com.untangle.node.token.TokenHandler;
import com.untangle.node.token.TokenHandlerFactory;
import com.untangle.uvm.vnet.TCPNewSessionRequest;
import com.untangle.uvm.vnet.NodeTCPSession;

/**
 * Factory to create the protocol handler for IMAP
 * virus scanning
 */
public final class VirusImapFactory implements TokenHandlerFactory
{
    private final Logger m_logger = Logger.getLogger(getClass());

    private final VirusNodeImpl node;
    private final MailExport mailExport;


    public VirusImapFactory(VirusNodeImpl node)
    {
        this.node = node;
        this.mailExport = MailExportFactory.factory().getExport();
    }

    public TokenHandler tokenHandler(NodeTCPSession session)
    {
        if(!node.getSettings().getScanImap()) {
            m_logger.debug("Scanning disabled.  Return passthrough token handler");
            return new ImapTokenStream(session);
        }

        long timeout = mailExport.getExportSettings().getImapTimeout();

        return new ImapTokenStream(session,
                                   new VirusImapHandler(session,
                                                        timeout,
                                                        timeout,
                                                        node)
                                   );
    }

    public void handleNewSessionRequest(TCPNewSessionRequest tsr)
    {
    }
}
