/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: FtpUnparser.java,v 1.11 2005/03/17 02:47:47 amread Exp $
 */

package com.metavize.tran.ftp;

import com.metavize.tran.token.UnparseEvent;
import com.metavize.tran.token.UnparseResult;
import com.metavize.tran.token.Unparser;
import org.apache.log4j.Logger;

class FtpUnparser implements Unparser
{
    private final Logger logger = Logger.getLogger(FtpUnparser.class);

    private final FtpCasing ftpCasing;

    FtpUnparser(FtpCasing ftpCasing)
    {
        this.ftpCasing = ftpCasing;
    }

    public void newSession(UnparseEvent ue)
    {
    }

    public UnparseResult unparse(UnparseEvent ue)
    {
    }
}
