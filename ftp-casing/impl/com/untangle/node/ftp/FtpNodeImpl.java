/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package com.untangle.node.ftp;

import com.untangle.uvm.util.TransactionWork;
import com.untangle.uvm.vnet.AbstractNode;
import com.untangle.uvm.vnet.CasingPipeSpec;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipeSpec;
import com.untangle.uvm.SettingsManager;
import org.apache.log4j.Logger;
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;

/**
 * FTP node implementation.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class FtpNodeImpl extends AbstractNode
    implements FtpNode
{
    private static final String SETTINGS_CONVERSION_SCRIPT = System.getProperty( "uvm.bin.dir" ) + "/ftp-casing-convert-settings.py";
    private final PipeSpec ctlPipeSpec = new CasingPipeSpec("ftp", this, FtpCasingFactory.factory(),Fitting.FTP_CTL_STREAM, Fitting.FTP_CTL_TOKENS);
    private final PipeSpec dataPipeSpec = new CasingPipeSpec("ftp", this, FtpCasingFactory.factory(),Fitting.FTP_DATA_STREAM, Fitting.FTP_DATA_TOKENS);
    private final PipeSpec[] pipeSpecs = new PipeSpec[] { ctlPipeSpec, dataPipeSpec };
    private final Logger logger = Logger.getLogger(FtpNodeImpl.class);
    private final SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
    private FtpSettings settings;

    // constructors -----------------------------------------------------------

    public FtpNodeImpl() { }

    // FtpNode methods ---------------------------------------------------

    public FtpSettings getFtpSettings()
    {
        return settings;
    }

    public void setFtpSettings(final FtpSettings settings)
    {
        String nodeID = this.getNodeId().getId().toString();
        String settingsName = System.getProperty("uvm.settings.dir") + "/untangle-casing-ftp/settings_" + nodeID;
        String settingsFile = settingsName + ".js";

        this.settings = settings;

        try
        {
            settingsManager.save(FtpSettings.class, settingsName, settings);
        }

        catch(Exception exn)
        {
            logger.error("setFtpSettings()",exn);
        }

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

    protected void postInit(String[] args)
    {
        String nodeID = this.getNodeId().getId().toString();
        String settingsName = System.getProperty("uvm.settings.dir") + "/untangle-casing-ftp/settings_" + nodeID;
        String settingsFile = settingsName + ".js";

        FtpSettings readSettings = null;
        logger.info("Loading settings from " + settingsFile );

        try
        {
            // first we try to read our json settings
            readSettings = settingsManager.load( FtpSettings.class, settingsName );
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
                readSettings = settingsManager.load( FtpSettings.class, settingsName );
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
                setFtpSettings(new FtpSettings());
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

    // AbstractNode methods ----------------------------------------------

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
