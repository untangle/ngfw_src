package com.untangle.uvm.logging;

import com.untangle.uvm.node.NodeContext;

public interface UncachedEventFilter<E extends LogEvent>
{
    RepositoryDesc getRepositoryDesc();
    /**
     * The query to fetch all of the data.
     * @return
     */
    String getQuery();
    
    /**
     * Get the node context to run this transaction in, or null to run it in the default context.
     */
    public NodeContext getNodeContext();
}

