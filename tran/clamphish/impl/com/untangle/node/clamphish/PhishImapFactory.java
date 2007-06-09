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

package com.untangle.node.clamphish;

import com.untangle.uvm.tapi.TCPNewSessionRequest;
import com.untangle.uvm.tapi.TCPSession;
import com.untangle.node.mail.papi.MailExport;
import com.untangle.node.mail.papi.MailExportFactory;
import com.untangle.node.mail.papi.imap.ImapTokenStream;
import com.untangle.node.mail.papi.safelist.SafelistNodeView;
import com.untangle.node.spam.SpamIMAPConfig;
import com.untangle.node.token.TokenHandler;
import com.untangle.node.token.TokenHandlerFactory;
import org.apache.log4j.Logger;

public class PhishImapFactory implements TokenHandlerFactory
{
    private final Logger m_logger = Logger.getLogger(getClass());

    private final MailExport m_mailExport;
    private final ClamPhishNode m_node;
    private SafelistNodeView m_safelist;

    PhishImapFactory(ClamPhishNode node) {
        m_node = node;
        /* XXX RBS I don't know if this will work */
        m_mailExport = MailExportFactory.factory().getExport();
        m_safelist = m_mailExport.getSafelistNodeView();
    }

    // TokenHandlerFactory methods --------------------------------------------

    public TokenHandler tokenHandler(TCPSession session) {

        boolean inbound = session.isInbound();

        SpamIMAPConfig config = (!inbound)?
            m_node.getSpamSettings().getIMAPInbound():
            m_node.getSpamSettings().getIMAPOutbound();

        if(!config.getScan()) {
            m_logger.debug("Scanning disabled.  Return passthrough token handler");
            return new ImapTokenStream(session);
        }

        long timeout = (!inbound)?m_mailExport.getExportSettings().getImapInboundTimeout():
            m_mailExport.getExportSettings().getImapOutboundTimeout();

        return new ImapTokenStream(session,
                                   new PhishImapHandler(
                                                        session,
                                                        timeout,
                                                        timeout,
                                                        m_node,
                                                        config,
                                                        m_safelist));
    }

    public void handleNewSessionRequest(TCPNewSessionRequest tsr)
    {
    }
}
