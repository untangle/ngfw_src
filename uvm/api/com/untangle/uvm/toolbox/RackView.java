/**
 * $Id: RackView.java,v 1.00 2012/04/06 11:29:48 dmorris Exp $
 */
package com.untangle.uvm.toolbox;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.untangle.uvm.message.StatDescs;
import com.untangle.uvm.node.License;
import com.untangle.uvm.node.NodeDesc;
import com.untangle.uvm.NodeSettings;

@SuppressWarnings("serial")
public class RackView implements Serializable
{
    private List<Application> applications;
    private List<NodeDesc> instances;
    private Map<NodeSettings, StatDescs> statDescs;
    private Map<String, License> licenseMap;
    private Map<NodeSettings, NodeSettings.NodeState> runStates;

    public RackView(List<Application> applications,
                    List<NodeDesc> instances,
                    Map<NodeSettings, StatDescs> statDescs,
                    Map<String, License> license,
                    Map<NodeSettings, NodeSettings.NodeState> runStates)
    {
        this.applications = Collections.unmodifiableList(applications);
        this.instances = Collections.unmodifiableList(instances);
        this.statDescs = Collections.unmodifiableMap(statDescs);
        this.licenseMap = Collections.unmodifiableMap(license);
        this.runStates = Collections.unmodifiableMap(runStates);
    }

    public List<Application> getApplications()
    {
        return applications;
    }

    public List<NodeDesc> getInstances()
    {
        return instances;
    }

    public Map<NodeSettings, StatDescs> getStatDescs()
    {
        return statDescs;
    }

    public Map<String, License> getLicenseMap()
    {
        return licenseMap;
    }

    public Map<NodeSettings, NodeSettings.NodeState> getRunStates() {
        return runStates;
    }

    @Override
    public String toString()
    {
        return "RackView\n  AVAILABLE: " + applications + "\n  INSTANCES: " + instances + "\n  STAT DESCS: " + statDescs;
    }

}