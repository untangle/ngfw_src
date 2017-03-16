/**
 * $Id$
 */
package com.untangle.app.http;

import java.io.Serializable;

/**
 * Holds information about why a page was blocked.
 */
@SuppressWarnings("serial")
public class BlockDetails implements Serializable
{
    private static final int SUB_LINE_LEN = 80;
    private static final int MAX_LEN = 40;

    private final String host;
    private final String uri;

    public BlockDetails(String host, String uri)
    {
        this.host = host;
        this.uri = uri;
    }

    public String getHost()
    {
        return host;
    }

    public String getFormattedHost()
    {
        return null == host ? "" : breakLine(host, SUB_LINE_LEN);
    }

    public String getWhitelistHost()
    {
        if (null == host) {
            return null;
        } if (host.startsWith("www.") && 4 < host.length()) {
            return host.substring(4);
        } else {
            return host;
        }
    }

    public String getUri()
    {
        return uri;
    }

    public String getUrl()
    {
        if (null == host) {
            return "javascript:history.back()";
        } else {
            return "http://" + host + uri;
        }
    }

    public String getFormattedUrl()
    {
        if (null == host) {
            return "";
        } else {
            return shorten("http://" + host + uri, MAX_LEN);
        }
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

    private String shorten(String s, int maxLen)
    {
        return s.length() > maxLen ? s.substring(0, maxLen) : s;
    }
}
