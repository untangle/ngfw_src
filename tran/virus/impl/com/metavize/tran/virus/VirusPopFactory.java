/*
 * Copyright (c) 2004, 2005, 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.tran.virus;

import com.metavize.mvvm.tapi.TCPNewSessionRequest;
import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.tran.mail.papi.MailExport;
import com.metavize.tran.mail.papi.MailExportFactory;
import com.metavize.tran.token.TokenHandler;
import com.metavize.tran.token.TokenHandlerFactory;

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
