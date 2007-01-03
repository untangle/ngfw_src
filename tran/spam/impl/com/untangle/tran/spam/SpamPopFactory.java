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

package com.untangle.tran.spam;

import com.untangle.mvvm.policy.Policy;
import com.untangle.mvvm.tapi.TCPNewSessionRequest;
import com.untangle.mvvm.tapi.TCPSession;
import com.untangle.tran.mail.papi.MailExport;
import com.untangle.tran.mail.papi.MailExportFactory;
import com.untangle.tran.token.TokenHandler;
import com.untangle.tran.token.TokenHandlerFactory;

public class SpamPopFactory implements TokenHandlerFactory
{
    private final SpamImpl transform;
    private final MailExport zMExport;

    // constructors -----------------------------------------------------------

    SpamPopFactory(SpamImpl transform)
    {
        this.transform = transform;
        Policy p = transform.getTid().getPolicy();
        zMExport = MailExportFactory.factory().getExport();
    }

    // TokenHandlerFactory methods --------------------------------------------

    public TokenHandler tokenHandler(TCPSession session)
    {
        return new SpamPopHandler(session, transform, zMExport);
    }

    public void handleNewSessionRequest(TCPNewSessionRequest tsr)
    {
    }
}
