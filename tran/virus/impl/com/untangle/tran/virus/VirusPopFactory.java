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
import com.untangle.tran.token.TokenHandler;
import com.untangle.tran.token.TokenHandlerFactory;

public class VirusPopFactory implements TokenHandlerFactory
{
    private final VirusTransformImpl transform;
    private final MailExport zMExport;

    VirusPopFactory(VirusTransformImpl transform)
    {
        this.transform = transform;
        zMExport = MailExportFactory.factory().getExport();
    }

    public TokenHandler tokenHandler(TCPSession session)
    {
        return new VirusPopHandler(session, transform, zMExport);
    }

    public void handleNewSessionRequest(TCPNewSessionRequest tsr)
    {
    }
}
