/*
 * $Id: OemManagerImpl.java,v 1.00 2011/08/11 13:35:05 dmorris Exp $
 */
package com.untangle.uvm.engine;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.OemManager;
import com.untangle.uvm.OemSettings;

/**
 * OemManagerImpl determines the OEM name and URL
 */
public class OemManagerImpl implements OemManager
{
    private final Logger logger = Logger.getLogger(getClass());

    private OemSettings settings;
    
    public OemManagerImpl()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        OemSettings readSettings = null;
        String settingsFileName = System.getProperty("uvm.conf.dir") + "/" + "oem.js";

        try {
            readSettings = settingsManager.load( OemSettings.class, settingsFileName );
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to load settings:",e);
        }

        if (readSettings != null)
            this.settings = readSettings;
        else
            this.settings = new OemSettings("Untangle","http://untangle.com");

        logger.info("OEM: " + this.settings.getOemName());
    }

    @Override
    public String getOemName()
    {
        return this.settings.getOemName();
    }

    @Override
    public String getOemUrl()
    {
        return this.settings.getOemUrl();
    }
}
