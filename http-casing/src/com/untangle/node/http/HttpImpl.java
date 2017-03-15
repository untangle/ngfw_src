/*
 * $Id$
 */
package com.untangle.node.http;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.vnet.SessionEventHandler;
import com.untangle.uvm.vnet.ForkedEventHandler;
import com.untangle.uvm.vnet.NodeBase;
import com.untangle.uvm.vnet.PipelineConnector;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.SettingsManager;
import org.apache.log4j.Logger;
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;

/**
 * An HTTP casing node.
 */
public class HttpImpl extends NodeBase
{
    private SessionEventHandler clientSideHandler = new ForkedEventHandler( new HttpParserEventHandler(true,this), new HttpUnparserEventHandler(true,this) );
    private SessionEventHandler serverSideHandler = new ForkedEventHandler( new HttpUnparserEventHandler(false,this), new HttpParserEventHandler(false,this) );
    
    private final PipelineConnector clientSideConnector = UvmContextFactory.context().pipelineFoundry().create( "http-client-side", this, null, clientSideHandler, Fitting.HTTP_STREAM, Fitting.HTTP_TOKENS, Affinity.CLIENT, -1000, false, null );
    private final PipelineConnector serverSideConnector = UvmContextFactory.context().pipelineFoundry().create( "http-server-side", this, null, serverSideHandler, Fitting.HTTP_TOKENS, Fitting.HTTP_STREAM, Affinity.SERVER, 1000, false, "http-client-side" );
    private final PipelineConnector[] connectors = new PipelineConnector[] { clientSideConnector, serverSideConnector };

    private final Logger logger = Logger.getLogger(HttpImpl.class);

    private final SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
    private HttpSettings settings;

    public HttpImpl( com.untangle.uvm.node.AppSettings appSettings, com.untangle.uvm.node.AppProperties appProperties )
    {
        super( appSettings, appProperties );
    }

    public HttpSettings getHttpSettings()
    {
        return settings;
    }

    public void setHttpSettings(final HttpSettings newSettings)
    {
        String nodeID = this.getAppSettings().getId().toString();
        String settingsFile = System.getProperty("uvm.settings.dir") + "/http/settings_" + nodeID + ".js";

        try {
            settingsManager.save( settingsFile, newSettings );
        } catch(Exception exn) {
            logger.error("setHttpSettings()",exn);
            return;
        }

        this.settings = newSettings;
        
        reconfigure();
    }

    private void reconfigure()
    {
        if ( settings != null ) {
            for ( PipelineConnector connector : this.connectors ) 
                connector.setEnabled( settings.isEnabled() );
        }
    }

    protected void postInit()
    {
        String nodeID = this.getAppSettings().getId().toString();
        String settingsFile = System.getProperty("uvm.settings.dir") + "/http/settings_" + nodeID + ".js";

        HttpSettings readSettings = null;
        logger.info("Loading settings from " + settingsFile );

        try {
            // first we try to read our json settings
            readSettings = settingsManager.load( HttpSettings.class, settingsFile );
        } catch (Exception exn) {
            logger.error("postInit()",exn);
        }

        try {
            // still no settings found so init with defaults
            if (readSettings == null)
            {
                logger.warn("No settings found... initializing with defaults");
                setHttpSettings(new HttpSettings());
            }

            // otherwise apply the loaded or imported settings from the file
            else
            {
                logger.info("Loaded settings from " + settingsFile);
                this.settings = readSettings;
                reconfigure();
            }
        } catch (Exception exn) {
            logger.error("Could not apply node settings",exn);
        }

    }

    @Override
    protected PipelineConnector[] getConnectors()
    {
        return this.connectors;
    }

    public Object getSettings()
    {
        return getHttpSettings();
    }

    public void setSettings(Object settings)
    {
        setHttpSettings((HttpSettings)settings);
    }
}
