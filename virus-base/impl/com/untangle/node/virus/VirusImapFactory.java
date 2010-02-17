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

package com.untangle.node.virus;


import org.apache.log4j.Logger;

import com.untangle.node.mail.papi.MailExport;
import com.untangle.node.mail.papi.MailExportFactory;
import com.untangle.node.mail.papi.imap.ImapTokenStream;
import com.untangle.node.token.TokenHandler;
import com.untangle.node.token.TokenHandlerFactory;
import com.untangle.uvm.vnet.TCPNewSessionRequest;
import com.untangle.uvm.vnet.TCPSession;

/**
 * Factory to create the protocol handler for IMAP
 * virus scanning
 */
public final class VirusImapFactory
    implements TokenHandlerFactory {


    private final Logger m_logger = Logger.getLogger(getClass());

    private final VirusNodeImpl m_virusImpl;
    private final MailExport m_mailExport;


    VirusImapFactory(VirusNodeImpl node) {
        m_virusImpl = node;
        /* XXX RBS I don't know if this will work */
        m_mailExport = MailExportFactory.factory().getExport();
    }


    public TokenHandler tokenHandler(TCPSession session) {

        VirusIMAPConfig virusConfig = m_virusImpl.getVirusSettings().getBaseSettings().getImapConfig();

        if(!virusConfig.getScan()) {
            m_logger.debug("Scanning disabled.  Return passthrough token handler");
            return new ImapTokenStream(session);
        }

        long timeout = m_mailExport.getExportSettings().getImapTimeout();

        return new ImapTokenStream(session,
                                   new VirusImapHandler(session,
                                                        timeout,
                                                        timeout,
                                                        m_virusImpl,
                                                        virusConfig)
                                   );
    }

    public void handleNewSessionRequest(TCPNewSessionRequest tsr)
    {
    }
}
