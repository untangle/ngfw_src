/**
 * $Id: WebFilterUnblockedFilter.java,v 1.00 2011/10/05 14:15:53 dmorris Exp $
 */
package com.untangle.node.webfilter;

import com.untangle.uvm.logging.RepositoryDesc;
import com.untangle.uvm.logging.SimpleEventFilter;
import com.untangle.uvm.util.I18nUtil;

/**
 * Selects Unblocked HTTP traffic.
 *
 * @author <a href="mailto:seb@untangle.com">Sebastien Delafond</a>
 * @version 1.0
 */
public class WebFilterUnblockedFilter
    implements SimpleEventFilter<UnblockEvent>
{
    private static final RepositoryDesc REPO_DESC = new RepositoryDesc(I18nUtil.marktr("Passlisted Web Events"));

    private final String evtQuery;

    private final String vendorName;
    private final String capitalizedVendorName;

    WebFilterUnblockedFilter(WebFilterBase node)
    {
        this.vendorName = node.getVendor();
        this.capitalizedVendorName = vendorName.substring(0, 1).toUpperCase() + 
            vendorName.substring(1);

        evtQuery = "FROM HttpLogEventFromReports evt " + 
            "WHERE evt.wf" + capitalizedVendorName + "Category = 'unblocked' " + 
            "AND evt.policyId = :policyId " + 
            "ORDER BY evt.timeStamp DESC" ;
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

    public boolean accept(UnblockEvent e)
    {
        return true;
    }
}
