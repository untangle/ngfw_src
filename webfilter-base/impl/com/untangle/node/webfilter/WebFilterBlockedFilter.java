package com.untangle.node.webfilter;


import com.untangle.uvm.logging.RepositoryDesc;
import com.untangle.uvm.logging.SimpleEventFilter;
import com.untangle.uvm.util.I18nUtil;

/**
 * Selects Blocked HTTP traffic.
 */
public class WebFilterBlockedFilter
    implements SimpleEventFilter<WebFilterEvent>
{
    private static final RepositoryDesc REPO_DESC = new RepositoryDesc(I18nUtil.marktr("Blocked HTTP Traffic(from reports tables"));

    private final String evtQuery;

    private final String vendorName;
    private final String capitalizedVendorName;

    WebFilterBlockedFilter(WebFilterBase node)
    {
        this.vendorName = node.getVendor();
        this.capitalizedVendorName = vendorName.substring(0, 1).toUpperCase() + 
            vendorName.substring(1);

        evtQuery = "FROM HttpLogEventFromReports evt " + 
            "WHERE evt.wf" + capitalizedVendorName + "Category IS NOT NULL " + 
            "AND evt.wf" + capitalizedVendorName + "Action = 'B' " + 
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
