/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: FtpParser.java,v 1.24 2005/03/17 02:47:47 amread Exp $
 */

package com.metavize.tran.ftp;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.tran.token.ParseEvent;
import com.metavize.tran.token.ParseResult;
import com.metavize.tran.token.Parser;
import com.metavize.tran.token.TokenStreamer;
import org.apache.log4j.Logger;

public class FtpParser implements Parser
{
    private final FtpCasing casing;

    private final Logger logger = Logger.getLogger(FtpParser.class);
    private final Logger eventLogger = MvvmContextFactory.context().eventLogger();

    FtpParser(FtpCasing casing)
    {
        this.casing = casing;
    }

    public void newSession(ParseEvent pe)
    {
    }

    public TokenStreamer endSession()
    {
    }

    public ParseResult parse(ParseEvent pe) throws FtpParseException
    {
    }
}
