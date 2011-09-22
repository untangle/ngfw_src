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
import com.untangle.node.mail.papi.imap.ImapTokenStream;
import com.untangle.node.mail.papi.safelist.SafelistNodeView;
import com.untangle.node.token.TokenHandler;
import com.untangle.node.token.TokenHandlerFactory;
import com.untangle.uvm.vnet.TCPNewSessionRequest;
import com.untangle.uvm.vnet.TCPSession;

public class SpamImapFactory
    implements TokenHandlerFactory {

    private final Logger m_logger = Logger.getLogger(getClass());

    private final SpamNodeImpl m_impl;
    private final MailExport m_mailExport;
    private SafelistNodeView m_safelist;

    SpamImapFactory(SpamNodeImpl impl) {
        m_impl = impl;
        /* XXX RBS I don't know if this will work */
        m_mailExport = MailExportFactory.factory().getExport();
        m_safelist = m_mailExport.getSafelistNodeView();
    }


    public TokenHandler tokenHandler(TCPSession session) {

        SpamImapConfig config = m_impl.getSettings().getImapConfig();

        if(!config.getScan()) {
            m_logger.debug("Scanning disabled.  Return passthrough token handler");
            return new ImapTokenStream(session);
        }

        long timeout = m_mailExport.getExportSettings().getImapTimeout();

        return new ImapTokenStream(session,
                                   new SpamImapHandler(
                                                       session,
                                                       timeout,
                                                       timeout,
                                                       m_impl,
                                                       config,
                                                       m_safelist));
    }

    public void handleNewSessionRequest(TCPNewSessionRequest tsr)
    {
    }
}
