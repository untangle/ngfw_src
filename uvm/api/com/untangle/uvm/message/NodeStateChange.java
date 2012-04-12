/**
 * $Id: NodeStateChange.java,v 1.00 2012/03/27 17:26:54 dmorris Exp $
 */
package com.untangle.uvm.message;

import com.untangle.uvm.message.Message;
import com.untangle.uvm.node.NodeDesc;
import com.untangle.uvm.NodeSettings;

@SuppressWarnings("serial")
public class NodeStateChange extends Message
{
    private final NodeDesc nodeDesc;
    private final NodeSettings.NodeState nodeState;

    public NodeStateChange(NodeDesc nodeDesc, NodeSettings.NodeState nodeState)
    {
        this.nodeDesc = nodeDesc;
        this.nodeState = nodeState;
    }

    public NodeDesc getNodeDesc()
    {
        return nodeDesc;
    }

    public NodeSettings.NodeState getNodeState()
    {
        return nodeState;
    }
}