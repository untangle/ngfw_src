/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.untangle.uvm.license;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Iterator;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import org.apache.log4j.Logger;

import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.util.Pulse;
import com.untangle.uvm.SettingsManager;

public class LicenseManagerImpl implements LicenseManager
{
    private static final String LICENSE_URL_PROPERTY = "uvm.license.url";
    private static final String DEFAULT_LICENSE_URL = "https://license.untangle.com/license.php";
    
    private static final String LICENSE_SCRIPT_NUMUSERS = "license-numdevices.sh";
    private static final String EXPIRED = "expired";

    /* update every 12 hours, leaves an hour window */
    private static final long TIMER_DELAY = 1000 * 60 * 60 * 12;

    /* has to be declared after all static declarations */
    private static final LicenseManagerImpl INSTANCE;

    /**
     * Map from the product name to the latest valid license available for this product
     * This is where the fully evaluated license are stored
     * This map stores the evaluated (validated) licenses
     */
    private Map<String, License> licenseMap = new HashMap<String, License>();

    /**
     * A list of all known licenses
     * These are fully evaluated (validated) license
     */
    private List<License> licenseList = new LinkedList<License>();

    /**
     * The current settings
     * Contains a list of all known licenses store locally
     * Note: the licenses in the settings don't have metadata
     */
    private LicenseSettings settings;

    /**
     * Sync task
     */
    private final LycenseSyncTask task = new LycenseSyncTask();

    /**
     * Pulse that syncs the license, this is a daemon task.
     */
    private Pulse pulse = null;

    private final Logger logger = Logger.getLogger(getClass());

    private LicenseManagerImpl()
    {
        this.pulse = new Pulse("uvm-license", true, task);
        this.pulse.start(TIMER_DELAY);
        this._readLicenses();
        this._mapLicenses();
        try {
            reloadLicenses();
        } catch (Exception e) {
            logger.warn("Failed to reload licenses: ", e);
        }
    }

    /**
     * Reload all of the licenses from the file system.
     */
    @Override
    public final void reloadLicenses()
    {
        // we actually want to block here so call reloadLicenses directly instead of
        // firing the pulse
        _syncLicensesWithServer();
    }

    @Override
    public final License getLicense(String identifier)
    {
        /**
         * The free apps have no licenses
         * If they are requestd just return null
         */
        if ("untangle-node-adblocker".equals(identifier)) return null;
        else if ("untangle-node-clam".equals(identifier)) return null;
        else if ("untangle-node-cpd".equals(identifier)) return null;
        else if ("untangle-node-firewall".equals(identifier)) return null;
        else if ("untangle-node-ips".equals(identifier)) return null;
        else if ("untangle-node-openvpn".equals(identifier)) return null;
        else if ("untangle-node-phish".equals(identifier)) return null;
        else if ("untangle-node-protofilter".equals(identifier)) return null;
        else if ("untangle-node-reporting".equals(identifier)) return null;
        else if ("untangle-node-shield".equals(identifier)) return null;
        else if ("untangle-node-spamassassin".equals(identifier)) return null;
        else if ("untangle-node-spyware".equals(identifier)) return null;
        else if ("untangle-node-webfilter".equals(identifier)) return null;

        License license = this.licenseMap.get(identifier);
        if (license != null)
            return license;
        
        /**
         * Special for development environment
         * Assume all licenses are valid
         * This should be removed if you want to test the licensing in the dev environment
         */
        if (LocalUvmContextFactory.context().isDevel()) {
            logger.warn("Creating development license: " + identifier);
            license = new License(identifier, "0000-0000-0000-0000", identifier, "Development", 0, 9999999999l, "development", 1, Boolean.TRUE, "Developer");
            this.licenseMap.put(identifier,license);
            return license;
        }

        logger.warn("No license found for: " + identifier);

        /**
         * This returns an invalid license for all other requests
         * Note: this includes the free apps, however they don't actually check the license so it won't effect behavior
         * The UI will request the license of all app (including free)
         */
        license = new License(identifier, "0000-0000-0000-0000", identifier, "Subscription", 0, 0, "invalid", 1, Boolean.FALSE, "No License Found");
        this.licenseMap.put(identifier,license); /* add it to the map for faster response next time */
        return license;
    }

    @Override
    public final List<License> getLicenses()
    {
        return this.licenseList;
    }
    
