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

package com.metavize.tran.mail;

import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.tran.token.Casing;
import com.metavize.tran.token.CasingFactory;

class SmtpCasingFactory implements CasingFactory
{
    private static final SmtpCasingFactory SMTP_CASING_FACTORY
        = new SmtpCasingFactory();

    private SmtpCasingFactory() { }

    static SmtpCasingFactory factory()
    {
        return SMTP_CASING_FACTORY;
    }

    public Casing casing(TCPSession session, boolean clientSide)
    {
        return new SmtpCasing(session, clientSide);
    }
}

