/**
 * $Id$
 */
package com.untangle.node.clam;

import com.untangle.node.virus.VirusNodeImpl;
import com.untangle.node.util.DaemonController;

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
        return "clam";
    }

    @Override
    protected void preStart()
    {
        DaemonController.getInstance().incrementUsageCount( "clamav-daemon" );
        DaemonController.getInstance().incrementUsageCount( "clamav-freshclam" );
        super.preStart();
    }

    @Override
    protected void postStop()
    {
        DaemonController.getInstance().decrementUsageCount( "clamav-daemon" );
        DaemonController.getInstance().decrementUsageCount( "clamav-freshclam" );
        super.postStop();
    }
    
}