    @Override
    public final boolean hasPremiumLicense()
    {
        return this.settings.getLicenses().size() > 0;
    }

    /**
     * Used by the LicenseManagerFactory to fetch the instance
     */
    public static LicenseManager getInstance()
    {
        return INSTANCE;
    }

    
    /**
     * Initialize the settings
     * (By default there are no liceneses)
     */
    private void _initializeSettings()
    {
        logger.info("Initializing Settings...");

        List<License> licenses = new LinkedList<License>();
        this.settings = new LicenseSettings(licenses);

        this._saveSettings(this.settings);
    }

    /**
     * Read the licenses and load them into the current settings object
     */
    private synchronized void _readLicenses()
    {
        SettingsManager settingsManager = LocalUvmContextFactory.context().settingsManager();
        
        try {
            this.settings = settingsManager.loadBasePath(LicenseSettings.class, System.getProperty("uvm.conf.dir"), "licenses", "licenses");
        } catch (SettingsManager.SettingsException e) {
            logger.error("Unable to read license file: ", e );
            return;
        }

        if (this.settings == null)
            _initializeSettings();

        return;
    }

    /**
     * This gets all the current revocations from the license server for this UID
     * and removes any licenses that have been revoked
     */
    @SuppressWarnings("unchecked") //LinkedList<LicenseRevocation> <-> LinkedList
    private synchronized void _checkRevocations()
    {
        SettingsManager settingsManager = LocalUvmContextFactory.context().settingsManager();
        LinkedList<LicenseRevocation> revocations;

        logger.info("REFRESH: Checking Revocations...");
        
        try {
            String urlStr = _getLicenseUrl() + "?" + "action=getRevocations" + "&" + "uid=" + LocalUvmContextFactory.context().getServerUID();
            logger.info("Downloading: \"" + urlStr + "\"");

            Object o = settingsManager.loadUrl(LinkedList.class, urlStr);
            revocations = (LinkedList<LicenseRevocation>)o;
        } catch (SettingsManager.SettingsException e) {
            logger.error("Unable to read license file: ", e );
            return;
        } catch (ClassCastException e) {
            logger.error("getRevocations returned unexpected response",e);
            return;
        }
            
        for (LicenseRevocation revoke : revocations) {
            _revokeLicense(revoke);
        }

        _saveSettings(settings);

        logger.info("REFRESH: Checking Revocations... done");

        return;
    }

    /** 
     * This remove a license from the list of current licenses
     */
    private synchronized void _revokeLicense(LicenseRevocation revoke)
    {
        if (this.settings == null || this.settings.getLicenses() == null) {
            logger.error("Invalid settings:" + this.settings);
            return;
        }
        if (revoke == null) {
            logger.error("Invalid argument:" + revoke);
            return;
        }

        /**
         * See if you find a match in the current licenses
         * If so, remove it
         */
        Iterator<License> itr = this.settings.getLicenses().iterator();
        while ( itr.hasNext() ) {
            License existingLicense = itr.next();
            if (existingLicense.getName().equals(revoke.getName())) {
                logger.warn("Revoking License: " + revoke.getName());
                itr.remove();
                return;
            }
        }
    }

    /**
     * This downloads a list of current licenese from the license server
     * Any new licenses are added. Duplicate licenses are updated if the new one grants better privleges
     */
    @SuppressWarnings("unchecked") //LinkedList<License> <-> LinkedList
    private synchronized void _downloadLicenses()
    {
        SettingsManager settingsManager = LocalUvmContextFactory.context().settingsManager();
        LinkedList<License> licenses;

        int numDevices = _getEstimatedNumDevices();
            
        logger.info("REFRESH: Downloading new Licenses...");
        
        try {
            String urlStr = _getLicenseUrl() + "?" + "action=getLicenses" + "&" + "uid=" + LocalUvmContextFactory.context().getServerUID() + "&" + "numDevices=" + numDevices;
            logger.info("Downloading: \"" + urlStr + "\"");

            Object o = settingsManager.loadUrl(LinkedList.class, urlStr);
            licenses = (LinkedList<License>)o;
        } catch (SettingsManager.SettingsException e) {
            logger.error("Unable to read license file: ", e );
            return;
        } catch (ClassCastException e) {
            logger.error("getRevocations returned unexpected response",e);
            return;
        }
        
        for (License lic : licenses) {
            _insertOrUpdate(lic);
        }

        _saveSettings(settings);

        logger.info("REFRESH: Downloading new Licenses... done");

        return;
    }

