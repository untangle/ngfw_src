/**
 * $Id: FacebookAuthenticator.java,v 1.00 2017/03/03 19:30:10 dmorris Exp $
 * 
 * Copyright (c) 2003-2017 Untangle, Inc.
 *
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Untangle.
 */
package com.untangle.node.license;

import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
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
import org.apache.http.HttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.CloseableHttpResponse;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.HookManager;
import com.untangle.uvm.node.AppBase;
import com.untangle.uvm.vnet.PipelineConnector;
import com.untangle.uvm.util.Pulse;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.node.License;
import com.untangle.uvm.node.LicenseManager;

public class LicenseManagerImpl extends AppBase implements LicenseManager
{
    private static final String LICENSE_URL_PROPERTY = "uvm.license.url";
    private static final String DEFAULT_LICENSE_URL = "https://license.untangle.com/license.php";

    private static final double LIENENCY_PERCENT = 1.25; /* the enforced seat limit is the license seat limit TIMES this value */
    private static final int    LIENENCY_CONSTANT = 5; /* the enforced seat limit is the license seat limit PLUS this value */
    private static final String LIENENCY_GIFT_FILE = System.getProperty("uvm.conf.dir") + "/gift"; /* the file that defines the gift value */
    private static final int    LIENENCY_GIFT = getLienencyGift(); /* and extra lienency constant */
    
    private static final String EXPIRED = "expired";

    /**
     * update every 4 hours, leaves an hour window
     */
    private static final long TIMER_DELAY = 1000 * 60 * 60 * 4;

    private static final Logger logger = Logger.getLogger(LicenseManagerImpl.class);

    private final PipelineConnector[] connectors = new PipelineConnector[] {};

    /**
     * Map from the product name to the latest valid license available for this product
     * This is where the fully evaluated license are stored
     * This map stores the evaluated (validated) licenses
     */
    private Map<String, License> licenseMap = new ConcurrentHashMap<String, License>();

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
    private final LicenseSyncTask task = new LicenseSyncTask();

    /**
     * Pulse that syncs the license, this is a daemon task.
     */
    private Pulse pulse = null;

    public LicenseManagerImpl( com.untangle.uvm.node.AppSettings appSettings, com.untangle.uvm.node.AppProperties appProperties )
    {
        super( appSettings, appProperties );

        this._readLicenses();
        this._mapLicenses();

        this.pulse = new Pulse("uvm-license", task, TIMER_DELAY);
        this.pulse.start();
    }

    @Override
    protected void preStop( boolean isPermanentTransition )
    {
        logger.debug("preStop()");
    }

    @Override
    protected void postStart( boolean isPermanentTransition )
    {
        logger.debug("postStart()");

        /* Reload the licenses */
        UvmContextFactory.context().licenseManager().reloadLicenses( false );
    }

    @Override
    protected PipelineConnector[] getConnectors()
    {
        return this.connectors;
    }
    
    /**
     * Reload all of the licenses from the file system.
     */
    @Override
    public final void reloadLicenses( boolean blocking )
    {
        if ( blocking ) {
            try {
                _syncLicensesWithServer();
            } catch ( Exception ex ) {
                logger.warn( "Unable to reload the licenses.", ex );
            }
        } else {
            Thread t = new Thread(new Runnable() {
                    public void run()
                    {
                        try {
                            _syncLicensesWithServer();
                        } catch ( Exception ex ) {
                            logger.warn( "Unable to reload the licenses.", ex );
                        }
                    }
                });
            t.run();
        }
    }

