/**
 * $Id$
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
public class WebFilterFlaggedFilter implements SimpleEventFilter<WebFilterEvent>
{
    private static final RepositoryDesc REPO_DESC = new RepositoryDesc(I18nUtil.marktr("Flagged Web Traffic"));

    private final String evtQuery;

    private final String vendorName;
    private final String capitalizedVendorName;

    WebFilterFlaggedFilter(WebFilterBase node)
    {
        this.vendorName = node.getVendor();
        this.capitalizedVendorName = vendorName.substring(0, 1).toUpperCase() + 
            vendorName.substring(1);

        evtQuery = "FROM HttpLogEventFromReports evt " + 
            "WHERE evt.wf" + capitalizedVendorName + "Flagged IS TRUE " + 
            "AND evt.policyId = :policyId ";
    }

    // SimpleEventFilter methods ----------------------------------------------
    public RepositoryDesc getRepositoryDesc()
    {
        return REPO_DESC;
    }

    public String[] getQueries()
    {
        return new String[] { evtQuery };
    }
}
