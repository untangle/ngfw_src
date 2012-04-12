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
    private final NodeProperties nodeProperties;
    private final StatDescs statDescs;
    private final License license;

    public NodeInstantiated(NodeProperties nodeProperties, StatDescs statDescs, License license)
    {
        this.nodeProperties = nodeProperties;
        this.statDescs = statDescs;
        this.license = license;
    }

    public Long getPolicyId()
    {
        return nodeProperties.getNodeSettings().getPolicyId();
    }

    public NodeProperties getNodeProperties()
    {
        return nodeProperties;
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