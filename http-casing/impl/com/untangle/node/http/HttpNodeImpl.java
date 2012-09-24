/*
 * $Id$
 */
package com.untangle.node.http;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.vnet.NodeBase;
import com.untangle.uvm.vnet.CasingPipeSpec;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipeSpec;
import com.untangle.uvm.SettingsManager;
import org.apache.log4j.Logger;
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;

/**
 * An HTTP casing node.
 */
public class HttpNodeImpl extends NodeBase implements HttpNode
{
    private final CasingPipeSpec pipeSpec = new CasingPipeSpec("http", this, new HttpCasingFactory(this), Fitting.HTTP_STREAM, Fitting.HTTP_TOKENS);
    private final PipeSpec[] pipeSpecs = new PipeSpec[] { pipeSpec };
    private final Logger logger = Logger.getLogger(HttpNodeImpl.class);

    private final SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
    private HttpSettings settings;

    public HttpNodeImpl( com.untangle.uvm.node.NodeSettings nodeSettings, com.untangle.uvm.node.NodeProperties nodeProperties )
    {
        super( nodeSettings, nodeProperties );
    }

    public HttpSettings getHttpSettings()
    {
        return settings;
    }

    public void setHttpSettings(final HttpSettings newSettings)
    {
        String nodeID = this.getNodeSettings().getId().toString();
        String settingsName = System.getProperty("uvm.settings.dir") + "/untangle-casing-http/settings_" + nodeID;
        String settingsFile = settingsName + ".js";

        try {
            settingsManager.save(HttpSettings.class, settingsName, newSettings);
        } catch(Exception exn) {
            logger.error("setHttpSettings()",exn);
            return;
        }

        this.settings = newSettings;
        
        reconfigure();
    }

    private void reconfigure()
    {
        if (null != settings) {
            pipeSpec.setEnabled(settings.isEnabled());
            pipeSpec.setReleaseParseExceptions(!settings.isNonHttpBlocked());
        }
    }

    protected void postInit()
    {
        String nodeID = this.getNodeSettings().getId().toString();
        String settingsName = System.getProperty("uvm.settings.dir") + "/untangle-casing-http/settings_" + nodeID;
        String settingsFile = settingsName + ".js";

        HttpSettings readSettings = null;
        logger.info("Loading settings from " + settingsFile );

        try {
            // first we try to read our json settings
            readSettings = settingsManager.load( HttpSettings.class, settingsName );
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
    protected PipeSpec[] getPipeSpecs()
    {
        return pipeSpecs;
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
