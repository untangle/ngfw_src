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

package com.untangle.node.virus;


import com.untangle.uvm.tapi.TCPNewSessionRequest;
import com.untangle.uvm.tapi.TCPSession;
import com.untangle.node.mail.papi.MailExport;
import com.untangle.node.mail.papi.MailExportFactory;
import com.untangle.node.mail.papi.imap.ImapTokenStream;
import com.untangle.node.token.TokenHandler;
import com.untangle.node.token.TokenHandlerFactory;
import org.apache.log4j.Logger;

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

        boolean inbound = session.isInbound();

        VirusIMAPConfig virusConfig = (!inbound)?
            m_virusImpl.getVirusSettings().getIMAPInbound():
            m_virusImpl.getVirusSettings().getIMAPOutbound();

        if(!virusConfig.getScan()) {
            m_logger.debug("Scanning disabled.  Return passthrough token handler");
            return new ImapTokenStream(session);
        }

        long timeout = (!inbound)?m_mailExport.getExportSettings().getImapInboundTimeout():
            m_mailExport.getExportSettings().getImapOutboundTimeout();

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
