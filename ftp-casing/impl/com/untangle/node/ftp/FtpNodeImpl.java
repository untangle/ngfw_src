/**
 * $Id$
 */
package com.untangle.node.ftp;

import com.untangle.uvm.vnet.NodeBase;
import com.untangle.uvm.vnet.CasingPipeSpec;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipeSpec;
import com.untangle.uvm.SettingsManager;
import org.apache.log4j.Logger;
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;

/**
 * FTP node implementation.
 */
public class FtpNodeImpl extends NodeBase
    implements FtpNode
{
    private final PipeSpec ctlPipeSpec = new CasingPipeSpec("ftp", this, FtpCasingFactory.factory(),Fitting.FTP_CTL_STREAM, Fitting.FTP_CTL_TOKENS);
    private final PipeSpec dataPipeSpec = new CasingPipeSpec("ftp", this, FtpCasingFactory.factory(),Fitting.FTP_DATA_STREAM, Fitting.FTP_DATA_TOKENS);
    private final PipeSpec[] pipeSpecs = new PipeSpec[] { ctlPipeSpec, dataPipeSpec };
    private final Logger logger = Logger.getLogger(FtpNodeImpl.class);
    private final SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
    private FtpSettings settings;

    // constructors -----------------------------------------------------------

    public FtpNodeImpl( com.untangle.uvm.node.NodeSettings nodeSettings, com.untangle.uvm.node.NodeProperties nodeProperties )
    {
        super( nodeSettings, nodeProperties );
    }

    // FtpNode methods ---------------------------------------------------

    public FtpSettings getFtpSettings()
    {
        return settings;
    }

    public void setFtpSettings(final FtpSettings newSettings)
    {
        String nodeID = this.getNodeSettings().getId().toString();
        String settingsName = System.getProperty("uvm.settings.dir") + "/untangle-casing-ftp/settings_" + nodeID;
        String settingsFile = settingsName + ".js";

        try {
            settingsManager.save(FtpSettings.class, settingsName, newSettings);
        } catch(Exception exn) {
            logger.error("setFtpSettings()",exn);
            return;
        }

        this.settings = newSettings;
        
        reconfigure();
    }

    // Node methods ------------------------------------------------------

    public void reconfigure()
    {
        if (null != settings) {
            ctlPipeSpec.setEnabled(settings.isEnabled());
            dataPipeSpec.setEnabled(settings.isEnabled());
        }
    }

    public void initializeSettings() { }

    protected void postInit()
    {
        String nodeID = this.getNodeSettings().getId().toString();
        String settingsName = System.getProperty("uvm.settings.dir") + "/untangle-casing-ftp/settings_" + nodeID;
        String settingsFile = settingsName + ".js";

        FtpSettings readSettings = null;
        logger.info("Loading settings from " + settingsFile );

        try {
            // first we try to read our json settings
            readSettings = settingsManager.load( FtpSettings.class, settingsName );
        } catch (Exception exn) {
            logger.error("postInit()",exn);
        }

        try {
            // still no settings found so init with defaults
            if (readSettings == null)
            {
                logger.warn("No settings found... initializing with defaults");
                setFtpSettings(new FtpSettings());
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

    // NodeBase methods ----------------------------------------------

    @Override
    protected PipeSpec[] getPipeSpecs()
    {
        return pipeSpecs;
    }

    // XXX soon to be deprecated ----------------------------------------------

    public Object getSettings()
    {
        return getFtpSettings();
    }

    public void setSettings(Object settings)
    {
        setFtpSettings((FtpSettings)settings);
    }
}
