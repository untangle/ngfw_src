/**
 * $Id$
 */
package com.untangle.app.live_support;

import com.untangle.uvm.app.AppBase;
import com.untangle.uvm.vnet.PipelineConnector;
import org.apache.log4j.Logger;

/**
 * Live Support application
 */
public class LiveSupportApp extends AppBase
{
    private final Logger logger = Logger.getLogger(getClass());

    private final PipelineConnector[] connectors = new PipelineConnector[] {};

    /**
     * Initialize Live support application
     *
     * @param appSettings
     *  Application settings.
     * @param appProperties
     *  Application properties
     */    
    public LiveSupportApp( com.untangle.uvm.app.AppSettings appSettings, com.untangle.uvm.app.AppProperties appProperties )
    {
        super( appSettings, appProperties );
    }

    /**
     * Initialization setting stub.
    */
    @Override
    public void initializeSettings()
    {
    }

    /**
     * Get the pineliene connector
     *
     * @return PipelineConector
     */
    @Override
    protected PipelineConnector[] getConnectors()
    {
        return this.connectors;
    }
}
