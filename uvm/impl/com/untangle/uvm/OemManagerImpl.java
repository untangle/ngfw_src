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
        else this.settings = new OemSettings("Arista", "Arista", "Untangle Server", "http://untangle.com", "https://www.untangle.com/legal", false);

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
     * Get the OEM short name
     * 
     * @return The OEM short name
     */
    @Override
    public String getOemShortName()
    {
        return this.settings.getOemShortName();
    }

    /**
     * Get the OEM product name
     * 
     * @return The OEM product name
     */
    @Override
    public String getOemProductName()
    {
        return this.settings.getOemProductName();
    }

    /**
     * Get the OEM URL
     * 
     * @return The OEM URL
     */
    @Override
    public String getOemUrl()
    {
        UriManager uriManager = UvmContextFactory.context().uriManager();
        return uriManager.getUri(this.settings.getOemUrl());
    }

    /**
     * Check if oem override file exists to avoid having settings saved twice
     * @return if oem file exists
     */
    @Override
    public boolean hasOemOverrideFile()
    {
        // if the override file does not exist return false
        File checker = new File(SETTINGS_OVERRIDE_FILE);
        if (checker.exists()) {
            return true;
        }

        return false;
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
        String originalFile = "/tmp/oem-original-" + argSettings.getClass().getSimpleName() + ".js";
        String modifiedFile = "/tmp/oem-modified-" + argSettings.getClass().getSimpleName() + ".js";

        // if the override file does not exist just return the settings unchanged
        File checker = new File(SETTINGS_OVERRIDE_FILE);
        if (!checker.exists()) {
            return argSettings;
        }

        logger.info("Applying OEM overrides for " + argSettings.getClass().getSimpleName());

        // save the passed settings to the original temporary file
        try {
            settingsManager.save(originalFile, argSettings, false);
        } catch (Exception exn) {
            logger.warn("Exception saving settings", exn);
            return argSettings;
        }

        // call the override script to apply the overrides to the original
        // file and write the new settings to the modified file
        int result = execManager.execResult(SETTINGS_OVERRIDE_SCRIPT + " " + originalFile + " " + SETTINGS_OVERRIDE_FILE + " " + argSettings.getClass().getSimpleName() + " " + modifiedFile);
        if (result != 0) {
            return argSettings;
        }

        Object overrideSettings = null;

        // load the settings from the modified file
        try {
            overrideSettings = settingsManager.load(argSettings.getClass(), modifiedFile);
        } catch (Exception exn) {
            logger.warn("Exception loading settings", exn);
            return argSettings;
        }

        // return the updated settings to the caller
        return (overrideSettings);
    }

    /**
     * Get the license agreement url
     *
     * @return the license agreement url
     */
    @Override 
    public String getLicenseAgreementUrl() 
    {
        UriManager uriManager = UvmContextFactory.context().uriManager();
        return uriManager.getUri(this.settings.getLicenseAgreementUrl());
    }

    /**
     * Get if local eula should be used
     * 
     * @return if local eula should be used
     */
    @Override
    public Boolean getUseLocalEula() 
    {
        return this.settings.getUseLocalEula();    
    }
}
