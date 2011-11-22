/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.node.ftp;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enumeration of FTP functions.
 *
 * XXX make 1.5 enum
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
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

    private FtpFunction(String fnStr, boolean official)
    {
        this.fnStr = fnStr;

        if (official) {
            FUNCTIONS.put(fnStr, this);
        }
    }

    public static FtpFunction valueOf(String fnStr)
    {
        FtpFunction function = FUNCTIONS.get(fnStr);

        return null == function ? new FtpFunction(fnStr, false) : function;
    }

    // Object methods ---------------------------------------------------------

    public String toString()
    {
        return fnStr;
    }

    // initialization ---------------------------------------------------------

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
