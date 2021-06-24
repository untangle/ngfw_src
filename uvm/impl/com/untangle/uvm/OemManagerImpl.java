/**
 * $Id$
 */

package com.untangle.uvm;

import java.io.File;

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

    private static final String SETTINGS_OVERRIDE_SCRIPT = System.getProperty("uvm.bin.dir") + "/oem-settings-override.py";
    private static final String SETTINGS_OVERRIDE_FILE = System.getProperty("uvm.conf.dir") + "/oem-settings-override.js";
    private static final String OVERRIDE_TEMPORARY_FILE = "/tmp/oem-temporary-settings.js";

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

    /**
     * Called to apply OEM overrides to default settings
     *
     * @param argSettings
     *        The default settings to be processed
     * @return The updated settings with OEM overrides applied or the argumented
     *         settings if there are any problems.
     */
    @Override
    public Object applyOemOverrides(Object argSettings)
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        ExecManager execManager = UvmContextFactory.context().execManager();

        // if the override file does not exist just return the settings unchanged
        File checker = new File(SETTINGS_OVERRIDE_FILE);
        if (!checker.exists()) {
            return argSettings;
        }

        logger.info("Applying OEM overrides for " + argSettings.getClass().getSimpleName());

        // save the passed settings to a temporary file
        try {
            settingsManager.save(OVERRIDE_TEMPORARY_FILE, argSettings, false);
        } catch (Exception exn) {
            logger.warn("Exception saving settings", exn);
            return argSettings;
        }

        // call the override script to process the overrides
        int result = execManager.execResult(SETTINGS_OVERRIDE_SCRIPT + " " + OVERRIDE_TEMPORARY_FILE + " " + SETTINGS_OVERRIDE_FILE + " " + argSettings.getClass().getSimpleName() + " " + OVERRIDE_TEMPORARY_FILE);
        if (result != 0) {
            return argSettings;
        }

        Object overrideSettings = null;

        // load the modified settings from the temporary file
        try {
            overrideSettings = settingsManager.load(argSettings.getClass(), OVERRIDE_TEMPORARY_FILE);
        } catch (Exception exn) {
            logger.warn("Exception loading settings", exn);
            return argSettings;
        }

        // return the updated settings to the caller
        return (overrideSettings);
    }
}
