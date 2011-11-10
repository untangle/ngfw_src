package com.untangle.node.firewall;

import com.untangle.uvm.logging.RepositoryDesc;
import com.untangle.uvm.logging.SimpleEventFilter;
import com.untangle.uvm.util.I18nUtil;

public class FirewallBlockedFilter implements SimpleEventFilter<FirewallEvent>
{
    private static final RepositoryDesc REPO_DESC = new RepositoryDesc(I18nUtil.marktr("Blocked Events"));

    private static final String WARM_QUERY
        = "FROM SessionLogEventFromReports evt " +
           "WHERE evt.policyId = :policyId " +
           "AND firewallWasBlocked IS TRUE " +
           "ORDER BY evt.timeStamp DESC";

    // SimpleEventFilter methods ----------------------------------------------

    public RepositoryDesc getRepositoryDesc()
    {
        return REPO_DESC;
    }

    public String[] getQueries()
    {
        return new String[] { WARM_QUERY };
    }
}
