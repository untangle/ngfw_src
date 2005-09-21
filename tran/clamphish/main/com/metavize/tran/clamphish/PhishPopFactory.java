/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.clamphish;

import com.metavize.mvvm.policy.Policy;
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
        Policy p = transform.getTid().getPolicy();
        zMExport = MailExportFactory.factory().getExport(p);
    }

    // TokenHandlerFactory methods --------------------------------------------

    public TokenHandler tokenHandler(TCPSession session)
    {
        return new PhishPopHandler(session, transform, zMExport);
    }
}
