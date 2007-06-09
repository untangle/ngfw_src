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

package com.untangle.node.ftp;

import com.untangle.uvm.tapi.TCPSession;
import com.untangle.node.token.AbstractCasing;
import com.untangle.node.token.Parser;
import com.untangle.node.token.Unparser;

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
