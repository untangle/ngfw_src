package com.untangle.node.cpd;

import com.untangle.uvm.logging.RepositoryDesc;
import com.untangle.uvm.logging.SimpleEventFilter;
import com.untangle.uvm.node.NodeContext;
import com.untangle.uvm.util.I18nUtil;

class BlockEventFilter implements SimpleEventFilter<BlockEvent>
{
    private final CPDImpl cpd;
    
    private static final RepositoryDesc REPO_DESC = new RepositoryDesc(I18nUtil.marktr("Captive Portal Block Events"));

    private static final String QUERY = "FROM com.untangle.node.cpd.BlockEvent evt ORDER BY evt.timeStamp DESC";
    
    public BlockEventFilter(CPDImpl cpd)
    {
        this.cpd = cpd;
    }
    
    // FIXME: unused ?
    public NodeContext getNodeContext()
    {
        return this.cpd.getNodeContext();
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
