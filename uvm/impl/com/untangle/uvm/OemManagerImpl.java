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
        else this.settings = new OemSettings("Untangle", "http://untangle.com", "https://www.untangle.com/legal", false, false);

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
     * Get the OEM license agreement url
     *
     * @return the license agreement url
     */
    @Override 
    public String getOemLicenseAgreementUrl() 
    {
        return this.settings.getOemLicenseAgreementUrl();
    }

    /**
     * Get if this is an OEM
     * 
     * @return if is oem
     */
    @Override
    public Boolean getIsOem() 
    {
        return this.settings.getIsOem();
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
