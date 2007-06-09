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

import com.untangle.uvm.tapi.*;
import com.untangle.node.mail.*;
import com.untangle.node.mail.papi.*;
import com.untangle.node.mail.papi.smtp.*;
import com.untangle.node.mail.papi.smtp.sapi.Session;
import com.untangle.node.token.TokenHandler;
import com.untangle.node.token.TokenHandlerFactory;
import org.apache.log4j.Logger;

public class VirusSmtpFactory
    implements TokenHandlerFactory {

    private final Logger m_logger =
        Logger.getLogger(VirusSmtpFactory.class);

    private final VirusNodeImpl m_virusImpl;
    private final MailExport m_mailExport;

    VirusSmtpFactory(VirusNodeImpl node) {
        m_virusImpl = node;
        m_mailExport = MailExportFactory.factory().getExport();
    }

    public TokenHandler tokenHandler(TCPSession session) {

        boolean inbound = session.isInbound();

        VirusSMTPConfig virusConfig = inbound?
            m_virusImpl.getVirusSettings().getSMTPInbound():
            m_virusImpl.getVirusSettings().getSMTPOutbound();

        if(!virusConfig.getScan()) {
            m_logger.debug("Scanning disabled.  Return passthrough token handler");
            return Session.createPassthruSession(session);
        }

        MailNodeSettings casingSettings = m_mailExport.getExportSettings();
        return new Session(session,
                           new SmtpSessionHandler(
                                                  session,
                                                  inbound?casingSettings.getSmtpInboundTimeout():casingSettings.getSmtpOutboundTimeout(),
                                                  inbound?casingSettings.getSmtpInboundTimeout():casingSettings.getSmtpOutboundTimeout(),
                                                  m_virusImpl,
                                                  virusConfig));


    }

    public void handleNewSessionRequest(TCPNewSessionRequest tsr)
    {
    }
}
