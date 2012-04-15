/**
 * $Id: RackView.java,v 1.00 2012/04/06 11:29:48 dmorris Exp $
 */
package com.untangle.uvm.toolbox;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.untangle.uvm.message.StatDescs;
import com.untangle.uvm.node.NodeProperties;
import com.untangle.uvm.node.License;
import com.untangle.uvm.node.NodeSettings;

@SuppressWarnings("serial")
public class RackView implements Serializable
{
    private List<Application> applications;
    private List<NodeSettings> instances;
    private List<NodeProperties> nodeProperties;
    private Map<Long, StatDescs> statDescs;
    private Map<String, License> licenseMap;
    private Map<Long, NodeSettings.NodeState> runStates;

    public RackView(List<Application> applications,
                    List<NodeSettings> instances,
                    List<NodeProperties> nodeProperties,
                    Map<Long, StatDescs> statDescs,
                    Map<String, License> license,
                    Map<Long, NodeSettings.NodeState> runStates)
    {
        this.applications = Collections.unmodifiableList(applications);
        this.instances = Collections.unmodifiableList(instances);
        this.nodeProperties = Collections.unmodifiableList(nodeProperties);
        this.statDescs = Collections.unmodifiableMap(statDescs);
        this.licenseMap = Collections.unmodifiableMap(license);
        this.runStates = Collections.unmodifiableMap(runStates);
    }

    public List<Application> getApplications()
    {
        return applications;
    }

    public List<NodeSettings> getInstances()
    {
        return instances;
    }

    public List<NodeProperties> getNodeProperties()
    {
        return nodeProperties;
    }
    
    public Map<Long, StatDescs> getStatDescs()
    {
        return statDescs;
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
        return "RackView\n  AVAILABLE: " + applications + "\n  INSTANCES: " + instances + "\n  STAT DESCS: " + statDescs;
    }

}