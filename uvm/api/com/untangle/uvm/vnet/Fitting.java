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
    public static final Fitting HTTP_STREAM = new Fitting("http-stream");
    public static final Fitting HTTPS_STREAM = new Fitting("https-stream");
    public static final Fitting FTP_STREAM = new Fitting("ftp-stream");
    public static final Fitting FTP_CTL_STREAM = new Fitting("ftp-ctl-stream");
    public static final Fitting FTP_DATA_STREAM = new Fitting("ftp-data-stream");
    public static final Fitting SMTP_STREAM = new Fitting("smtp-stream");

    public static final Fitting TOKEN_STREAM = new Fitting("token-stream");
    public static final Fitting HTTP_TOKENS = new Fitting("http-tokens");
    public static final Fitting FTP_TOKENS = new Fitting("ftp-tokens");
    public static final Fitting FTP_CTL_TOKENS = new Fitting("ftp-ctl-tokens");
    public static final Fitting FTP_DATA_TOKENS = new Fitting("ftp-data-tokens");
    public static final Fitting SMTP_TOKENS = new Fitting("smtp-tokens");

    private String type;

    private Fitting(String type)
    {
        this.type = type;
    }

    @Override
    public String toString()
    {
        return "(fitting " + type + ")";
    }
}
