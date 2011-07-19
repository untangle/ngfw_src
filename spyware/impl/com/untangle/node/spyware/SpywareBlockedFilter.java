/*
 * $Id$
 */
package com.untangle.node.spyware;

import com.untangle.uvm.logging.RepositoryDesc;
import com.untangle.uvm.logging.SimpleEventFilter;
import com.untangle.uvm.util.I18nUtil;

public class SpywareBlockedFilter implements SimpleEventFilter<SpywareEvent>
{
    private static final RepositoryDesc REPO_DESC = new RepositoryDesc(I18nUtil.marktr("Blocked Events"));

    private static final String ACCESS_QUERY
        = "FROM SpywareAccessEvent evt WHERE evt.blocked = true AND evt.pipelineEndpoints.policy = :policy ORDER BY evt.timeStamp DESC";
    private static final String BLACKLIST_QUERY
        = "FROM SpywareBlacklistEvent evt WHERE evt.requestLine.pipelineEndpoints.policy = :policy ORDER BY evt.timeStamp DESC";
    private static final String COOKIE_QUERY
        = "FROM SpywareCookieEvent evt WHERE evt.requestLine.pipelineEndpoints.policy = :policy ORDER BY evt.timeStamp DESC";

    // SimpleEventFilter methods ----------------------------------------------

    public RepositoryDesc getRepositoryDesc()
    {
        return REPO_DESC;
    }

    public String[] getQueries()
    {
        return new String[] { ACCESS_QUERY, BLACKLIST_QUERY, COOKIE_QUERY };
    }

    public boolean accept(SpywareEvent e)
    {
        return e.isBlocked();
    }
}
