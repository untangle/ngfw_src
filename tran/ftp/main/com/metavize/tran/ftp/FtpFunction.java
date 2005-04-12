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

import java.util.Map;

/**
 * Enumeration of FTP functions.
 *
 * XXX make 1.5 enum
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 * @see "RFC 959, Section 4"
 */
public class FtpFunction
{
    public static final FtpFunction USER = new FtpFunction("USER");
    public static final FtpFunction PASS = new FtpFunction("PASS");
    public static final FtpFunction ACCT = new FtpFunction("ACCT");
    public static final FtpFunction CWD = new FtpFunction("CWD");
    public static final FtpFunction CDUP = new FtpFunction("CDUP");
    public static final FtpFunction SMNT = new FtpFunction("SMNT");
    public static final FtpFunction REIN = new FtpFunction("REIN");
    public static final FtpFunction QUIT = new FtpFunction("QUIT");
    public static final FtpFunction PORT = new FtpFunction("PORT");
    public static final FtpFunction PASV = new FtpFunction("PASV");
    public static final FtpFunction TYPE = new FtpFunction("TYPE");
    public static final FtpFunction STRU = new FtpFunction("STRU");
    public static final FtpFunction MODE = new FtpFunction("MODE");
    public static final FtpFunction RETR = new FtpFunction("RETR");
    public static final FtpFunction STOR = new FtpFunction("STOR");
    public static final FtpFunction STOU = new FtpFunction("STOU");
    public static final FtpFunction APPE = new FtpFunction("APPE");
    public static final FtpFunction ALLO = new FtpFunction("ALLO");
    public static final FtpFunction REST = new FtpFunction("REST");
    public static final FtpFunction RNFR = new FtpFunction("RNFR");
    public static final FtpFunction RNTO = new FtpFunction("RNTO");
    public static final FtpFunction ABOR = new FtpFunction("ABOR");
    public static final FtpFunction DELE = new FtpFunction("DELE");
    public static final FtpFunction RMD = new FtpFunction("RMD");
    public static final FtpFunction MKD = new FtpFunction("MKD");
    public static final FtpFunction PWD = new FtpFunction("PWD");
    public static final FtpFunction LIST = new FtpFunction("LIST");
    public static final FtpFunction NLST = new FtpFunction("NLST");
    public static final FtpFunction SITE = new FtpFunction("SITE");
    public static final FtpFunction SYST = new FtpFunction("SYST");
    public static final FtpFunction STAT = new FtpFunction("STAT");
    public static final FtpFunction HELP = new FtpFunction("HELP");
    public static final FtpFunction NOOP = new FtpFunction("NOOP");

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

    private FtpFunction(String commandName)
    {
        this.commandName = commandName;
    }

    public static FtpFunction getInstance(String commandName)
    {
        return (FtpFunction)INSTANCES.get(commandName.toUpperCase());
    }

    // Object methods ---------------------------------------------------------

    public String toString() { return commandName; }

    // Serialization ----------------------------------------------------------

    Object readResolve()
    {
        return getInstance(key);
    }
}
