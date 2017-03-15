/**
 * $Id$
 */
package com.untangle.uvm;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.untangle.uvm.node.AppProperties;
import com.untangle.uvm.node.License;
import com.untangle.uvm.node.AppSettings;
import com.untangle.uvm.node.AppMetric;

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
    private List<AppProperties> installable;
    private List<AppSettings> instances;
    private List<AppProperties> appProperties;
    private Map<Long, List<AppMetric>> nodeMetrics;
    private Map<String, License> licenseMap;
    private Map<Long, AppSettings.AppState> runStates;

    public AppsView(int policyId, List<AppProperties> installable, List<AppSettings> instances, List<AppProperties> appProperties, Map<Long, List<AppMetric>> nodeMetrics, Map<String, License> license, Map<Long, AppSettings.AppState> runStates)
    {
        this.policyId = policyId;
        this.installable = installable;
        this.instances = instances;
        this.appProperties = appProperties;
        this.nodeMetrics = nodeMetrics;
        this.licenseMap = license;
        this.runStates = runStates;
    }

    public int getPolicyId() { return policyId; }
    public List<AppProperties> getInstallable() { return installable; }
    public List<AppSettings> getInstances() { return instances; }
    public List<AppProperties> getAppProperties() { return appProperties; }
    public Map<Long, List<AppMetric>> getAppMetrics() { return nodeMetrics; }
    public Map<String, License> getLicenseMap() { return licenseMap; }
    public Map<Long, AppSettings.AppState> getRunStates() { return runStates; }

    @Override
    public String toString()
    {
        return "AppsView\n  INSTALLABLE: " + installable + "\n  INSTANCES: " + instances + "\n  STAT DESCS: " + nodeMetrics;
    }

}
