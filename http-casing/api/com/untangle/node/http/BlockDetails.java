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

package com.untangle.node.http;

import java.io.Serializable;

/**
 * Holds information about why a page was blocked.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class BlockDetails implements Serializable
{
	private static final long serialVersionUID = 577462065375834496L;
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
