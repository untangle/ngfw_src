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

package com.untangle.tran.mail.impl.imap;

import com.untangle.mvvm.tapi.TCPSession;
import com.untangle.tran.token.Casing;
import com.untangle.tran.token.CasingFactory;

public class ImapCasingFactory implements CasingFactory
{
    private static final ImapCasingFactory IMAP_CASING_FACTORY
        = new ImapCasingFactory();

    private ImapCasingFactory() { }

    public static ImapCasingFactory factory()
    {
        return IMAP_CASING_FACTORY;
    }

    public Casing casing(TCPSession session, boolean clientSide)
    {
        return new ImapCasing(session, clientSide);
    }
}
