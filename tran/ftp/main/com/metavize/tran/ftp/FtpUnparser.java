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

import com.metavize.tran.token.AbstractUnparser;
import com.metavize.tran.token.UnparseResult;
import org.apache.log4j.Logger;
import com.metavize.tran.token.Token;

class FtpUnparser extends AbstractUnparser
{
    private final Logger logger = Logger.getLogger(FtpUnparser.class);

    private final FtpCasing ftpCasing;

    FtpUnparser(FtpCasing ftpCasing)
    {
        this.ftpCasing = ftpCasing;
    }

    public UnparseResult unparse(Token token)
    {
    }
}
