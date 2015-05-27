/**
 * $Id$
 */
package com.untangle.node.clam;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.DaemonManager;
import com.untangle.node.virus.VirusNodeImpl;

public class ClamNode extends VirusNodeImpl
{
    public ClamNode( com.untangle.uvm.node.NodeSettings nodeSettings, com.untangle.uvm.node.NodeProperties nodeProperties )
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

    public String getOldName()
    {
        return "clam";
    }
    
    @Override
    protected void preStart()
    {
        UvmContextFactory.context().daemonManager().incrementUsageCount( "clamav-daemon" );
        UvmContextFactory.context().daemonManager().incrementUsageCount( "clamav-freshclam" );
        UvmContextFactory.context().daemonManager().enableRequestMonitoring("clamav-daemon", 300, "127.0.0.1", 3310, "TEST", "UNKNOWN COMMAND");
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
