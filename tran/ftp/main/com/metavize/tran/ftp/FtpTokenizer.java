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
import com.metavize.tran.token.AbstractTokenizer;
import com.metavize.tran.token.TokenStreamer;
import org.apache.log4j.Logger;

public class FtpTokenizer extends AbstractTokenizer
{
    private final FtpCasing casing;

    private final Logger logger = Logger.getLogger(FtpTokenizer.class);
    private final Logger eventLogger = MvvmContextFactory.context().eventLogger();

    FtpTokenizer(FtpCasing casing)
    {
        this.casing = casing;
    }

    public TokenStreamer endSession()
    {
    }

    public TokenizerResult tokenizer(ByteBuffer buf) throws ParseException
    {
    }
}
