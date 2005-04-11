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

import java.nio.ByteBuffer;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.tran.token.AbstractParser;
import com.metavize.tran.token.ParseException;
import com.metavize.tran.token.ParseResult;
import com.metavize.tran.token.TokenStreamer;
import org.apache.log4j.Logger;

public class FtpParser extends AbstractParser
{
    private final FtpCasing casing;

    private final Logger logger = Logger.getLogger(FtpParser.class);
    private final Logger eventLogger = MvvmContextFactory.context().eventLogger();

    FtpParser(FtpCasing casing)
    {
        this.casing = casing;
    }

    public TokenStreamer endSession()
    {
    }

    public ParseResult parse(ByteBuffer buf) throws ParseException
    {
    }
}
