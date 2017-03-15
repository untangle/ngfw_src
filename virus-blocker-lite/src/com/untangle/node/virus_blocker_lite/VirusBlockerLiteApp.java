/**
 * $Id$
 */
package com.untangle.node.virus_blocker_lite;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.DaemonManager;
import com.untangle.uvm.node.AppSettings;
import com.untangle.uvm.node.AppProperties;
import com.untangle.node.virus_blocker.VirusBlockerBaseApp;

public class VirusBlockerLiteApp extends VirusBlockerBaseApp
{
    public VirusBlockerLiteApp( AppSettings appSettings, AppProperties appProperties )
    {
        super( appSettings, appProperties );
        this.setScanner( new ClamScanner(this) );
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
    protected void preStart( boolean isPermanentTransition )
    {
        UvmContextFactory.context().daemonManager().incrementUsageCount( "clamav-daemon" );
        UvmContextFactory.context().daemonManager().incrementUsageCount( "clamav-freshclam" );
        UvmContextFactory.context().daemonManager().enableDaemonMonitoring("clamav-daemon", 300, "clamd");
        UvmContextFactory.context().daemonManager().enableDaemonMonitoring("clamav-freshclam", 3600, "freshclam");
        super.preStart( isPermanentTransition );
    }

    @Override
    protected void postStop( boolean isPermanentTransition )
    {
        UvmContextFactory.context().daemonManager().decrementUsageCount( "clamav-daemon" );
        UvmContextFactory.context().daemonManager().decrementUsageCount( "clamav-freshclam" );
        super.postStop( isPermanentTransition );
    }
    
}