    /**
     * This takes the passed argument and inserts it into the current licenses
     * If there is currently an existing license for that product it will be removed
     */
    private synchronized void _insertOrUpdate(License license)
    {
        boolean insertNewLicense = true;
        
        if (this.settings == null || this.settings.getLicenses() == null) {
            logger.error("Invalid settings:" + this.settings);
            return;
        }
        if (license == null) {
            logger.error("Invalid argument:" + license);
            return;
        }

        /**
         * See if you find a match in the current licenses
         * If so, the new one replaces it so remove the existing one
         */
        Iterator<License> itr = this.settings.getLicenses().iterator();
        while ( itr.hasNext() ) {
            License existingLicense = itr.next();
            if (existingLicense.getName().equals(license.getName())) {

                /**
                 * As a measure of safety we only replace an existing license under certain circumstances
                 * This is so we are careful to only increase entitlements during this phase
                 */
                boolean replaceLicense = false;
                insertNewLicense = false;

                /**
                 * Check the validity of the current license
                 * If it isn't valid, we might as well try the new one
                 * Note: we have to use getLicenses to do this because the settings don't store validity
                 */
                if ( (getLicense(existingLicense.getName()) != null) && !(getLicense(existingLicense.getName()).getValid()) ) {
                    logger.info("REFRESH: Replacing license " + license + " - old one is invalid");
                    replaceLicense = true;
                }

                /**
                 * If the current one is a trial, and the new one is not, use the new one
                 */
                if ( !(License.LICENSE_TYPE_TRIAL.equals(license.getType())) && License.LICENSE_TYPE_TRIAL.equals(existingLicense.getType())) {
                    logger.info("REFRESH: Replacing license " + license + " - old one is trial");
                    replaceLicense = true;
                }

                /**
                 * If the new one has a later end date, use the new one
                 */
                if ( license.getEnd() > existingLicense.getEnd() ) {
                    logger.info("REFRESH: Replacing license " + license + " - new one has later end date");
                    replaceLicense = true;
                }

                if (replaceLicense) {
                    itr.remove();
                    insertNewLicense = true;
                } else {
                    logger.info("REFRESH: Keeping current license: " + license);
                }
            }
        }

        /**
         * if a match hasnt been found it needs to be added
         */
        if (insertNewLicense) {
            logger.info("REFRESH: Inserting new license   : " + license);
            List<License> licenses = this.settings.getLicenses();
            licenses.add(license);
        }
    }
    
    /**
     * update the app to License Map
     */
    private synchronized void _mapLicenses()
    {
        /* Create a new map of all of the valid licenses */
        Map<String, License> newMap = new HashMap<String, License>();
        LinkedList<License> newList = new LinkedList<License>();

        for (License lic : this.settings.getLicenses()) {

            /**
             * Create a duplicate - we're about to fill in metadata
             * But we don't want to mess with the original
             */
            License license = new License(lic);

            /**
             * Complete Meta-data
             */
            if (_isLicenseValid(license)) {
                license.setValid(Boolean.TRUE);
                license.setStatus("Valid"); /* XXX i18n */
            } else {
                license.setValid(Boolean.FALSE);
            }
            
            String identifier = license.getName();
            License current = newMap.get(identifier);

            /* current license is newer and better */
            if ((current != null) && (current.getEnd() > license.getEnd()))
                continue;

            logger.info("Adding License: " + license.getName() + " to Map. (valid: " + license.getValid() + ")");
            
            newMap.put(identifier, license);
            newList.add(license);
        }

        this.licenseMap = newMap;
        this.licenseList = newList;
    }

