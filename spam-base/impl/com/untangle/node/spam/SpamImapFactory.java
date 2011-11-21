/**
 * $Id$
 */
package com.untangle.node.spam;

import org.apache.log4j.Logger;

import com.untangle.node.mail.papi.MailExport;
import com.untangle.node.mail.papi.MailExportFactory;
import com.untangle.node.mail.papi.imap.ImapTokenStream;
import com.untangle.node.mail.papi.safelist.SafelistNodeView;
import com.untangle.node.token.TokenHandler;
import com.untangle.node.token.TokenHandlerFactory;
import com.untangle.uvm.vnet.TCPNewSessionRequest;
import com.untangle.uvm.vnet.TCPSession;

public class SpamImapFactory implements TokenHandlerFactory
{
    private final Logger m_logger = Logger.getLogger(getClass());

    private final SpamNodeImpl m_impl;
    private final MailExport m_mailExport;
    private SafelistNodeView m_safelist;

    public SpamImapFactory(SpamNodeImpl impl)
    {
        m_impl = impl;
        m_mailExport = MailExportFactory.factory().getExport();
        m_safelist = m_mailExport.getSafelistNodeView();
    }

    public TokenHandler tokenHandler(TCPSession session)
    {

        SpamImapConfig config = m_impl.getSettings().getImapConfig();

        if(!config.getScan()) {
            m_logger.debug("Scanning disabled.  Return passthrough token handler");
            return new ImapTokenStream(session);
        }

        long timeout = m_mailExport.getExportSettings().getImapTimeout();

        return new ImapTokenStream(session, new SpamImapHandler( session, timeout, timeout, m_impl, config, m_safelist ) );
    }

    public void handleNewSessionRequest(TCPNewSessionRequest tsr){ }
}
