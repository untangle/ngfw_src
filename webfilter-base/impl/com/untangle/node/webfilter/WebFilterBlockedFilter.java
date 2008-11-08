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


import com.untangle.uvm.logging.RepositoryDesc;
import com.untangle.uvm.logging.SimpleEventFilter;
import com.untangle.uvm.util.I18nUtil;

/**
 * Selects Blocked HTTP traffic.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class WebFilterBlockedFilter
    implements SimpleEventFilter<WebFilterEvent>
{
    private static final RepositoryDesc REPO_DESC
        = new RepositoryDesc(I18nUtil.marktr("Blocked HTTP Traffic"));

    private final String warmQuery;

    WebFilterBlockedFilter(WebFilterBase node)
    {
        warmQuery = "FROM WebFilterEvent evt WHERE evt.vendorName = '"
            + node.getVendor()
            + "' AND evt.action = 'B' AND evt.requestLine.pipelineEndpoints.policy = :policy ORDER BY evt.timeStamp DESC";
    }

    // SimpleEventFilter methods ----------------------------------------------

    public RepositoryDesc getRepositoryDesc()
    {
        return REPO_DESC;
    }

    public String[] getQueries()
    {
        return new String[] { warmQuery };
    }

    public boolean accept(WebFilterEvent e)
    {
        return e.isPersistent() && Action.BLOCK == e.getAction();
    }
}
