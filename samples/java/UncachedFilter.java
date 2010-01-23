package com.untangle.node.cpd;

import com.untangle.uvm.logging.RepositoryDesc;
import com.untangle.uvm.logging.UncachedEventFilter;
import com.untangle.uvm.node.NodeContext;
import com.untangle.uvm.util.I18nUtil;

/**
 * Use this to create a cache that never stores the results in memory,
 * but goes directly to the database.  the only time this is useful is
 * when there is something outside of the UVM that is doing the actual
 * logging.
 * To use this : 
 *      this.blockEventLogger = new UncachedEventManager<BlockEvent>();
        this.blockEventLogger.makeRepository(new BlockEventFilter(this));
        *
 */
class BlockEventFilter implements UncachedEventFilter<BlockEvent>
{
    private final NodeContext nodeContext;
    
    private static final RepositoryDesc REPO_DESC = new RepositoryDesc(I18nUtil.marktr("Captive Portal Block Events"));

    private static final String QUERY = "FROM com.untangle.node.cpd.BlockEvent evt ORDER BY evt.timeStamp DESC";
    
    public BlockEventFilter(NodeContext nodeContext) {
        this.nodeContext = nodeContext;
    }
    
    @Override
    public NodeContext getNodeContext() {
        return nodeContext;
    }

    @Override
    public String getQuery() {
        return QUERY; 
    }

    @Override
    public RepositoryDesc getRepositoryDesc() {
        return REPO_DESC;
    }

}
