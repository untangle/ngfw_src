/**
 * $Id$
 */
package com.untangle.uvm.vnet;

/**
 * Describes the type of the stream between two nodes.
 */
public class Fitting
{
    public static final Fitting OCTET_STREAM = new Fitting("octet-stream");
    public static final Fitting HTTP_STREAM = new Fitting("http-stream", OCTET_STREAM);
    public static final Fitting FTP_STREAM = new Fitting("ftp-stream", OCTET_STREAM);
    public static final Fitting FTP_CTL_STREAM = new Fitting("ftp-ctl-stream", FTP_STREAM);
    public static final Fitting FTP_DATA_STREAM = new Fitting("ftp-data-stream", FTP_STREAM);
    public static final Fitting SMTP_STREAM = new Fitting("smtp-stream", OCTET_STREAM);

    public static final Fitting TOKEN_STREAM = new Fitting("token-stream");
    public static final Fitting HTTP_TOKENS = new Fitting("http-tokens", TOKEN_STREAM);
    public static final Fitting FTP_TOKENS = new Fitting("ftp-tokens", TOKEN_STREAM);
    public static final Fitting FTP_CTL_TOKENS = new Fitting("ftp-ctl-tokens", FTP_TOKENS);
    public static final Fitting FTP_DATA_TOKENS = new Fitting("ftp-data-tokens", FTP_TOKENS);
    public static final Fitting SMTP_TOKENS = new Fitting("smtp-tokens", TOKEN_STREAM);

    private String type;
    private Fitting parent;

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

    @Override
    public String toString()
    {
        return "(fitting " + type + " " + parent + ")";
    }
}
