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

public class PopCasingFactory implements CasingFactory
{
    private static final PopCasingFactory POP_CASING_FACTORY
        = new PopCasingFactory();

    private PopCasingFactory() { }

    public static PopCasingFactory factory()
    {
        return POP_CASING_FACTORY;
    }

    public Casing casing(TCPSession session, boolean clientSide)
    {
        return new PopCasing(session, clientSide);
    }
}
