package com.untangle.node.spyware;

import com.untangle.uvm.logging.SessionLogEventFromReports;
import com.untangle.uvm.logging.RepositoryDesc;
import com.untangle.uvm.logging.SimpleEventFilter;
import com.untangle.uvm.util.I18nUtil;

public class SpywareAllFilter implements SimpleEventFilter<SessionLogEventFromReports>
{
    private static final RepositoryDesc REPO_DESC = new RepositoryDesc(I18nUtil.marktr("All Events (from reports tables)"));

    private static final String logQuery = "FROM SessionLogEventFromReports evt " +
        "WHERE evt.policyId = :policyId " +
        "AND sw_access_ident != ''" +
        "ORDER BY evt.timeStamp DESC";

    // SimpleEventFilter methods ----------------------------------------------

    public RepositoryDesc getRepositoryDesc()
    {
        return REPO_DESC;
    }

    public String[] getQueries()
    {
        return new String[] { logQuery };
    }
}
