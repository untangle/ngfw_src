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

package com.untangle.tran.mail;

import com.untangle.mvvm.tapi.TCPSession;
import com.untangle.tran.token.Casing;
import com.untangle.tran.token.CasingFactory;

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