    @Override
    public final License getLicense(String identifier)
    {
        if (isGPLApp(identifier))
            return null;

        /**
         * Check the correct name first,
         * If the license exists and is valid use that one
         */
        License license = null;
        License oldLicense = null;
        License longLicense = null;
        License oldLongLicense = null;

        license = this.licenseMap.get(identifier);
        logger.debug("getLicense(" + identifier + ") = " + license );

        String oldIdentifier = getOldIdentifier(identifier);
        if ( oldIdentifier != null ) {
            oldLicense = this.licenseMap.get(oldIdentifier);
            logger.debug("getLicense(" + oldIdentifier + ") = " + oldLicense );

            String oldLongIdentifier = getLongIdentifier(identifier);
            if ( oldLongIdentifier != null ) {
                oldLongLicense = this.licenseMap.get(oldLongIdentifier);
                logger.debug("getLicense(" + oldLongIdentifier + ") = " + oldLongLicense );
            }

        }
        String longIdentifier = getLongIdentifier(identifier);
        if ( longIdentifier != null ) {
            longLicense = this.licenseMap.get(longIdentifier);
            logger.debug("getLicense(" + longIdentifier + ") = " + longLicense );
        }

        /**
         * Ranked in order of preference
         */
        if (license != null && license.getValid())
            return license;
        if (longLicense != null && longLicense.getValid())
            return longLicense;
        if (oldLicense != null && oldLicense.getValid())
            return oldLicense;
        if (oldLongLicense != null && oldLongLicense.getValid())
            return oldLongLicense;

        if (license != null)
            return license;
        if (longLicense != null)
            return longLicense;
        if (oldLicense != null)
            return oldLicense;
        if (oldLongLicense != null)
            return oldLongLicense;


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
        license = new License(identifier, "0000-0000-0000-0000", identifier, "Subscription", 0, 0, "invalid", 1, Boolean.FALSE, I18nUtil.marktr("No License Found"));
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
        return validLicenseCount() > 0;
    }

    @Override
    public int validLicenseCount()
    {
        int validCount = 0;
        for ( License lic : this.settings.getLicenses() ) {
            try {
                if (lic.getValid())
                    validCount++;
            } catch (Exception e) {
                logger.warn("Exception",e);
            }
        }
        return validCount;
    }
    
    @Override
    public int getSeatLimit( )
    {
        return getSeatLimit( true );
    }

    @Override
    public int getSeatLimit( boolean lienency )
    {
        if ( UvmContextFactory.context().isDevel() )
            return -1;

        int seats = -1;
        for (License lic : this.settings.getLicenses()) {
            if ( ! lic.getValid() || lic.getTrial() ) // only count valid non-trials
                continue;
            if ( lic.getSeats() == null )
                continue;
            if ( lic.getSeats() <= 0 ) //ignore invalid seat ranges
                continue;
            if ( lic.getSeats() > seats && seats > 0 ) //if there is already a lower count limit, ignore this one
                continue;
            seats = lic.getSeats();
        }

        if ( seats > 0 && lienency )
            seats = ((int)Math.round(((double)seats)*LIENENCY_PERCENT)) + LIENENCY_CONSTANT + LIENENCY_GIFT;
        
        return seats;
    }

