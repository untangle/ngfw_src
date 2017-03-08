/**
 * $Id$
 */
package com.untangle.node.live_support;

import com.untangle.uvm.vnet.NodeBase;
import com.untangle.uvm.vnet.PipelineConnector;
import org.apache.log4j.Logger;


public class LiveSupportApp extends NodeBase
{
    private final Logger logger = Logger.getLogger(getClass());

    private final PipelineConnector[] connectors = new PipelineConnector[] {};
    
    // constructor ------------------------------------------------------------
    public LiveSupportApp( com.untangle.uvm.node.NodeSettings nodeSettings, com.untangle.uvm.node.NodeProperties nodeProperties )
    {
        super( nodeSettings, nodeProperties );
    }

    @Override
    public void initializeSettings()
    {
    }

    @Override
    protected PipelineConnector[] getConnectors()
    {
        return this.connectors;
    }
}
