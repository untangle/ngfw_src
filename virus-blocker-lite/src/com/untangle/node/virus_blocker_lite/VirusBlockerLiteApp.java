/**
 * $Id$
 */
package com.untangle.node.virus_blocker_lite;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.DaemonManager;
import com.untangle.node.virus_blocker.VirusBlockerBaseApp;

public class VirusBlockerLiteApp extends VirusBlockerBaseApp
{
    public VirusBlockerLiteApp( com.untangle.uvm.node.NodeSettings nodeSettings, com.untangle.uvm.node.NodeProperties nodeProperties )
    {
        super( nodeSettings, nodeProperties, new ClamScanner() );
    }

    protected int getStrength()
    {
        return 15;
    }

    public String getName()
    {
        return "virus_blocker_lite";
    }

    public String getAppName()
    {
        return "virus-blocker-lite";
    }
    
    @Override
    protected void preStart()
    {
        UvmContextFactory.context().daemonManager().incrementUsageCount( "clamav-daemon" );
        UvmContextFactory.context().daemonManager().incrementUsageCount( "clamav-freshclam" );
        UvmContextFactory.context().daemonManager().enableDaemonMonitoring("clamav-daemon", 300, "/usr/sbin/clamd");
        super.preStart();
    }

    @Override
    protected void postStop()
    {
        UvmContextFactory.context().daemonManager().decrementUsageCount( "clamav-daemon" );
        UvmContextFactory.context().daemonManager().decrementUsageCount( "clamav-freshclam" );
        super.postStop();
    }
    
}
