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
    private static final RepositoryDesc REPO_DESC = new RepositoryDesc(I18nUtil.marktr("Blocked HTTP Traffic"));

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
