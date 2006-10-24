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

package com.metavize.tran.mail.impl.smtp;

import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.tran.token.Casing;
import com.metavize.tran.token.CasingFactory;

public class SmtpCasingFactory implements CasingFactory
{
    private static final SmtpCasingFactory SMTP_CASING_FACTORY
        = new SmtpCasingFactory();

    private SmtpCasingFactory() { }

    public static SmtpCasingFactory factory()
    {
        return SMTP_CASING_FACTORY;
    }

    public Casing casing(TCPSession session, boolean clientSide)
    {
        return new SmtpCasing(session, clientSide);
    }
}
