/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: VirusPopFactory.java 194 2005-04-06 19:13:55Z rbscott $
 */
package com.metavize.tran.virus;

import com.metavize.mvvm.policy.Policy;
import com.metavize.tran.mail.papi.MailExport;
import com.metavize.tran.mail.papi.MailExportFactory;
import com.metavize.tran.token.TokenHandler;
import com.metavize.tran.token.TokenHandlerFactory;
import com.metavize.mvvm.tapi.TCPSession;

public class VirusPopFactory implements TokenHandlerFactory
{
    private final VirusTransformImpl transform;
    private final MailExport zMExport;

    VirusPopFactory(VirusTransformImpl transform)
    {
        this.transform = transform;
        Policy p = transform.getTid().getPolicy();
        zMExport = MailExportFactory.factory().getExport(p);
    }

    public TokenHandler tokenHandler(TCPSession session)
    {
        return new VirusPopHandler(session, transform, zMExport);
    }
}
