package com.untangle.node.cpd;

import com.untangle.uvm.logging.RepositoryDesc;
import com.untangle.uvm.logging.SimpleEventFilter;
import com.untangle.uvm.util.I18nUtil;

class BlockEventFilter implements SimpleEventFilter<BlockEvent>
{
    private final CPDImpl cpd;
    
    private static final RepositoryDesc REPO_DESC = new RepositoryDesc(I18nUtil.marktr("Block Events"));

    private static final String QUERY = "FROM CpdBlockEventsFromReports evt ORDER BY evt.timeStamp DESC";
    
    public BlockEventFilter(CPDImpl cpd)
    {
        this.cpd = cpd;
    }

    public String[] getQueries()
    {
        return new String[] { QUERY };
    }

    public boolean accept(BlockEvent e)
    {
        return true;
    }

    public RepositoryDesc getRepositoryDesc()
    {
        return REPO_DESC;
    }

}