    @Override
    public void requestTrialLicense( String nodeName ) throws Exception
    {
        if ( nodeName == null ) {
            logger.warn("Invalid name: " + nodeName);
            return;
        }
        // if already have a valid license, just return
        if ( UvmContextFactory.context().licenseManager().isLicenseValid( nodeName ) ) {
            logger.warn("Already have a valid license for: " + nodeName);
            return;
        }
        
        String licenseUrl = System.getProperty( "uvm.license.url" );
        if ( licenseUrl == null )
            licenseUrl = "https://license.untangle.com/license.php";


        /**
         * The API specifies libitem, however libitems no longer exist
         * First we try with the actual name, then the libitem time
         * Then we try with the old app name (if it has an old name), then we try with the old libitem name
         * We do all these different calls so that the product supports any version of the license server
         */

        String libitemName = "untangle-libitem-" + nodeName;
        String urlStr  = licenseUrl + "?action=startTrial&uid=" + UvmContextFactory.context().getServerUID() + "&node=" + nodeName + "&appliance=" + UvmContextFactory.context().isAppliance();
        String urlStr2 = licenseUrl + "?action=startTrial&uid=" + UvmContextFactory.context().getServerUID() + "&libitem=" + libitemName + "&appliance=" + UvmContextFactory.context().isAppliance();

        String oldName = null;
        String urlStr3 = null;
        String urlStr4 = null;
        
        switch ( nodeName ) {
        case License.DIRECTORY_CONNECTOR:
            oldName = License.DIRECTORY_CONNECTOR_OLDNAME; break;
        case License.BANDWIDTH_CONTROL:
            oldName = License.BANDWIDTH_CONTROL_OLDNAME; break;
        case License.CONFIGURATION_BACKUP:
            oldName = License.CONFIGURATION_BACKUP_OLDNAME; break;
        case License.BRANDING_MANAGER:
            oldName = License.BRANDING_MANAGER_OLDNAME; break;
        case License.VIRUS_BLOCKER:
            oldName = License.VIRUS_BLOCKER_OLDNAME; break;
        case License.SPAM_BLOCKER:
            oldName = License.SPAM_BLOCKER_OLDNAME; break;
        case License.WAN_FAILOVER:
            oldName = License.WAN_FAILOVER_OLDNAME; break;
        case License.IPSEC_VPN:
            oldName = License.IPSEC_VPN_OLDNAME; break;
        case License.POLICY_MANAGER:
            oldName = License.POLICY_MANAGER_OLDNAME; break;
        case License.WEB_FILTER:
            oldName = License.WEB_FILTER_OLDNAME; break;
        case License.WAN_BALANCER:
            oldName = License.WAN_BALANCER_OLDNAME; break;
        case License.WEB_CACHE:
            oldName = License.WEB_CACHE_OLDNAME; break;
        case License.APPLICATION_CONTROL:
            oldName = License.APPLICATION_CONTROL_OLDNAME; break;
        case License.SSL_INSPECTOR:
            oldName = License.SSL_INSPECTOR_OLDNAME; break;
        case License.LIVE_SUPPORT:
            oldName = License.LIVE_SUPPORT_OLDNAME; break;
        }
        if ( oldName != null ) {
            String oldLibitemName = "untangle-libitem-" + oldName;
            urlStr3 = licenseUrl + "?action=startTrial&uid=" + UvmContextFactory.context().getServerUID() + "&node=" + oldName + "&appliance=" + UvmContextFactory.context().isAppliance();
            urlStr4 = licenseUrl + "?action=startTrial&uid=" + UvmContextFactory.context().getServerUID() + "&libitem=" + oldLibitemName + "&appliance=" + UvmContextFactory.context().isAppliance();
        }
        
        CloseableHttpClient httpClient = HttpClients.custom().build();
        CloseableHttpResponse response = null;
        HttpGet get;
        URL url;
        
        try {
            logger.info("Requesting Trial: " + urlStr);
            url = new URL(urlStr);
            get = new HttpGet(url.toString());
            response = httpClient.execute(get);
            if ( response != null ) { response.close(); response = null; }
            
            if ( urlStr2 != null ) {
                logger.info("Requesting Trial: " + urlStr2);
                url = new URL(urlStr2);
                get = new HttpGet(url.toString());
                response = httpClient.execute(get);
                if ( response != null ) { response.close(); response = null; }
            }

            if ( urlStr3 != null ) {
                logger.info("Requesting Trial: " + urlStr3);
                url = new URL(urlStr3);
                get = new HttpGet(url.toString());
                response = httpClient.execute(get);
                if ( response != null ) { response.close(); response = null; }
            }

            if ( urlStr4 != null ) {
                logger.info("Requesting Trial: " + urlStr4);
                url = new URL(urlStr4);
                get = new HttpGet(url.toString());
                response = httpClient.execute(get);
                if ( response != null ) { response.close(); response = null; }
            }
        } catch ( java.net.UnknownHostException e ) {
            logger.warn("Exception requesting trial license:" + e.toString());
            throw ( new Exception( "Unable to fetch trial license: DNS lookup failed.", e ) );
        } catch ( java.net.ConnectException e ) {
            logger.warn("Exception requesting trial license:" + e.toString());
            throw ( new Exception( "Unable to fetch trial license: Connection timeout.", e ) );
        } catch ( Exception e ) {
            logger.warn("Exception requesting trial license:" + e.toString());
            throw ( new Exception( "Unable to fetch trial license: " + e.toString(), e ) );
        } finally {
            try { if ( response != null ) response.close(); } catch (Exception e) { logger.warn("close",e); }
            try { httpClient.close(); } catch (Exception e) { logger.warn("close",e); }
        }

        // blocking call because we need the new trial license
        UvmContextFactory.context().licenseManager().reloadLicenses( true );
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
            this.settings = settingsManager.load( LicenseSettings.class, System.getProperty("uvm.conf.dir") + "/licenses/licenses.js" );
        } catch (SettingsManager.SettingsException e) {
            logger.error("Unable to read license file: ", e );
        }

        if (this.settings == null)
            _initializeSettings();

