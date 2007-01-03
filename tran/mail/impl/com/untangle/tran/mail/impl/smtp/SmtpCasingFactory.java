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

package com.untangle.tran.mail.impl.smtp;

import com.untangle.mvvm.tapi.TCPSession;
import com.untangle.tran.token.Casing;
import com.untangle.tran.token.CasingFactory;

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
