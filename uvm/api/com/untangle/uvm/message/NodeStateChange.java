/**
 * $Id: NodeStateChange.java,v 1.00 2012/03/27 17:26:54 dmorris Exp $
 */
package com.untangle.uvm.message;

import com.untangle.uvm.message.Message;
import com.untangle.uvm.node.NodeProperties;
import com.untangle.uvm.NodeSettings;

@SuppressWarnings("serial")
public class NodeStateChange extends Message
{
    private final NodeProperties nodeProperties;
    private final NodeSettings.NodeState nodeState;

    public NodeStateChange(NodeProperties nodeProperties, NodeSettings.NodeState nodeState)
    {
        this.nodeProperties = nodeProperties;
        this.nodeState = nodeState;
    }

    public NodeProperties getNodeProperties()
    {
        return nodeProperties;
    }

    public NodeSettings.NodeState getNodeState()
    {
        return nodeState;
    }
}