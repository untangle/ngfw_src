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

import com.metavize.tran.token.AbstractUntokenizer;
import com.metavize.tran.token.Token;
import com.metavize.tran.token.Untokenizeresult;
import org.apache.log4j.Logger;

class FtpUntokenizer extends AbstractUntokenizer
{
    private final Logger logger = Logger.getLogger(FtpUntokenizer.class);

    private final FtpCasing ftpCasing;

    FtpUntokenizer(FtpCasing ftpCasing)
    {
        this.ftpCasing = ftpCasing;
    }

    public Untokenizeresult untokenize(Token token)
    {
    }
}
