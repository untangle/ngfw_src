package com.untangle.node.openvpn;

import com.untangle.uvm.logging.RepositoryDesc;
import com.untangle.uvm.logging.SimpleEventFilter;
import com.untangle.uvm.util.I18nUtil;

public class ClientConnectEventAllFilter
    implements SimpleEventFilter<ClientConnectEvent> {

    public static final String REPOSITORY_NAME = I18nUtil.marktr("Closed Sessions (from reports tables)");

    private static final RepositoryDesc REPO_DESC = new RepositoryDesc(REPOSITORY_NAME);

    private static final String WARM_QUERY
        = "FROM OpenvpnLogEventFromReports evt ORDER BY evt.timeStamp DESC";

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
