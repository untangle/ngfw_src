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
import com.metavize.tran.token.AbstractCasing;
import com.metavize.tran.token.Parser;
import com.metavize.tran.token.Unparser;

class SmtpCasing extends AbstractCasing
{
    private final Parser parser;
    private final SmtpUnparser unparser;

    // constructors -----------------------------------------------------------

    SmtpCasing(TCPSession session, boolean clientSide)
    {
        parser = clientSide ? new SmtpClientParser(session)
            : new SmtpServerParser(session);
        unparser = new SmtpUnparser(session, clientSide);
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
}