        if (this.settings.getLicenses() != null) {
            Iterator<License> iterator = this.settings.getLicenses().iterator();
            while ( iterator.hasNext() ) {
                License license = iterator.next();

                // remove obsolete names
                if ( isObsoleteApp( license.getName() ) )
                    iterator.remove();

                // recompute metadata - we don't want to use value in file (could have been changed)
                _setValidAndStatus(license);
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

        int numDevices = _getEstimatedNumDevices();
        String uvmVersion = UvmContextFactory.context().version();
        
        logger.info("REFRESH: Checking Revocations...");
        
        try {
            String urlStr = _getLicenseUrl() + "?" + "action=getRevocations" + "&" + "uid=" + UvmContextFactory.context().getServerUID() + "&" + "numDevices=" + numDevices + "&" + "version=" + uvmVersion;
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

        if ( changed )
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
        if ( revoke.getName() == null ) {
            logger.error("Invalid name:" + revoke.getName());
            return false;
        }

        /**
         * See if you find a match in the current licenses
         * If so, remove it
         */
        Iterator<License> itr = this.settings.getLicenses().iterator();
        while ( itr.hasNext() ) {
            License existingLicense = itr.next();
            
            if (revoke.getName().equals(existingLicense.getName())) {
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
            if ( ! isObsoleteApp( lic.getName() ) ) {
                changed |= _insertOrUpdate(lic);
            }
        }

        if ( changed ) 
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
            try {
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

                    /**
                     * If the new one has a different seat amount
                     */
                    if ( license.getSeats() != null && existingLicense.getSeats() == null ) {
                        logger.info("REFRESH: Replacing license " + license + " - number of seats now specified");
                        replaceLicense = true;
                    }
                    if ( license.getSeats() != null && existingLicense.getSeats() != null && license.getSeats() > existingLicense.getSeats() ) {
                        logger.info("REFRESH: Replacing license " + license + " - new one has more seats");
                        replaceLicense = true;
                    }
                
                    if (replaceLicense) {
                        itr.remove();
                        insertNewLicense = true;
                    } else {
                        logger.info("REFRESH: Keeping current license: " + license);
                    }
                }
            } catch (Exception e) {
                logger.warn("Exception processing existing license.",e);
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
        Map<String, License> newMap = new ConcurrentHashMap<String, License>();
        LinkedList<License> newList = new LinkedList<License>();
        License license = null;
        
        if (this.settings != null) {
            for (License lic : this.settings.getLicenses()) {
                try {
                    /**
                     * Create a duplicate - we're about to fill in metadata
                     * But we don't want to mess with the original
                     */
                    license = new License(lic);

                    /**
                     * Complete Meta-data
                     */
                    _setValidAndStatus(license);
            
                    String identifier = license.getName();
                    if ( identifier == null ) {
                        logger.warn("Ignoring license with no name: " + license );
                        continue;
                    }
                        
                    License current = newMap.get(identifier);

                    /* current license is newer and better */
                    if ((current != null) && (current.getEnd() > license.getEnd()))
                        continue;

                    logger.info("Adding License: " + license.getName() + " to Map. (valid: " + license.getValid() + ")");
            
                    newMap.put(identifier, license);
                    newList.add(license);
                } catch (Exception e) {
                    logger.warn("Failed to load license: " + license, e);
                }
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

        String input = null;
        if ( license.getKeyVersion() == 1 ) {
            input = license.getKeyVersion() + license.getUID() + license.getName() + license.getType() + license.getStart() + license.getEnd() + "the meaning of life is 42";
        } else if ( license.getKeyVersion() == 3 ) {
            input = license.getKeyVersion() + license.getUID() + license.getName() + license.getType() + license.getStart() + license.getEnd() + nullToEmptyStr(license.getSeats()) + "the meaning of life is 42";
        }
        else {
            // versions v1,v3 are supported. v2 is for ICC
            // any other version is unknown
            license.setStatus("Invalid (Invalid Key Version)");
            return false;
        }
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
        Iterator<License> itr = this.settings.getLicenses().iterator();
        while ( itr.hasNext() ) {
            License license = itr.next();
            _setValidAndStatus(license);
            if ( license.getValid() != null && !license.getValid() ) {
                logger.warn("Removing invalid license from list: " + license);
                itr.remove();
            }
        }


        /**
         * Save the settings
         */
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        try {
            settingsManager.save( System.getProperty("uvm.conf.dir") + "/licenses/licenses.js", newSettings );
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to save settings.",e);
            return;
        }

        /**
         * Change current settings
         */
        this.settings = newSettings;

        /**
         * Licenses are only saved when changed - call license changed hook
         */
        UvmContextFactory.context().hookManager().callCallbacks( HookManager.LICENSE_CHANGE, null );
    }
    
    /**
     * Returns an estimate of # devices on the network
     * This is not meant to be very accurate - it is just an estimate
     */
    private int _getEstimatedNumDevices()
    {
        return UvmContextFactory.context().hostTable().getCurrentActiveSize();
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
    
    private class LicenseSyncTask implements Runnable
    {
        public void run()
        {
            _syncLicensesWithServer();    
        }
    }
    
    private boolean isGPLApp(String identifier)
    {
        switch ( identifier ) {
        case "untangle-node-ad-blocker": return true;
        case "ad-blocker": return true;
        case "untangle-node-virus-blocker-lite": return true;
        case "virus-blocker-lite": return true;
        case "untangle-node-captive-portal": return true;
        case "captive-portal": return true;
        case "untangle-node-firewall": return true;
        case "firewall": return true;
        case "untangle-node-intrusion-prevention": return true;
        case "intrusion-prevention": return true;
        case "untangle-node-openvpn": return true;
        case "openvpn": return true;
        case "untangle-node-phish-blocker": return true;
        case "phish-blocker": return true;
        case "untangle-node-application-control-lite": return true;
        case "application-control-lite": return true;
        case "untangle-node-router": return true;
        case "router": return true;
        case "untangle-node-reports": return true;
        case "reports": return true;
        case "untangle-node-shield": return true;
        case "shield": return true;
        case "untangle-node-spam-blocker-lite": return true;
        case "spam-blocker-lite": return true;
        case "untangle-node-web-monitor": return true;
        case "web-monitor": return true;
        case "untangle-node-license": return true;
        case "license": return true;
        case "untangle-casing-http": return true;
        case "http": return true;
        case "untangle-casing-ftp": return true;
        case "ftp": return true;
        case "untangle-casing-smtp": return true;
        case "smtp": return true;
        default: return false;
        }
    }

    private boolean isObsoleteApp(String identifier)
    {
        if ("untangle-node-kav".equals(identifier)) return true;
        if ("kav".equals(identifier)) return true;
        if ("untangle-node-commtouch".equals(identifier)) return true;
        if ("commtouch".equals(identifier)) return true;
        if ("untangle-node-commtouchav".equals(identifier)) return true;
        if ("commtouchav".equals(identifier)) return true;
        if ("untangle-node-commtouchas".equals(identifier)) return true;
        if ("commtouchas".equals(identifier)) return true;
        return false;
    }
    
    private String getOldIdentifier(String identifier)
    {
        switch (identifier) {
        case License.DIRECTORY_CONNECTOR:
            return License.DIRECTORY_CONNECTOR_OLDNAME;
        case License.BANDWIDTH_CONTROL:
            return License.BANDWIDTH_CONTROL_OLDNAME;
        case License.CONFIGURATION_BACKUP:
            return License.CONFIGURATION_BACKUP_OLDNAME;
        case License.BRANDING_MANAGER:
            return License.BRANDING_MANAGER_OLDNAME;
        case License.VIRUS_BLOCKER:
            return License.VIRUS_BLOCKER_OLDNAME;
        case License.SPAM_BLOCKER:
            return License.SPAM_BLOCKER_OLDNAME;
        case License.WAN_FAILOVER:
            return License.WAN_FAILOVER_OLDNAME;
        case License.IPSEC_VPN:
            return License.IPSEC_VPN_OLDNAME;
        case License.POLICY_MANAGER:
            return License.POLICY_MANAGER_OLDNAME;
        case License.WEB_FILTER:
            return License.WEB_FILTER_OLDNAME;
        case License.WAN_BALANCER:
            return License.WAN_BALANCER_OLDNAME;
        case License.WEB_CACHE:
            return License.WEB_CACHE_OLDNAME;
        case License.APPLICATION_CONTROL:
            return License.APPLICATION_CONTROL_OLDNAME;
        case License.SSL_INSPECTOR:
            return License.SSL_INSPECTOR_OLDNAME;
        case License.LIVE_SUPPORT:
            return License.LIVE_SUPPORT_OLDNAME;
        }            

        return null;
    }

    private String getLongIdentifier(String identifier)
    {
        if ( identifier.contains("untangle-node-"))
            return identifier;
        else
            return identifier.replaceAll("untangle-node-","");
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

    private static String nullToEmptyStr( Object foo )
    {
        if ( foo == null )
            return "";
        else
            return foo.toString();
    }

    private static int getLienencyGift()
    {
        try {
            File giftFile = new File(LIENENCY_GIFT_FILE);
            if (!giftFile.exists())
                return 0;
            
            BufferedReader reader = new BufferedReader(new FileReader(giftFile));
            Integer i = Integer.parseInt(reader.readLine());
            if ( i == null )
                return 0;
            else
                return i;

        } catch (Exception x) {
            logger.warn("Exception",x);
            return 0;
        }
    }
}
