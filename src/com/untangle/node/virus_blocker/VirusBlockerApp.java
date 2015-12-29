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

    @Override
    protected int getHttpStrength()
    {
        // virus blocker is 18
        // virus blocker lite is 15
        // virus blocker should be higher (closer to server)
        return 18;
    }

    @Override
    protected int getFtpStrength()
    {
        // virus blocker is 18
        // virus blocker lite is 15
        // virus blocker should be higher (closer to server)
        return 18;
    }

    @Override
    protected int getSmtpStrength()
    {
        // virus blocker is 15
        // virus blocker lite is 18
        // virus blocker should be lower (closer to client)
        return 15; 
    }

    @Override
    public String getName()
    {
        return "virus_blocker";
    }

    @Override
    public String getAppName()
    {
        return "virus-blocker";
    }

    @Override
    public boolean isPremium()
    {
        return true;
    }
    
    @Override
    protected void preStart()
    {
        UvmContextFactory.context().daemonManager().incrementUsageCount("untangle-bdamserver");

        // we only need to enable the monitoring since it will be disabled
        // automatically when the daemon count reaches zero
        String transmit = "INFO 1\r\n";
        String search = "200 1";
        UvmContextFactory.context().daemonManager().enableRequestMonitoring("untangle-bdamserver", 1200, "127.0.0.1", 1344, transmit, search);
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
