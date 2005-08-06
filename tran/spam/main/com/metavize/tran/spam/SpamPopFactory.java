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

package com.metavize.tran.spam;

import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.tran.mail.papi.MailExport;
import com.metavize.tran.mail.papi.MailExportFactory;
import com.metavize.tran.token.TokenHandler;
import com.metavize.tran.token.TokenHandlerFactory;

public class SpamPopFactory implements TokenHandlerFactory
{
    private final SpamImpl transform;
    private final MailExport zMExport;

    // constructors -----------------------------------------------------------

    SpamPopFactory(SpamImpl transform)
    {
        this.transform = transform;
        zMExport = MailExportFactory.getExport();
    }

    // TokenHandlerFactory methods --------------------------------------------

    public TokenHandler tokenHandler(TCPSession session)
    {
        return new SpamPopHandler(session, transform, zMExport);
    }
}
