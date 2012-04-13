/*
 * $Id$
 */
package com.untangle.node.license;

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

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.vnet.NodeBase;
import com.untangle.uvm.vnet.PipeSpec;
import com.untangle.uvm.util.Pulse;
import com.untangle.uvm.node.License;
import com.untangle.uvm.node.LicenseManager;

public class LicenseManagerImpl extends NodeBase implements LicenseManager
{
    private static final String LICENSE_URL_PROPERTY = "uvm.license.url";
    private static final String DEFAULT_LICENSE_URL = "https://license.untangle.com/license.php";
    
    private static final String LICENSE_SCRIPT_NUMUSERS = "license-numdevices.sh";
    private static final String EXPIRED = "expired";

    /* update every 12 hours, leaves an hour window */
    private static final long TIMER_DELAY = 1000 * 60 * 60 * 12;

    private final Logger logger = Logger.getLogger(getClass());

    private final PipeSpec[] pipeSpecs = new PipeSpec[0];

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

    public LicenseManagerImpl( com.untangle.uvm.NodeSettings nodeSettings, com.untangle.uvm.node.NodeProperties nodeProperties )
    {
        super( nodeSettings, nodeProperties );

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

    @Override
    protected void preStop()
    {
        super.preStop();
        logger.debug("preStop()");
    }

    @Override
    protected void postStart()
    {
        logger.debug("postStart()");

        /* save the current license state on every start */
        /* We do this just in case local changes have been made to the file */
        /* Some applications read settings from the file directly */
        this._saveSettings(this.settings);
        
        /* Reload the licenses */
        try {
            UvmContextFactory.context().licenseManager().reloadLicenses();
        } catch ( Exception ex ) {
            logger.warn( "Unable to reload the licenses." );
        }
    }

    @Override
    protected PipeSpec[] getPipeSpecs()
    {
        return pipeSpecs;
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
        if (isGPLApp(identifier))
            return null;

        License license = this.licenseMap.get(identifier);
        if (license != null)
            return license;
        
        /**
         * Special for development environment
         * Assume all licenses are valid
         * This should be removed if you want to test the licensing in the dev environment
         */
        if (UvmContextFactory.context().isDevel()) {
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
    public final boolean isLicenseValid(String identifier)
    {
        if (isGPLApp(identifier))
            return true;

        License lic = getLicense(identifier);
        if (lic == null)
            return false;
        Boolean isValid = lic.getValid();
        if (isValid == null)
            return false;
        else
            return isValid;
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

    public Object getSettings()
    {
        /* These are controlled using the methods in the uvm class */
        return null;
    }

    public void setSettings(Object settings)
    {
        /* These are controlled using the methods in the uvm class */
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
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        
        try {
            this.settings = settingsManager.load( LicenseSettings.class, System.getProperty("uvm.conf.dir") + "/licenses/licenses" );
        } catch (SettingsManager.SettingsException e) {
            logger.error("Unable to read license file: ", e );
            return;
        }

        if (this.settings == null)
            _initializeSettings();

        /**
         * Re-compute metadata - we don't want to use value in file (could have been changed)
         */
        if (this.settings.getLicenses() != null) {
            for (License lic : this.settings.getLicenses()) {
                _setValidAndStatus(lic);
            }
        }

        return;
    }

    /**
     * This gets all the current revocations from the license server for this UID
     * and removes any licenses that have been revoked
     */
    @SuppressWarnings("unchecked") //LinkedList<LicenseRevocation> <-> LinkedList
    private synchronized void _checkRevocations()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        LinkedList<LicenseRevocation> revocations;
        boolean changed = false;
        
        logger.info("REFRESH: Checking Revocations...");
        
        try {
            String urlStr = _getLicenseUrl() + "?" + "action=getRevocations" + "&" + "uid=" + UvmContextFactory.context().getServerUID();
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
            changed |= _revokeLicense(revoke);
        }

        if (changed)
            _saveSettings(settings);

        logger.info("REFRESH: Checking Revocations... done (modified: " + changed + ")");

        return;
    }

    /** 
     * This remove a license from the list of current licenses
     * Returns true if a license was removed, false otherwise
     */
    private synchronized boolean _revokeLicense(LicenseRevocation revoke)
    {
        if (this.settings == null || this.settings.getLicenses() == null) {
            logger.error("Invalid settings:" + this.settings);
            return false;
        }
        if (revoke == null) {
            logger.error("Invalid argument:" + revoke);
            return false;
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
                return true;
            }
        }

        return false;
    }

    /**
     * This downloads a list of current licenese from the license server
     * Any new licenses are added. Duplicate licenses are updated if the new one grants better privleges
     */
    @SuppressWarnings("unchecked") //LinkedList<License> <-> LinkedList
    private synchronized void _downloadLicenses()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        LinkedList<License> licenses;
        boolean changed = false;

        int numDevices = _getEstimatedNumDevices();
        String uvmVersion = UvmContextFactory.context().version();
        
        logger.info("REFRESH: Downloading new Licenses...");
        
        try {
            String urlStr = _getLicenseUrl() + "?" + "action=getLicenses" + "&" + "uid=" + UvmContextFactory.context().getServerUID() + "&" + "numDevices=" + numDevices + "&" + "version=" + uvmVersion;
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
            changed |= _insertOrUpdate(lic);
        }

        if (changed)
            _saveSettings(settings);

        logger.info("REFRESH: Downloading new Licenses... done (changed: " + changed + ")");

        return;
    }

    /**
     * This takes the passed argument and inserts it into the current licenses
     * If there is currently an existing license for that product it will be removed
     * Returns true if a license was added or modified, false otherwise
     */
    private synchronized boolean _insertOrUpdate(License license)
    {
        boolean insertNewLicense = true;
        
        if (this.settings == null || this.settings.getLicenses() == null) {
            logger.error("Invalid settings:" + this.settings);
            return false;
        }
        if (license == null) {
            logger.error("Invalid argument:" + license);
            return false;
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
            return true;
        }

        return false;
    }
    
    /**
     * update the app to License Map
     */
    private synchronized void _mapLicenses()
    {
        /* Create a new map of all of the valid licenses */
        Map<String, License> newMap = new HashMap<String, License>();
        LinkedList<License> newList = new LinkedList<License>();

        if (this.settings != null) {
            for (License lic : this.settings.getLicenses()) {
                /**
                 * Create a duplicate - we're about to fill in metadata
                 * But we don't want to mess with the original
                 */
                License license = new License(lic);

                /**
                 * Complete Meta-data
                 */
                _setValidAndStatus(license);
            
                String identifier = license.getName();
                License current = newMap.get(identifier);

                /* current license is newer and better */
                if ((current != null) && (current.getEnd() > license.getEnd()))
                    continue;

                logger.info("Adding License: " + license.getName() + " to Map. (valid: " + license.getValid() + ")");
            
                newMap.put(identifier, license);
                newList.add(license);
            }
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
        if (license.getUID() == null || !license.getUID().equals(UvmContextFactory.context().getServerUID())) {
            logger.warn( "The license: " + license + " does not match this server's UID (" + license.getUID() + " != " + UvmContextFactory.context().getServerUID() + ")");
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
         * Compute metadata before saving
         */
        for (License lic : newSettings.getLicenses()) {
            _setValidAndStatus(lic);
        }

        /**
         * Save the settings
         */
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        try {
            settingsManager.save(LicenseSettings.class, System.getProperty("uvm.conf.dir") + "/licenses/licenses", newSettings);
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
            String wholeOutput = UvmContextFactory.context().execManager().execOutput(command);
            Integer result = new Integer(wholeOutput.replaceAll("\\W",""));
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

            if (! UvmContextFactory.context().isDevel()) {
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
    
    private boolean isGPLApp(String identifier)
    {
        if ("untangle-node-adblocker".equals(identifier)) return true;
        else if ("untangle-node-clam".equals(identifier)) return true;
        else if ("untangle-node-cpd".equals(identifier)) return true;
        else if ("untangle-node-firewall".equals(identifier)) return true;
        else if ("untangle-node-ips".equals(identifier)) return true;
        else if ("untangle-node-openvpn".equals(identifier)) return true;
        else if ("untangle-node-phish".equals(identifier)) return true;
        else if ("untangle-node-protofilter".equals(identifier)) return true;
        else if ("untangle-node-reporting".equals(identifier)) return true;
        else if ("untangle-node-shield".equals(identifier)) return true;
        else if ("untangle-node-spamassassin".equals(identifier)) return true;
        else if ("untangle-node-spyware".equals(identifier)) return true;
        else if ("untangle-node-webfilter".equals(identifier)) return true;

        return false;
    }

    private void _setValidAndStatus(License license)
    {
        if (_isLicenseValid(license)) {
            license.setValid(Boolean.TRUE);
            license.setStatus("Valid");
        } else {
            license.setValid(Boolean.FALSE);
        }
    }
}
