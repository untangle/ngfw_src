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

package com.untangle.uvm.vnet;

/**
 * Describes the type of the stream between two nodes.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class Fitting
{
    public static final Fitting OCTET_STREAM = new Fitting("octet-stream");
    public static final Fitting HTTP_STREAM = new Fitting("http-stream", OCTET_STREAM);
    public static final Fitting FTP_STREAM = new Fitting("ftp-stream", OCTET_STREAM);
    public static final Fitting FTP_CTL_STREAM = new Fitting("ftp-ctl-stream", FTP_STREAM);
    public static final Fitting FTP_DATA_STREAM = new Fitting("ftp-data-stream", FTP_STREAM);
    public static final Fitting SMTP_STREAM = new Fitting("smtp-stream", OCTET_STREAM);
    public static final Fitting POP_STREAM = new Fitting("pop-stream", OCTET_STREAM);
    public static final Fitting IMAP_STREAM = new Fitting("imap-stream", OCTET_STREAM);

    public static final Fitting TOKEN_STREAM = new Fitting("token-stream");
    public static final Fitting HTTP_TOKENS = new Fitting("http-tokens", TOKEN_STREAM);
    public static final Fitting FTP_TOKENS = new Fitting("ftp-tokens", TOKEN_STREAM);
    public static final Fitting FTP_CTL_TOKENS = new Fitting("ftp-ctl-tokens", FTP_TOKENS);
    public static final Fitting FTP_DATA_TOKENS = new Fitting("ftp-data-tokens", FTP_TOKENS);
    public static final Fitting SMTP_TOKENS = new Fitting("smtp-tokens", TOKEN_STREAM);
    public static final Fitting POP_TOKENS = new Fitting("pop-tokens", TOKEN_STREAM);
    public static final Fitting IMAP_TOKENS = new Fitting("imap-tokens", TOKEN_STREAM);

    private String type;
    private Fitting parent;

    // constructors -----------------------------------------------------------

    private Fitting(String type, Fitting parent)
    {
        this.type = type;
        this.parent = parent;
    }

    private Fitting(String type)
    {
        this.type = type;
        this.parent = null;
    }

    // factories --------------------------------------------------------------

    // XXX define some factories for defining & retrieving fittings

    // public methods --------------------------------------------------------

    public Fitting getParent()
    {
        return parent;
    }

    public boolean instanceOf(Fitting o)
    {
        Fitting t = this;
        while (null != t) {
            if (t == o) {
                return true;
            }

            t = t.getParent();
        }

        return false;
    }

    // Object methods ---------------------------------------------------------

    @Override
    public String toString()
    {
        return "(fitting " + type + " " + parent + ")";
    }
}