    /**
     * Verify the validity of a license
     */
    private boolean _isLicenseValid(License license)
    {
        long now = (System.currentTimeMillis()/1000);

        /* check if the license hasn't started yet (start date in future) */
        if (license.getStart() > now) {
            logger.warn( "The license: " + license + " isn't valid yet (" + license.getStart() + " > " + now + ")");
            license.setStatus("Invalid (Start Date in Future)"); /* XXX i18n */
            return false;
        }

        /* check if it is already expired */
        if ((license.getEnd() < now)) {
            logger.warn( "The license: " + license + " has expired (" + license.getEnd() + " < " + now + ")");
            license.setStatus("Invalid (Expired)"); /* XXX i18n */
            return false;
        }

        /* check the UID */
        if (license.getUID() == null || !license.getUID().equals(LocalUvmContextFactory.context().getServerUID())) {
            logger.warn( "The license: " + license + " does not match this server's UID (" + license.getUID() + " != " + LocalUvmContextFactory.context().getServerUID() + ")");
            license.setStatus("Invalid (UID Mismatch)"); /* XXX i18n */
            return false;
        }
        
        /* verify md5 */
        if (license.getKeyVersion() == 1) {
            //$string = "".$version.$uid.$name.$type.$start.$end."the meaning of life is 42";
            String input = license.getKeyVersion() + license.getUID() + license.getName() + license.getType() + license.getStart() + license.getEnd() + "the meaning of life is 42";
            //logger.info("KEY Input: " + input);
    
            MessageDigest md;
            try {
                md = MessageDigest.getInstance("MD5");
            } catch (java.security.NoSuchAlgorithmException e) {
                logger.warn( "Unknown Algorith MD5", e);
                license.setStatus("Invalid (Invalid Algorithm)");
                return false;
            }
            
            byte[] digest = md.digest(input.getBytes());

            String output = _toHex(digest);
            //logger.info("KEY Output: " + output);
            //logger.info("KEY Expect: " + license.getKey());

            if (!license.getKey().equals(output)) {
                logger.warn( "Invalid key: " + output );
                license.setStatus("Invalid (Invalid Key)");
                return false;
            }
        }
        else {
            logger.warn( "Unknown key version: " + license.getKeyVersion() );
            license.setStatus("Invalid (Invalid Key Version)");
            return false;
        }

        logger.debug("License " + license + " is valid.");
        return true;
    }

    /**
     * Convert the bytes to a hex string
     */
    private String _toHex(byte data[])
    {
        String response = "";
        for (byte b : data) {
            int c = b;
            if (c < 0)
                c = c + 0x100;
            response += String.format("%02x", c);
        }

        return response;
    }

    /**
     * Set the current settings to new Settings
     * Also save the settings to disk if save is true
     */
    private void _saveSettings(LicenseSettings newSettings)
    {
        /**
         * Save the settings
         */
        SettingsManager settingsManager = LocalUvmContextFactory.context().settingsManager();
        try {
            settingsManager.saveBasePath(LicenseSettings.class, System.getProperty("uvm.conf.dir"), "licenses", "licenses", newSettings);
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to save settings.",e);
        }

        /**
         * Change current settings
         */
        this.settings = newSettings;

    }
    
    /**
     * Returns an estimate of # devices on the network
     * This is not meant to be very accurate - it is just an estimate
     */
    private int _getEstimatedNumDevices()
    {
        try {
            String command = System.getProperty("uvm.bin.dir") + "/" + LICENSE_SCRIPT_NUMUSERS;
            Process proc = LocalUvmContextFactory.context().exec(command);
            InputStream is  = proc.getInputStream();
            OutputStream os = proc.getOutputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(is));
            os.close();

            StringBuilder wholeOutput = new StringBuilder();
            String s;
            while ((s = in.readLine()) != null) {
                wholeOutput.append(s);
            }

            in.close();
            is.close();

            Integer result = new Integer(wholeOutput.toString());
            return result;
        } catch (Exception e) {
            logger.warn("Unabled to estimate seats",e);
        }

        return -1;
    }

    /**
     * Returns the url for the license server API
     */
    private String _getLicenseUrl()
    {
        String urlStr = System.getProperty(LICENSE_URL_PROPERTY);
        
        if (urlStr == null)
            urlStr = DEFAULT_LICENSE_URL;

        return urlStr;
    }

    /**
     * syncs the license server state with local state
     */
    private void _syncLicensesWithServer()
    {
        logger.info("Reloading licenses..." );

        synchronized (LicenseManagerImpl.this) {
            _readLicenses();

            if (! LocalUvmContextFactory.context().isDevel()) {
                _downloadLicenses();
                _checkRevocations();
            }
                
            _mapLicenses();
        }

        logger.info("Reloading licenses... done" );
    }
    
    private class LycenseSyncTask implements Runnable
    {
        public void run()
        {
            _syncLicensesWithServer();    
        }
    }
    
    static {
        INSTANCE = new LicenseManagerImpl();
    }
}
