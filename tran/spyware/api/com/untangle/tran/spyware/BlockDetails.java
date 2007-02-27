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

package com.untangle.tran.spyware;

import java.io.Serializable;
import java.lang.StringBuffer;
import java.net.InetAddress;

public class BlockDetails implements Serializable
{
    private static final int SUB_LINE_LEN = 80;

    private final String host;
    private final String uri;
    private final InetAddress clientAddr;

    public BlockDetails(String host, String uri, InetAddress clientAddr)
    {
        this.host = host;
        this.uri = uri;
        this.clientAddr = clientAddr;
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
            return breakLine("http://" + host + uri, SUB_LINE_LEN);
        }
    }

    public InetAddress getClientAddress()
    {
        return clientAddr;
    }

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
