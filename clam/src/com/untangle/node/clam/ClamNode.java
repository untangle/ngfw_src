/**
 * $Id$
 */
package com.untangle.node.clam;

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
        return "clam";
    }

    @Override
    protected void preStart()
    {
        ClamDaemonController.getInstance().incrementUsageCount();
        super.preStart();
    }

    @Override
    protected void postStop()
    {
        ClamDaemonController.getInstance().decrementUsageCount();
        super.postStop();
    }
    
}
