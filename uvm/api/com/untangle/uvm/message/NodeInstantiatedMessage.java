/**
 * $Id: NodeInstantiated.java,v 1.00 2012/04/01 18:13:16 dmorris Exp $
 */
package com.untangle.uvm.message;

import java.util.List;

import com.untangle.uvm.node.License;
import com.untangle.uvm.node.NodeProperties;
import com.untangle.uvm.node.NodeSettings;
import com.untangle.uvm.node.NodeMetric;
import com.untangle.uvm.message.Message;

@SuppressWarnings("serial")
public class NodeInstantiatedMessage extends Message
{
    private final NodeProperties nodeProperties;
    private final NodeSettings nodeSettings;
    private final List<NodeMetric> nodeMetrics;
    private final License license;
    private final Long policyId;
    
    public NodeInstantiatedMessage(NodeProperties nodeProperties, NodeSettings nodeSettings, List<NodeMetric> nodeMetrics, License license, Long policyId)
    {
        this.nodeProperties = nodeProperties;
        this.nodeSettings = nodeSettings;
        this.nodeMetrics = nodeMetrics;
        this.license = license;
        this.policyId = policyId;
    }

    public Long getPolicyId()
    {
        return this.policyId;
    }

    public NodeProperties getNodeProperties()
    {
        return this.nodeProperties;
    }

    public NodeSettings getNodeSettings()
    {
        return this.nodeSettings;
    }
    
    public List<NodeMetric> getNodeMetrics()
    {
        return this.nodeMetrics;
    }

    public License getLicense()
    {
        return this.license;
    }
}