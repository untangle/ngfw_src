/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.clamphish;

import com.metavize.mvvm.tapi.TCPNewSessionRequest;
import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.tran.mail.papi.MailExport;
import com.metavize.tran.mail.papi.MailExportFactory;
import com.metavize.tran.token.TokenHandler;
import com.metavize.tran.token.TokenHandlerFactory;

public class PhishPopFactory implements TokenHandlerFactory
{
    private final ClamPhishTransform transform;
    private final MailExport zMExport;

    // constructors -----------------------------------------------------------

    PhishPopFactory(ClamPhishTransform transform)
    {
        this.transform = transform;
        zMExport = MailExportFactory.factory().getExport();
    }

    // TokenHandlerFactory methods --------------------------------------------

    public TokenHandler tokenHandler(TCPSession session)
    {
        return new PhishPopHandler(session, transform, zMExport);
    }

    public void handleNewSessionRequest(TCPNewSessionRequest tsr)
    {
    }
}
