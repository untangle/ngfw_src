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

package com.untangle.tran.virus;


import com.untangle.mvvm.tapi.TCPNewSessionRequest;
import com.untangle.mvvm.tapi.TCPSession;
import com.untangle.tran.mail.papi.MailExport;
import com.untangle.tran.mail.papi.MailExportFactory;
import com.untangle.tran.mail.papi.imap.ImapTokenStream;
import com.untangle.tran.token.TokenHandler;
import com.untangle.tran.token.TokenHandlerFactory;
import org.apache.log4j.Logger;

/**
 * Factory to create the protocol handler for IMAP
 * virus scanning
 */
public final class VirusImapFactory
    implements TokenHandlerFactory {


    private final Logger m_logger = Logger.getLogger(getClass());

    private final VirusTransformImpl m_virusImpl;
    private final MailExport m_mailExport;


    VirusImapFactory(VirusTransformImpl transform) {
        m_virusImpl = transform;
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
