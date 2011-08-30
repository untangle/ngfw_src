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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.Session;

import com.untangle.node.http.RequestLine;
import com.untangle.uvm.logging.SimpleEventFilter;
import com.untangle.uvm.logging.ListEventFilter;
import com.untangle.uvm.logging.RepositoryDesc;
import com.untangle.uvm.util.I18nUtil;

/**
 * Filter for passed HTTP traffic.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class WebFilterPassedFilter implements SimpleEventFilter<WebFilterEvent>
{
    private final String evtQuery;

    private static final RepositoryDesc REPO_DESC
        = new RepositoryDesc(I18nUtil.marktr("Passed HTTP Traffic (from reports tables)"));

    private final String vendorName;
    private final String capitalizedVendorName;

    public WebFilterPassedFilter(WebFilterBase node)
    {
        this.vendorName = node.getVendor();
        this.capitalizedVendorName = vendorName.substring(0, 1).toUpperCase() + 
            vendorName.substring(1);

        evtQuery = "FROM HttpLogEventFromReports evt " + 
            "WHERE evt.wf" + capitalizedVendorName + "Category IS NOT NULL " + 
            "AND evt.wf" + capitalizedVendorName + "Action = 'I' " + 
            "AND evt.policyId = :policyId ";
    }

    public RepositoryDesc getRepositoryDesc()
    {
        return REPO_DESC;
    }

    public boolean accept(WebFilterEvent e)
    {
        return !e.getBlocked();
    }

    public String[] getQueries()
    {
        return new String[] { evtQuery }; 
    }
}
