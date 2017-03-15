/**
 * $Id$
 */
package com.untangle.node.live_support;

import com.untangle.uvm.node.AppBase;
import com.untangle.uvm.vnet.PipelineConnector;
import org.apache.log4j.Logger;


public class LiveSupportApp extends AppBase
{
    private final Logger logger = Logger.getLogger(getClass());

    private final PipelineConnector[] connectors = new PipelineConnector[] {};
    
    // constructor ------------------------------------------------------------
    public LiveSupportApp( com.untangle.uvm.node.AppSettings appSettings, com.untangle.uvm.node.AppProperties appProperties )
    {
        super( appSettings, appProperties );
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
