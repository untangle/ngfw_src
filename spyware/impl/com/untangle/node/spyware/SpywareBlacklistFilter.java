package com.untangle.node.spyware;

import com.untangle.uvm.logging.HttpLogEventFromReports;
import com.untangle.uvm.logging.RepositoryDesc;
import com.untangle.uvm.logging.SimpleEventFilter;
import com.untangle.uvm.util.I18nUtil;

public class SpywareBlacklistFilter implements SimpleEventFilter<HttpLogEventFromReports>
{
    private static final RepositoryDesc REPO_DESC = new RepositoryDesc(I18nUtil.marktr("Balcklisted Events (from reports tables)"));

    private static final String logQuery = "FROM HttpLogEventFromReports evt" + 
            " WHERE evt.swBlacklisted IS TRUE" + 
            " AND evt.policyId = :policyId" + 
            " ORDER BY evt.timeStamp DESC";

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
