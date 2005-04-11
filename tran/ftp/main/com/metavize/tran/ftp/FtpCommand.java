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

package com.metavize.tran.ftp;

/**
 * Describe class <code>FtpCommand</code> here.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 */
public class FtpCommand implements Token
{
    private final FtpCommandName command;
    private final String argument;

    public FtpCommand(FtpCommandName command, String argument)
    {
        this.command = command;
        this.argument = argument;
    }


}
