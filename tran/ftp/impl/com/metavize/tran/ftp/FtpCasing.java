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

package com.metavize.tran.ftp;

import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.tran.token.AbstractCasing;
import com.metavize.tran.token.Parser;
import com.metavize.tran.token.Unparser;

class FtpCasing extends AbstractCasing
{
    private final Parser parser;
    private final FtpUnparser unparser;

    // constructors -----------------------------------------------------------

    FtpCasing(TCPSession session, boolean clientSide)
    {
        parser = clientSide ? new FtpClientParser(session)
            : new FtpServerParser(session);
        unparser = new FtpUnparser(session, clientSide);
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
