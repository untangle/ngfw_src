package com.untangle.node.cpd;

import com.untangle.uvm.logging.RepositoryDesc;
import com.untangle.uvm.logging.UncachedEventFilter;
import com.untangle.uvm.node.NodeContext;
import com.untangle.uvm.util.I18nUtil;

class BlockEventFilter implements UncachedEventFilter<BlockEvent>
{
    private final CPDImpl cpd;
    
    private static final RepositoryDesc REPO_DESC = new RepositoryDesc(I18nUtil.marktr("Captive Portal Block Events"));

    private static final String QUERY = "FROM com.untangle.node.cpd.BlockEvent evt ORDER BY evt.timeStamp DESC";
    
    public BlockEventFilter(CPDImpl cpd)
    {
        this.cpd = cpd;
    }
    
    @Override
    public NodeContext getNodeContext()
    {
        return this.cpd.getNodeContext();
    }

    @Override
    public String getQuery()
    {
        return QUERY; 
    }

    @Override
    public RepositoryDesc getRepositoryDesc()
    {
        return REPO_DESC;
    }

}
