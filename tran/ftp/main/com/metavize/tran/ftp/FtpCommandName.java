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
 * Enumeration of FTP command names.
 *
 * XXX make 1.5 enum
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 */
public class FtpCommandName
{
    public static final FtpCommandName USER = new FtpCommandName("USER");
    public static final FtpCommandName PASS = new FtpCommandName("PASS");
    public static final FtpCommandName ACCT = new FtpCommandName("ACCT");
    public static final FtpCommandName CWD = new FtpCommandName("CWD");
    public static final FtpCommandName CDUP = new FtpCommandName("CDUP");
    public static final FtpCommandName SMNT = new FtpCommandName("SMNT");
    public static final FtpCommandName REIN = new FtpCommandName("REIN");
    public static final FtpCommandName QUIT = new FtpCommandName("QUIT");
    public static final FtpCommandName PORT = new FtpCommandName("PORT");
    public static final FtpCommandName PASV = new FtpCommandName("PASV");
    public static final FtpCommandName TYPE = new FtpCommandName("TYPE");
    public static final FtpCommandName STRU = new FtpCommandName("STRU");
    public static final FtpCommandName MODE = new FtpCommandName("MODE");
    public static final FtpCommandName RETR = new FtpCommandName("RETR");
    public static final FtpCommandName STOR = new FtpCommandName("STOR");
    public static final FtpCommandName STOU = new FtpCommandName("STOU");
    public static final FtpCommandName APPE = new FtpCommandName("APPE");
    public static final FtpCommandName ALLO = new FtpCommandName("ALLO");
    public static final FtpCommandName REST = new FtpCommandName("REST");
    public static final FtpCommandName RNFR = new FtpCommandName("RNFR");
    public static final FtpCommandName RNTO = new FtpCommandName("RNTO");
    public static final FtpCommandName ABOR = new FtpCommandName("ABOR");
    public static final FtpCommandName DELE = new FtpCommandName("DELE");
    public static final FtpCommandName RMD = new FtpCommandName("RMD");
    public static final FtpCommandName MKD = new FtpCommandName("MKD");
    public static final FtpCommandName PWD = new FtpCommandName("PWD");
    public static final FtpCommandName LIST = new FtpCommandName("LIST");
    public static final FtpCommandName NLST = new FtpCommandName("NLST");
    public static final FtpCommandName SITE = new FtpCommandName("SITE");
    public static final FtpCommandName SYST = new FtpCommandName("SYST");
    public static final FtpCommandName STAT = new FtpCommandName("STAT");
    public static final FtpCommandName HELP = new FtpCommandName("HELP");
    public static final FtpCommandName NOOP = new FtpCommandName("NOOP");

    private static final Map INSTANCES = new HashMap();

    static {
        INSTANCES.put(USER.toString(), USER);
        INSTANCES.put(PASS.toString(), PASS);
        INSTANCES.put(ACCT.toString(), ACCT);
        INSTANCES.put(CWD.toString(), CWD);
        INSTANCES.put(CDUP.toString(), CDUP);
        INSTANCES.put(SMNT.toString(), SMNT);
        INSTANCES.put(REIN.toString(), REIN);
        INSTANCES.put(QUIT.toString(), QUIT);
        INSTANCES.put(PORT.toString(), PORT);
        INSTANCES.put(PASV.toString(), PASV);
        INSTANCES.put(TYPE.toString(), TYPE);
        INSTANCES.put(STRU.toString(), STRU);
        INSTANCES.put(MODE.toString(), MODE);
        INSTANCES.put(RETR.toString(), RETR);
        INSTANCES.put(STOR.toString(), STOR);
        INSTANCES.put(STOU.toString(), STOU);
        INSTANCES.put(APPE.toString(), APPE);
        INSTANCES.put(ALLO.toString(), ALLO);
        INSTANCES.put(REST.toString(), REST);
        INSTANCES.put(RNFR.toString(), RNFR);
        INSTANCES.put(RNTO.toString(), RNTO);
        INSTANCES.put(ABOR.toString(), ABOR);
        INSTANCES.put(DELE.toString(), DELE);
        INSTANCES.put(RMD.toString(), RMD);
        INSTANCES.put(MKD.toString(), MKD);
        INSTANCES.put(PWD.toString(), PWD);
        INSTANCES.put(LIST.toString(), LIST);
        INSTANCES.put(NLST.toString(), NLST);
        INSTANCES.put(SITE.toString(), SITE);
        INSTANCES.put(SYST.toString(), SYST);
        INSTANCES.put(STAT.toString(), STAT);
        INSTANCES.put(HELP.toString(), HELP);
        INSTANCES.put(NOOP.toString(), NOOP);
    }

    private final String commandName;

    private FtpCommandName(String commandName)
    {
        this.commandName = commandName;
    }

    public static FtpCommandName getInstance(String commandName)
    {
        return (FtpCommandName)INSTANCES.get(commandName.toUpperCase());
    }

    // Object methods ---------------------------------------------------------

    public String toString() { return commandName; }

    // Serialization ----------------------------------------------------------

    Object readResolve()
    {
        return getInstance(key);
    }
}
