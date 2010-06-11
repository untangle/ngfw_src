/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.webfilter;

import java.net.InetAddress;

import com.untangle.node.http.BlockDetails;

/**
 * BlockDetails for WebFilter.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@SuppressWarnings("serial")
public class WebFilterBlockDetails extends BlockDetails
{
    private final WebFilterSettings settings;
    private final String reason;
    private final InetAddress clientAddr;
    private final String nodeTitle;

    // constructor ------------------------------------------------------------

    public WebFilterBlockDetails(WebFilterSettings settings, String host,
                                 String uri, String reason,
                                 InetAddress clientAddr,
                                 String nodeTitle)
    {
        super(host, uri);
        this.settings = settings;
        this.reason = reason;
        this.clientAddr = clientAddr;
        this.nodeTitle = nodeTitle;
    }

    // public methods ---------------------------------------------------------

    public String getHeader()
    {
        return settings.getBaseSettings().getBlockTemplate().getHeader();
    }

    public String getReason()
    {
        return reason;
    }

    public String getNodeTitle()
    {
        return nodeTitle;
    }

    public InetAddress getClientAddress()
    {
        return clientAddr;
    }

    public WebFilterSettings getSettings()
    {
        return settings;
    }
}
