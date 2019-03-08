/**
 * $Id$
 */

package com.untangle.uvm;

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

    /**
     * Constructor
     */
    public OemManagerImpl()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        OemSettings readSettings = null;
        String settingsFileName = System.getProperty("uvm.conf.dir") + "/" + "oem.js";

        try {
            readSettings = settingsManager.load(OemSettings.class, settingsFileName);
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to load settings:", e);
        }

        if (readSettings != null) this.settings = readSettings;
        else this.settings = new OemSettings("Untangle", "http://untangle.com");

        logger.info("OEM: " + this.settings.getOemName());
    }

    /**
     * Get the OEM name
     * 
     * @return The OEM name
     */
    @Override
    public String getOemName()
    {
        return this.settings.getOemName();
    }

    /**
     * Get the OEM URL
     * 
     * @return The OEM URL
     */
    @Override
    public String getOemUrl()
    {
        return this.settings.getOemUrl();
    }
}
