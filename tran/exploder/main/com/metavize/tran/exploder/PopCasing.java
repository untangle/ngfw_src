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
import com.metavize.tran.mail.impl.pop.PopClientParser;
import com.metavize.tran.mail.impl.pop.PopServerParser;
import com.metavize.tran.mail.impl.pop.PopUnparser;
import com.metavize.tran.token.AbstractCasing;
import com.metavize.tran.token.Parser;
import com.metavize.tran.token.Unparser;

public class PopCasing extends AbstractCasing
{
    private final Parser parser;
    private final PopUnparser unparser;

    private String zUser;

    // constructors -----------------------------------------------------------

    PopCasing(TCPSession session, boolean clientSide)
    {
        parser = true == clientSide ? new PopClientParser(session, this) : new PopServerParser(session, this);
        unparser = new PopUnparser(session, clientSide, this);

        zUser = null;
    }

    // Casing methods ---------------------------------------------------------

    public Parser parser()
    {
        return parser;
    }

    public Unparser unparser()
    {
        return unparser;
    }

    public void setUser(String zUser)
    {
        this.zUser = zUser;
        return;
    }

    public String getUser()
    {
        return zUser;
    }
}
