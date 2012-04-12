/**
 * $Id: NodeInstantiated.java,v 1.00 2012/04/01 18:13:16 dmorris Exp $
 */
package com.untangle.uvm.node;

import com.untangle.uvm.node.License;
import com.untangle.uvm.message.Message;
import com.untangle.uvm.message.StatDescs;

@SuppressWarnings("serial")
public class NodeInstantiated extends Message
{
    private final NodeDesc nodeDesc;
    private final StatDescs statDescs;
    private final License license;

    public NodeInstantiated(NodeDesc nodeDesc, StatDescs statDescs, License license)
    {
        this.nodeDesc = nodeDesc;
        this.statDescs = statDescs;
        this.license = license;
    }

    public Long getPolicyId()
    {
        return nodeDesc.getNodeSettings().getPolicyId();
    }

    public NodeDesc getNodeDesc()
    {
        return nodeDesc;
    }

    public StatDescs getStatDescs()
    {
        return statDescs;
    }

    public License getLicense()
    {
        return license;
    }
}