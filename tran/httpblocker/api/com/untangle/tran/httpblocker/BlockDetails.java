/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.tran.httpblocker;

import java.io.Serializable;
import java.lang.StringBuffer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

public class BlockDetails implements Serializable
{
    private static final int SUB_LINE_LEN = 80;

    private final String nonce;
    private final HttpBlockerSettings settings;
    private final String host;
    private final URI uri;
    private final String reason;

    // constructor ------------------------------------------------------------

    public BlockDetails(String nonce, HttpBlockerSettings settings,
                        String host, URI uri, String reason)
    {
        this.nonce = nonce;
        this.settings = settings;
        this.host = host;
        this.uri = uri;
        this.reason = reason;
    }

    // public methods ---------------------------------------------------------

    public String getNonce()
    {
        return nonce;
    }

    public String getHost()
    {
        return null == host ? "" : host;
    }

    public String getHeader()
    {
        return settings.getBlockTemplate().getHeader();
    }

    public String getContact()
    {
        return settings.getBlockTemplate().getContact();
    }

    public String getFormattedHost()
    {
        return null == host ? "" : breakLine(host, SUB_LINE_LEN);
    }

    public URI getUri()
    {
        return uri;
    }

    public String getFormattedUri()
    {
        return breakLine(getUri().toString(), SUB_LINE_LEN);
    }

    public URL getUrl()
    {
        try {
            return uri.resolve("http://" + host).toURL();
        } catch (MalformedURLException exn) {
            throw new RuntimeException(exn);
        }
    }

    public String getFormattedUrl()
    {
        return breakLine(getUrl().toString(), SUB_LINE_LEN);
    }

    public String getReason()
    {
        return reason;
    }

    // private methods --------------------------------------------------------

    private String breakLine(String orgLine, int subLineLen) {
        StringBuffer newLine = new StringBuffer(orgLine.length());
        char chVal;
        int subLineCnt = 0;
        subLineLen--;
        for (int idx = 0; idx < orgLine.length(); idx++) {
            chVal = orgLine.charAt(idx);
            if ('\n' == chVal || ' ' == chVal) {
                newLine.append(chVal);
                subLineCnt = 1;
            } else if (subLineLen == subLineCnt) {
                newLine.append('\n');
                newLine.append(chVal);
                subLineCnt = 1;
            } else {
                newLine.append(chVal);
                subLineCnt++;
            }
        }
        return newLine.toString();
    }
}
