/**
 * $Id$
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

/**
 * The AppsView is an object that represents the current "view" of the apps tab
 *
 * This includes things like:
 * Apps currently installed (in this policy)
 * Apps that can be installed
 * Names, and license state.
 */
@SuppressWarnings("serial")
public class AppsView implements Serializable
{
    private int policyId;
    private List<NodeProperties> installable;
    private List<NodeSettings> instances;
    private List<NodeProperties> nodeProperties;
    private Map<Long, List<NodeMetric>> nodeMetrics;
    private Map<String, License> licenseMap;
    private Map<Long, NodeSettings.NodeState> runStates;

    public AppsView(int policyId, List<NodeProperties> installable, List<NodeSettings> instances, List<NodeProperties> nodeProperties, Map<Long, List<NodeMetric>> nodeMetrics, Map<String, License> license, Map<Long, NodeSettings.NodeState> runStates)
    {
        this.policyId = policyId;
        this.installable = installable;
        this.instances = instances;
        this.nodeProperties = nodeProperties;
        this.nodeMetrics = nodeMetrics;
        this.licenseMap = license;
        this.runStates = runStates;
    }

    public int getPolicyId() { return policyId; }
    public List<NodeProperties> getInstallable() { return installable; }
    public List<NodeSettings> getInstances() { return instances; }
    public List<NodeProperties> getNodeProperties() { return nodeProperties; }
    public Map<Long, List<NodeMetric>> getNodeMetrics() { return nodeMetrics; }
    public Map<String, License> getLicenseMap() { return licenseMap; }
    public Map<Long, NodeSettings.NodeState> getRunStates() { return runStates; }

    @Override
    public String toString()
    {
        return "AppsView\n  INSTALLABLE: " + installable + "\n  INSTANCES: " + instances + "\n  STAT DESCS: " + nodeMetrics;
    }

}
