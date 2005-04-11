/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.ftp;

import com.metavize.tran.token.AbstractCasing;
import com.metavize.tran.token.Parser;
import com.metavize.tran.token.Unparser;
import org.apache.log4j.Logger;

class FtpCasing extends AbstractCasing
{
    private final FtpParser parser;
    private final FtpUnparser unparser;
    private final String insideStr;
    private final Logger logger = Logger.getLogger(FtpCasing.class);

    // constructors -----------------------------------------------------------

    FtpCasing(boolean inside)
    {
        insideStr = inside ? "inside " : "outside";
        parser = new FtpParser(this);
        unparser = new FtpUnparser(this);
    }

    // Casing methods ---------------------------------------------------------

    public Unparser unparser()
    {
        return unparser;
    }

    public Parser parser()
    {
        return parser;
    }
}
