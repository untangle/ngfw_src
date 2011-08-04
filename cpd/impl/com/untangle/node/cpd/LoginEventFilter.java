package com.untangle.node.cpd;

import com.untangle.uvm.logging.RepositoryDesc;
import com.untangle.uvm.logging.SimpleEventFilter;
import com.untangle.uvm.util.I18nUtil;

class LoginEventFilter implements SimpleEventFilter<CPDLoginEvent>
{
    private static final RepositoryDesc REPO_DESC = new RepositoryDesc(I18nUtil.marktr("Captive Portal Login Events (from reports tables)"));

    private static final String WARM_QUERY = "FROM CpdLoginEventsFromReports evt ORDER BY evt.timeStamp DESC";

    // SimpleEventFilter methods ----------------------------------------------

    public RepositoryDesc getRepositoryDesc()
    {
        return REPO_DESC;
    }

    public String[] getQueries()
    {
        return new String[] { WARM_QUERY };
    }

    public boolean accept(CPDLoginEvent e)
    {
        return true;
    }
}
