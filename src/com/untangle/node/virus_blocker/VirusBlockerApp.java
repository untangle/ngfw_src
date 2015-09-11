/*
 * $Id: VirusBlockerApp.java 37269 2014-02-26 23:46:16Z dmorris $
 */

package com.untangle.node.virus_blocker;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.DaemonManager;
import com.untangle.uvm.node.License;
import com.untangle.node.virus_blocker.VirusBlockerBaseApp;

public class VirusBlockerApp extends VirusBlockerBaseApp
{
    public VirusBlockerApp(com.untangle.uvm.node.NodeSettings nodeSettings, com.untangle.uvm.node.NodeProperties nodeProperties)
    {
        super(nodeSettings, nodeProperties, new VirusBlockerScanner());
    }

    protected int getStrength()
    {
        return 18;
    }

    public String getName()
    {
        return "virus_blocker";
    }

    public String getAppName()
    {
        return "virus-blocker";
    }
    
    @Override
    protected void preStart()
    {
        UvmContextFactory.context().daemonManager().incrementUsageCount("untangle-bdamserver");

        // we only need to enable the monitoring since it will be disabled
        // automatically when the daemon count reaches zero
        String transmit = "INFO 1\r\n";
        String search = "200 1";
        UvmContextFactory.context().daemonManager().enableRequestMonitoring("untangle-bdamserver", 60, "127.0.0.1", 1344, transmit, search);
        super.preStart();
    }

    @Override
    protected void postStop()
    {
        UvmContextFactory.context().daemonManager().decrementUsageCount("untangle-bdamserver");
        super.postStop();
    }

    private boolean isLicenseValid()
    {
        if (UvmContextFactory.context().licenseManager().isLicenseValid(License.VIRUS_BLOCKER))
            return true;
        if (UvmContextFactory.context().licenseManager().isLicenseValid(License.VIRUS_BLOCKER_OLDNAME))
            return true;
        if (UvmContextFactory.context().licenseManager().isLicenseValid(License.COMMTOUCHAV))
            return true;
        return false;
    }
}
