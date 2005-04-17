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
 * Enumeration of FTP functions.
 *
 * XXX make 1.5 enum
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 * @see "RFC 959, Section 4"
 */
public enum FtpFunction
{
    USER,
    PASS,
    ACCT,
    CWD,
    CDUP,
    SMNT,
    REIN,
    QUIT,
    PORT,
    PASV,
    TYPE,
    STRU,
    MODE,
    RETR,
    STOR,
    STOU,
    APPE,
    ALLO,
    REST,
    RNFR,
    RNTO,
    ABOR,
    DELE,
    RMD,
    MKD,
    PWD,
    LIST,
    NLST,
    SITE,
    SYST,
    STAT,
    HELP,
    NOOP;
}
