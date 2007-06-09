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

package com.untangle.node.spam;

import com.untangle.uvm.policy.Policy;
import com.untangle.uvm.tapi.TCPNewSessionRequest;
import com.untangle.uvm.tapi.TCPSession;
import com.untangle.node.mail.papi.MailExport;
import com.untangle.node.mail.papi.MailExportFactory;
import com.untangle.node.token.TokenHandler;
import com.untangle.node.token.TokenHandlerFactory;

public class SpamPopFactory implements TokenHandlerFactory
{
    private final SpamImpl node;
    private final MailExport zMExport;

    // constructors -----------------------------------------------------------

    SpamPopFactory(SpamImpl node)
    {
        this.node = node;
        Policy p = node.getTid().getPolicy();
        zMExport = MailExportFactory.factory().getExport();
    }

    // TokenHandlerFactory methods --------------------------------------------

    public TokenHandler tokenHandler(TCPSession session)
    {
        return new SpamPopHandler(session, node, zMExport);
    }

    public void handleNewSessionRequest(TCPNewSessionRequest tsr)
    {
    }
}
