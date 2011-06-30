/*
 * $HeadURL: svn://chef/work/src/webfilter-base/impl/com/untangle/node/webfilter/UnblockEventAllFilter.java $
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
public class UnblockEventAllFilter
    implements SimpleEventFilter<UnblockEvent>
{
    private static final RepositoryDesc REPO_DESC = new RepositoryDesc(I18nUtil.marktr("All unblock events"));

    private final String warmQuery;

    UnblockEventAllFilter(WebFilterBase node)
    {
        warmQuery = "FROM UnblockEvent evt WHERE evt.vendorName = '"
            + node.getVendor()
            + "' AND evt.policy = :policy ORDER BY evt.timeStamp DESC";
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

    public boolean accept(UnblockEvent e)
    {
        return true;
    }
}
