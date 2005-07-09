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

class ImapCasingFactory implements CasingFactory
{
    private static final ImapCasingFactory IMAP_CASING_FACTORY
        = new ImapCasingFactory();

    private ImapCasingFactory() { }

    static ImapCasingFactory factory()
    {
        return IMAP_CASING_FACTORY;
    }

    public Casing casing(TCPSession session, boolean clientSide)
    {
        return new ImapCasing(session, clientSide);
    }
}
