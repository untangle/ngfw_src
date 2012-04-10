/*
 * $Id$
 */
package com.untangle.node.http;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.vnet.AbstractNode;
import com.untangle.uvm.vnet.CasingPipeSpec;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipeSpec;
import com.untangle.uvm.SettingsManager;
import org.apache.log4j.Logger;
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;

/**
 * An HTTP casing node.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class HttpNodeImpl extends AbstractNode implements HttpNode
{
    private static final String SETTINGS_CONVERSION_SCRIPT = System.getProperty( "uvm.bin.dir" ) + "/http-casing-convert-settings.py";
    private final CasingPipeSpec pipeSpec = new CasingPipeSpec("http", this, new HttpCasingFactory(this), Fitting.HTTP_STREAM, Fitting.HTTP_TOKENS);
    private final PipeSpec[] pipeSpecs = new PipeSpec[] { pipeSpec };
    private final Logger logger = Logger.getLogger(HttpNodeImpl.class);

    private final SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
    private HttpSettings settings;

    public HttpNodeImpl() {}

    public HttpSettings getHttpSettings()
    {
        return settings;
    }

    public void setHttpSettings(final HttpSettings settings)
    {
        String nodeID = this.getNodeId().getId().toString();
        String settingsName = System.getProperty("uvm.settings.dir") + "/untangle-casing-http/settings_" + nodeID;
        String settingsFile = settingsName + ".js";

        this.settings = settings;

        try
        {
            settingsManager.save(HttpSettings.class, settingsName, settings);
        }

        catch(Exception exn)
        {
            logger.error("setHttpSettings()",exn);
        }

        reconfigure();
    }

    private void reconfigure()
    {
        if (null != settings) {
            pipeSpec.setEnabled(settings.isEnabled());
            pipeSpec.setReleaseParseExceptions(!settings.isNonHttpBlocked());
        }
    }

    protected void postInit(String[] args)
    {
        String nodeID = this.getNodeId().getId().toString();
        String settingsName = System.getProperty("uvm.settings.dir") + "/untangle-casing-http/settings_" + nodeID;
        String settingsFile = settingsName + ".js";

        HttpSettings readSettings = null;
        logger.info("Loading settings from " + settingsFile );

        try
        {
            // first we try to read our json settings
            readSettings = settingsManager.load( HttpSettings.class, settingsName );
        }

        catch (Exception exn)
        {
            logger.error("postInit()",exn);
        }

        // if no settings found try importing from the database
        if (readSettings == null)
        {
            logger.info("No json settings found... attempting to import from database");

            try
            {
                String convertCmd = SETTINGS_CONVERSION_SCRIPT + " " + settingsFile;
                logger.info("Running: " + convertCmd);
                UvmContextFactory.context().execManager().exec( convertCmd );
            }

            catch (Exception exn)
            {
                logger.error("Conversion script failed", exn);
            }

            try
            {
                // try to read the settings created by the conversion script
                readSettings = settingsManager.load( HttpSettings.class, settingsName );
            }

            catch (Exception exn)
            {
                logger.error("Could not read node settings", exn);
            }
        }

        try
        {
            // still no settings found so init with defaults
            if (readSettings == null)
            {
                logger.warn("No database or json settings found... initializing with defaults");
                setHttpSettings(new HttpSettings());
            }

            // otherwise apply the loaded or imported settings from the file
            else
            {
                logger.info("Loaded settings from " + settingsFile);
                this.settings = readSettings;
                reconfigure();
            }
        }

        catch (Exception exn)
        {
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
