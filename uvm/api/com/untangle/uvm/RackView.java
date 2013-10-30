/**
 * $Id: RackView.java,v 1.00 2012/04/06 11:29:48 dmorris Exp $
 */
package com.untangle.uvm;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.untangle.uvm.node.NodeProperties;
import com.untangle.uvm.node.License;
import com.untangle.uvm.node.NodeSettings;
import com.untangle.uvm.node.NodeMetric;

@SuppressWarnings("serial")
public class RackView implements Serializable
{
    private List<NodeProperties> installable;
    private List<NodeSettings> instances;
    private List<NodeProperties> nodeProperties;
    private Map<Long, List<NodeMetric>> nodeMetrics;
    private Map<String, License> licenseMap;
    private Map<Long, NodeSettings.NodeState> runStates;

    public RackView(List<NodeProperties> installable, List<NodeSettings> instances, List<NodeProperties> nodeProperties, Map<Long, List<NodeMetric>> nodeMetrics, Map<String, License> license, Map<Long, NodeSettings.NodeState> runStates)
    {
        this.installable = installable;
        this.instances = instances;
        this.nodeProperties = nodeProperties;
        this.nodeMetrics = nodeMetrics;
        this.licenseMap = license;
        this.runStates = runStates;
    }

    public List<NodeProperties> getInstallable()
    {
        return installable;
    }

    public List<NodeSettings> getInstances()
    {
        return instances;
    }

    public List<NodeProperties> getNodeProperties()
    {
        return nodeProperties;
    }
    
    public Map<Long, List<NodeMetric>> getNodeMetrics()
    {
        return nodeMetrics;
    }

    public Map<String, License> getLicenseMap()
    {
        return licenseMap;
    }

    public Map<Long, NodeSettings.NodeState> getRunStates() {
        return runStates;
    }

    @Override
    public String toString()
    {
        return "RackView\n  INSTALLABLE: " + installable + "\n  INSTANCES: " + instances + "\n  STAT DESCS: " + nodeMetrics;
    }

}