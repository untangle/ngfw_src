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
import com.untangle.node.token.TokenHandler;
import com.untangle.node.token.TokenHandlerFactory;

public class VirusPopFactory implements TokenHandlerFactory
{
    private final VirusNodeImpl node;
    private final MailExport zMExport;

    VirusPopFactory(VirusNodeImpl node)
    {
        this.node = node;
        zMExport = MailExportFactory.factory().getExport();
    }

    public TokenHandler tokenHandler(TCPSession session)
    {
        return new VirusPopHandler(session, node, zMExport);
    }

    public void handleNewSessionRequest(TCPNewSessionRequest tsr)
    {
    }
}
