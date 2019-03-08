/**
 * $Id$
 */
package com.untangle.app.ftp;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enumeration of FTP functions.
 * @see "RFC 959, Section 4"
 */
public class FtpFunction
{
    public static final FtpFunction USER;
    public static final FtpFunction PASS;
    public static final FtpFunction ACCT;
    public static final FtpFunction CWD;
    public static final FtpFunction CDUP;
    public static final FtpFunction SMNT;
    public static final FtpFunction REIN;
    public static final FtpFunction QUIT;
    public static final FtpFunction PORT;
    public static final FtpFunction PASV;
    public static final FtpFunction TYPE;
    public static final FtpFunction STRU;
    public static final FtpFunction MODE;
    public static final FtpFunction RETR;
    public static final FtpFunction STOR;
    public static final FtpFunction STOU;
    public static final FtpFunction APPE;
    public static final FtpFunction ALLO;
    public static final FtpFunction REST;
    public static final FtpFunction RNFR;
    public static final FtpFunction RNTO;
    public static final FtpFunction ABOR;
    public static final FtpFunction DELE;
    public static final FtpFunction RMD;
    public static final FtpFunction MKD;
    public static final FtpFunction PWD;
    public static final FtpFunction LIST;
    public static final FtpFunction NLST;
    public static final FtpFunction SITE;
    public static final FtpFunction SYST;
    public static final FtpFunction STAT;
    public static final FtpFunction HELP;
    public static final FtpFunction NOOP;

    // RFC 2389
    public static final FtpFunction FEAT;
    public static final FtpFunction OPTS;

    // RFC 2428
    public static final FtpFunction EPRT;
    public static final FtpFunction EPSV;

    private static final Map<String, FtpFunction> FUNCTIONS;

    private final String fnStr;

    /**
     * FtpFunction creates a new FtpFunction
     * use valueOf to get create FtpFunction
     * @param fnStr
     * @param official
     */
    private FtpFunction(String fnStr, boolean official)
    {
        this.fnStr = fnStr;

        if (official) {
            FUNCTIONS.put(fnStr, this);
        }
    }

    /**
     * valueOf to get the FtpFunction for a specified function string
     * @param fnStr
     * @return the FtpFunction
     */
    public static FtpFunction valueOf(String fnStr)
    {
        FtpFunction function = FUNCTIONS.get(fnStr);

        return null == function ? new FtpFunction(fnStr, false) : function;
    }

    /**
     * toString returns the string equivalent of the command
     * @return the command string
     */
    public String toString()
    {
        return fnStr;
    }

    static {
        FUNCTIONS = new ConcurrentHashMap<String, FtpFunction>();

        USER = new FtpFunction("USER", true);
        PASS = new FtpFunction("PASS", true);
        ACCT = new FtpFunction("ACCT", true);
        CWD = new FtpFunction("CWD", true);
        CDUP = new FtpFunction("CDUP", true);
        SMNT = new FtpFunction("SMNT", true);
        REIN = new FtpFunction("REIN", true);
        QUIT = new FtpFunction("QUIT", true);
        PORT = new FtpFunction("PORT", true);
        PASV = new FtpFunction("PASV", true);
        TYPE = new FtpFunction("TYPE", true);
        STRU = new FtpFunction("STRU", true);
        MODE = new FtpFunction("MODE", true);
        RETR = new FtpFunction("RETR", true);
        STOR = new FtpFunction("STOR", true);
        STOU = new FtpFunction("STOU", true);
        APPE = new FtpFunction("APPE", true);
        ALLO = new FtpFunction("ALLO", true);
        REST = new FtpFunction("REST", true);
        RNFR = new FtpFunction("RNFR", true);
        RNTO = new FtpFunction("RNTO", true);
        ABOR = new FtpFunction("ABOR", true);
        DELE = new FtpFunction("DELE", true);
        RMD = new FtpFunction("RMD", true);
        MKD = new FtpFunction("MKD", true);
        PWD = new FtpFunction("PWD", true);
        LIST = new FtpFunction("LIST", true);
        NLST = new FtpFunction("NLST", true);
        SITE = new FtpFunction("SITE", true);
        SYST = new FtpFunction("SYST", true);
        STAT = new FtpFunction("STAT", true);
        HELP = new FtpFunction("HELP", true);
        NOOP = new FtpFunction("NOOP", true);
        FEAT = new FtpFunction("FEAT", true);
        OPTS = new FtpFunction("OPTS", true);
        EPRT = new FtpFunction("EPRT", true);
        EPSV = new FtpFunction("EPSV", true);
    }
}
