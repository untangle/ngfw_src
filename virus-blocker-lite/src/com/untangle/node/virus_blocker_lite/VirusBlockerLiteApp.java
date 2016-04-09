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

    @Override
    protected int getHttpStrength()
    {
        // virus blocker is 18
        // virus blocker lite is 15
        // virus blocker should be higher (closer to server)
        return 15;
    }

    @Override
    protected int getFtpStrength()
    {
        // virus blocker is 18
        // virus blocker lite is 15
        // virus blocker should be higher (closer to server)
        return 15;
    }

    @Override
    protected int getSmtpStrength()
    {
        // virus blocker is 15
        // virus blocker lite is 18
        // virus blocker should be lower (closer to client)
        return 18; 
    }

    @Override
    public String getName()
    {
        return "virus_blocker_lite";
    }

    @Override
    public String getAppName()
    {
        return "virus-blocker-lite";
    }

    @Override
    public boolean isPremium()
    {
        return false;
    }
    
    @Override
    protected void preStart()
    {
        UvmContextFactory.context().daemonManager().incrementUsageCount( "clamav-daemon" );
        UvmContextFactory.context().daemonManager().incrementUsageCount( "clamav-freshclam" );
        UvmContextFactory.context().daemonManager().enableDaemonMonitoring("clamav-daemon", 300, "clamd");
        UvmContextFactory.context().daemonManager().enableDaemonMonitoring("clamav-freshclam", 3600, "freshclam");
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
