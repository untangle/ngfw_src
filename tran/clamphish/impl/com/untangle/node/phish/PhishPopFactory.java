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

package com.untangle.node.phish;

import com.untangle.uvm.tapi.TCPNewSessionRequest;
import com.untangle.uvm.tapi.TCPSession;
import com.untangle.node.mail.papi.MailExport;
import com.untangle.node.mail.papi.MailExportFactory;
import com.untangle.node.token.TokenHandler;
import com.untangle.node.token.TokenHandlerFactory;

public class PhishPopFactory implements TokenHandlerFactory
{
    private final PhishNode node;
    private final MailExport zMExport;

    // constructors -----------------------------------------------------------

    PhishPopFactory(PhishNode node)
    {
        this.node = node;
        zMExport = MailExportFactory.factory().getExport();
    }

    // TokenHandlerFactory methods --------------------------------------------

    public TokenHandler tokenHandler(TCPSession session)
    {
        return new PhishPopHandler(session, node, zMExport);
    }

    public void handleNewSessionRequest(TCPNewSessionRequest tsr)
    {
    }
}
