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
package com.untangle.app.license;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.concurrent.ConcurrentHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Iterator;
import java.net.InetAddress;
import java.net.Socket;
import java.net.InetSocketAddress;

import org.apache.log4j.Logger;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.HookManager;
import com.untangle.uvm.HookCallback;
import com.untangle.uvm.app.AppBase;
import com.untangle.uvm.vnet.PipelineConnector;
import com.untangle.uvm.util.Pulse;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.app.License;
import com.untangle.uvm.app.LicenseManager;
import com.untangle.uvm.app.AppManager;
import com.untangle.uvm.app.UserLicenseMessage;
import com.untangle.uvm.UriTranslation;

/**
 * License manager
 */
public class LicenseManagerImpl extends AppBase implements LicenseManager
{
    private static final String LICENSE_URL_PROPERTY = "uvm.license.url";
    private static final String DEFAULT_LICENSE_URL = "https://license.untangle.com/license.php";

    private static final double LIENENCY_PERCENT = 1.25; /* the enforced seat limit is the license seat limit TIMES this value */
    private static final int    LIENENCY_CONSTANT = 5; /* the enforced seat limit is the license seat limit PLUS this value */
    private static final String LIENENCY_GIFT_FILE = System.getProperty("uvm.conf.dir") + "/gift"; /* the file that defines the gift value */
    private static final int    LIENENCY_GIFT = getLienencyGift(); /* and extra lienency constant */
    
    public static final String DIRECTORY_CONNECTOR_OLDNAME = "adconnector";
    public static final String BANDWIDTH_CONTROL_OLDNAME = "bandwidth";
    public static final String CONFIGURATION_BACKUP_OLDNAME = "boxbackup";
    public static final String BRANDING_MANAGER_OLDNAME = "branding";
    public static final String VIRUS_BLOCKER_OLDNAME = "virusblocker";
    public static final String SPAM_BLOCKER_OLDNAME = "spamblocker";
    public static final String WAN_FAILOVER_OLDNAME = "faild";
    public static final String IPSEC_VPN_OLDNAME = "ipsec";
    public static final String POLICY_MANAGER_OLDNAME = "policy";
    public static final String WEB_FILTER_OLDNAME = "sitefilter";
    public static final String WAN_BALANCER_OLDNAME = "splitd";
    public static final String WEB_CACHE_OLDNAME = "webcache";
    public static final String APPLICATION_CONTROL_OLDNAME = "classd";
    public static final String SSL_INSPECTOR_OLDNAME = "https";
    public static final String LIVE_SUPPORT_OLDNAME = "support";

    private static final String NO_LICENSE_SERVER_CONNECTION_MESSAGE = 
        "<strong>" + I18nUtil.marktr("Unable to establish connection to the License Service!") + "</strong> " + 
        I18nUtil.marktr("Installation of apps is disabled. Please ensure connectivity and ") + "<a href=\"/admin\">" + I18nUtil.marktr("try again") + "</a>";
    private static final String CMD_URL = UvmContextFactory.context().getCmdUrl() + "/appliances/add/" + UvmContextFactory.context().getServerUID();
    private static final String NO_COMMAND_CENTER_ACCOUNT = 
        I18nUtil.marktr("This appliance is not connected to a Command Center account. Please")
        + " <a href=\"" + CMD_URL + "\" target=\"_blank\">" + I18nUtil.marktr("connect the appliance to Command Center") + "</a> " 
        + I18nUtil.marktr("to install applications.");

    private static final String EXPIRED = "expired";

    /**
     * update every 4 hours, leaves an hour window
     */
    private static final long TIMER_DELAY = 1000 * 60 * 60 * 4;

    /**
     * check more frequently if no internet at startup
     */
    private static final long TIMER_DELAY_NO_INTERNET = 1000 * 60 * 10;

    private static final Logger logger = Logger.getLogger(LicenseManagerImpl.class);

    private final PipelineConnector[] connectors = new PipelineConnector[] {};

    /**
     * Map from the product name to the latest valid license available for this product
     * This is where the fully evaluated license are stored
     * This map stores the evaluated (validated) licenses
     */
    private ConcurrentHashMap<String, License> licenseMap = new ConcurrentHashMap<>();

    /**
     * A list of all known licenses
     * These are fully evaluated (validated) license
     */
    private List<License> licenseList = new LinkedList<>();

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

    /**
     * Boolean for if connection is good or not
     */
    private boolean licenseServerConnectivity = false;

    /**
     * Boolean set after the initial license check has completed
     */
    private boolean initialLicenseCheck = false;

    /**
     * Boolean set when auto install is running
     */
    private boolean autoInstallRunning = false;

    /**
     * Setup license manager application.
     * 
     *
     * @param appSettings       License manager application settings.
     * @param appProperties     Licese manager application properties
     */
    public LicenseManagerImpl( com.untangle.uvm.app.AppSettings appSettings, com.untangle.uvm.app.AppProperties appProperties )
    {
        super( appSettings, appProperties );

        logger.info("Starting load");

        this.licenseServerConnectivity = false;

        // initialize settings
        this._readLicenseSettings();

        // try the initial license load
        this.reloadLicenses(true);

        /**
         * If the connectivity flag is set after the initial load set the initial check
         * flag and start the pulse at the normal refresh interval. Otherwise start the
         * pulse with the shorter delay so we check more frequently until successful.
         */
        if (getLicenseServerConnectivity()) {
            logger.info("License connectivity established - Starting normal refresh pulse");
            initialLicenseCheck = true;
            pulse = new Pulse("uvm-license", task, TIMER_DELAY);
            pulse.start();
        } else {
            logger.info("License connectivity failed - Starting urgent refresh pulse");
            initialLicenseCheck = false;
            pulse = new Pulse("uvm-license", task, TIMER_DELAY_NO_INTERNET);
            pulse.start();
        }
    }

    /**
     * Pre license manager start.
     * Reload the licenses.
     *
     * @param isPermanentTransition
     *  If true, the app is permenant
     */
    @Override
    protected void postStart( boolean isPermanentTransition )
    {
        logger.debug("postStart()");
    }

    /**
     * Get the pineliene connector(???)
     *
     * @return PipelineConector
     */
    @Override
    protected PipelineConnector[] getConnectors()
    {
        return this.connectors;
    }
    
    /**
     * Reload all of the licenses from the file system.
     *
     * @param
     *     blocking If true, block the current context until we're finished.  Otherwise, launch a new non-blocking thread.
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
                    /**
                     * Launch the license synchronize routine.
                     */
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

    /**
     * From the existing license map, return the first matching string identifier (e.g.,"virus")
     * 
     * @param  identifier Identifier to find.
     * @param  exactMatch If true, identifier must match license name excactly.  Otherwise, return the first license that begins with the identifier.
     * @return
     *     Matching license.  Return an invalid license if not found.
     */
    @Override
    public final License getLicense(String identifier, boolean exactMatch)
    {
        /**
         * Check the correct name first,
         * If the license exists and is valid use that one
         */
        License license = null;

        /**
         * Look for an existing perfect match
         */
        license = this.licenseMap.get(identifier);
        logger.debug("getLicense(" + identifier + ") = " + license );
        if (license != null)
            return license;

        /**
         * If there is no perfect match,
         * Look for one that the prefix matches
         * example: identifer "virus-blocker" should accept "virus-blocker-cloud"
         */
        if (!exactMatch) {
            for (String name : this.licenseMap.keySet()) {
                if (name.startsWith(identifier)) {
                    license = this.licenseMap.get(name);
                    logger.debug("getLicense(" + identifier + ") = " + license );
                    if (license != null && license.getValid())
                        return license;
                }
            }
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

    /**
     * Return the license for the exactly matching identifier.
     * 
     * @param  identifier Application name to find.
     * @return            License of matching identifier or an invalid license if not found.
     */
    @Override
    public final License getLicense(String identifier)
    {
        return getLicense(identifier, true);
    }

    /**
     * For the specify identifier, determine if license is valid.
     * 
     * @param  identifier Application name to find.
     * @return            true if license is valid, false otherwise.
     */
    @Override
    public final boolean isLicenseValid(String identifier)
    {
        License lic = getLicense(identifier);
        if (lic == null)
            return false;
        Boolean isValid = lic.getValid();
        if (isValid == null)
            return false;
        else
            return isValid;
    }

    /**
     * Get list of all licenses.
     * 
     * @return License List.
     */
    @Override
    public final List<License> getLicenses()
    {
        return this.licenseList;
    }
    
    /**
     * Determine if product has premium license.
     * 
     * @return true if at least one license is valid.
     */
    @Override
    public final boolean hasPremiumLicense()
    {
        return validLicenseCount() > 0;
    }

    /**
     * Determine number of valid licenses.
     * 
     * @return count of valid licenses.
     */
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
    
    /**
     * Return the lowest valid license seat.
     * 
     * @return Number of seats.
     */
    @Override
    public int getSeatLimit( )
    {
        return getSeatLimit( true );
    }

    /**
     * Calculate the lowest license seat for valid subscriptions.
     * 
     * @param  lienency If non-zero seats, calculate seats based on a value higher than actual.  Otherwise, use strict seat value. 
     * @return          Calculated seat number.
     */
    @Override
    public int getSeatLimit( boolean lienency )
    {
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

    /**
     * For the specified applicaton, attempt to get a trial license.
     * 
     * @param  appName   Application name to request.
     * @throws Exception Throw excepton based on inability or general errors conecting license server.
     */
    @Override
    public void requestTrialLicense( String appName ) throws Exception
    {
        if ( appName == null ) {
            logger.warn("Invalid name: " + appName);
            return;
        }
        // if already have a valid license, just return
        if ( UvmContextFactory.context().licenseManager().isLicenseValid( appName ) ) {
            logger.warn("Already have a valid license for: " + appName);
            return;
        }

        // if on restricted license, disallow starting trials
        if (UvmContextFactory.context().licenseManager().isRestricted()) {
            logger.warn("Restricted license, not creating trial for: " + appName);
            return;
        }
        
        String licenseUrl = System.getProperty( "uvm.license.url" );
        if ( licenseUrl == null )
            licenseUrl = "https://license.untangle.com/license.php";
        licenseUrl = UvmContextFactory.context().uriManager().getUri(licenseUrl);


        /**
         * The API specifies libitem, however libitems no longer exist
         * First we try with the actual name, then the libitem time
         * Then we try with the old app name (if it has an old name), then we try with the old libitem name
         * We do all these different calls so that the product supports any version of the license server
         */
        String libitemName = "untangle-libitem-" + appName;
        String urlStr  = licenseUrl + "?action=startTrial" + "&node=" + appName + "&" + getServerParams();
        String urlStr2 = licenseUrl + "?action=startTrial" + "&libitem=" + libitemName + "&" + getServerParams();

        String oldName = null;
        String urlStr3 = null;
        String urlStr4 = null;
        
        switch ( appName ) {
        case License.DIRECTORY_CONNECTOR:
            oldName = DIRECTORY_CONNECTOR_OLDNAME; break;
        case License.BANDWIDTH_CONTROL:
            oldName = BANDWIDTH_CONTROL_OLDNAME; break;
        case License.CONFIGURATION_BACKUP:
            oldName = CONFIGURATION_BACKUP_OLDNAME; break;
        case License.BRANDING_MANAGER:
            oldName = BRANDING_MANAGER_OLDNAME; break;
        case License.VIRUS_BLOCKER:
            oldName = VIRUS_BLOCKER_OLDNAME; break;
        case License.SPAM_BLOCKER:
            oldName = SPAM_BLOCKER_OLDNAME; break;
        case License.WAN_FAILOVER:
            oldName = WAN_FAILOVER_OLDNAME; break;
        case License.IPSEC_VPN:
            oldName = IPSEC_VPN_OLDNAME; break;
        case License.POLICY_MANAGER:
            oldName = POLICY_MANAGER_OLDNAME; break;
        case License.WEB_FILTER:
            oldName = WEB_FILTER_OLDNAME; break;
        case License.WAN_BALANCER:
            oldName = WAN_BALANCER_OLDNAME; break;
        case License.WEB_CACHE:
            oldName = WEB_CACHE_OLDNAME; break;
        case License.APPLICATION_CONTROL:
            oldName = APPLICATION_CONTROL_OLDNAME; break;
        case License.SSL_INSPECTOR:
            oldName = SSL_INSPECTOR_OLDNAME; break;
        case License.LIVE_SUPPORT:
            oldName = LIVE_SUPPORT_OLDNAME; break;
        }
        if ( oldName != null ) {
            String oldLibitemName = "untangle-libitem-" + oldName;
            urlStr3 = licenseUrl + "?action=startTrial" + "&node=" + oldName + "&" + getServerParams();
            urlStr4 = licenseUrl + "?action=startTrial" + "&libitem=" + oldLibitemName + "&" + getServerParams();
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

    /**
     * Not used.  Use methods in uvm class.
     * 
     * @return null
     */
    public Object getSettings()
    {
        /* These are controlled using the methods in the uvm class */
        return null;
    }

    /**
     * Not used.  Use methods in uvm class.
     * @param settings null
     */
    public void setSettings(Object settings)
    {
        /* These are controlled using the methods in the uvm class */
    }

    /**
     * checks and returns if this license instance is restricted
     * 
     * @return boolean indicating restricted status of the license
     */
    public boolean isRestricted() {
        return this.settings.getIsRestricted();
    }

    /**
     * Get UserLicense messages
     * 
     * @return list of messages
     */
    public List<UserLicenseMessage> getUserLicenseMessages() {
        return this.settings.getUserLicenseMessages();
    }

    /**
     * Get is connection to license server is good
     *
     * @return if connection is good
     */
    public boolean getLicenseServerConnectivity() {
        return this.licenseServerConnectivity;
    }


    /**
     * Initialize the settings
     * (By default there are no liceneses)
     */
    private void _initializeSettings()
    {
        logger.info("Initializing Settings...");

        List<License> licenses = new LinkedList<>();
        this.settings = new LicenseSettings(licenses);

        this._saveSettings(this.settings);
    }


    /**
     * Initialize license settings from file or new settings
     */
    private void _readLicenseSettings()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        try {
            this.settings = settingsManager.load( LicenseSettings.class, System.getProperty("uvm.conf.dir") + "/licenses/licenses.js" );
        } catch (SettingsManager.SettingsException e) {
            logger.error("Unable to read license file: ", e );
        }

        // Initialize if we for some reason failed to get licenses.js
        if (this.settings == null) 
            _initializeSettings();
        
        _mapLicenses();
    }

    /**
     * This downloads a list of current licenese from the license server
     *
     * @return if success
     */
    @SuppressWarnings("unchecked") //LinkedList<License> <-> LinkedList
    private synchronized boolean _downloadLicenses()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        boolean restricted = false;
        boolean success = false;

        logger.info("REFRESH: Downloading new Licenses...");

        
        try {
            String urlStr = _getLicenseUrl() + "?" + "action=getLicenses" + "&" + getServerParams();
            logger.info("Downloading: \"" + urlStr + "\"");

            //
            // We need to extract the restricted flag from the json object, so pass the class as JSONObject class
            //
            Object o = settingsManager.loadUrl(JSONObject.class, urlStr);

            JSONObject parse = (JSONObject)o;

            // The list on the json object contains the licenses
            boolean hasList = parse.has("list");
            if(hasList) {
                // Clear out our existing list.
                if (this.settings.getLicenses() != null) {
                    this.settings.getLicenses().clear();
                }
                // Get the restricted out, only if it exists in the json
                if(parse.has("restricted")) {
                    boolean restrict = parse.getBoolean("restricted");
                    restricted = restrict;
                }
                
                JSONArray licList = parse.getJSONArray("list");
                List<License> licenses = this.settings.getLicenses();
                for (int i = 0; i < licList.length(); i++) {
                    JSONObject lic = licList.getJSONObject(i);

                    License newLic = new License();
                    //
                    // We are using the JSONObject return here because JAbsorb does not have
                    // any functionality to pull additional properties that do not
                    // link up with it's desired JSON format, and we also want to 
                    // minimize changes on both Cloud and NGFW side (See NGFW-13214)
                    //
                    if(lic.has("name")) {newLic.setName(lic.getString("name"));}
                    if(lic.has("displayName")) {newLic.setDisplayName(lic.getString("displayName"));}
                    if(lic.has("UID")) {newLic.setUID(lic.getString("UID"));}
                    if(lic.has("type")) {newLic.setType(lic.getString("type"));}
                    if(lic.has("start")) {newLic.setStart(lic.getLong("start"));}
                    if(lic.has("end")) {newLic.setEnd(lic.getLong("end"));}
                    if(lic.has("key")) {newLic.setKey(lic.getString("key"));}
                    if(lic.has("keyVersion")) {newLic.setKeyVersion(lic.getInt("keyVersion"));}
                    if((lic.has("seats")) && (!lic.isNull("seats"))) {newLic.setSeats(lic.getInt("seats"));}

                    licenses.add(newLic);
                }
            }
            // get UserLicense messages
            boolean hasMessages = parse.has("userMessages");
            if (hasMessages) {
                JSONArray userMessageList = parse.getJSONArray("userMessages");
                List<UserLicenseMessage> userMessages = this.settings.getUserLicenseMessages();
                for (int i = 0; i < userMessageList.length(); i++) {
                    JSONObject userMessage = userMessageList.getJSONObject(i);

                    UserLicenseMessage newUlm = new UserLicenseMessage();

                    if(userMessage.has("message")) {newUlm.setMessage(userMessage.getString("message"));}
                    if(userMessage.has("closure")) {newUlm.setHasClosure(userMessage.getBoolean("closure"));}
                    if(userMessage.has("type")) {
                        UserLicenseMessage.UserLicenseMessageType type = 
                            UserLicenseMessage.UserLicenseMessageType.valueOf(userMessage.getString("type").toUpperCase());
                        newUlm.setType(type);
                    } 

                    userMessages.add(newUlm);
                }
            }
        } catch (JSONException e) {
            logger.error("Unable to read license file: ", e );
            return success;
        } catch (SettingsManager.SettingsException e) {
            logger.error("Unable to read license file: ", e );
            return success;
        } catch (ClassCastException e) {
            logger.error("downloadLicenses returned unexpected response",e);
            return success;
        }

        success = true;
        //If license restriction changes, we want to save, this allows toggling between restricted/unrestricted based on license server result
        if (settings.getIsRestricted() != restricted) {
            settings.setIsRestricted(restricted);
        }

        logger.info("REFRESH: Downloading new Licenses... done (successful: " + success + ")");

        return success;
    }
    
    /**
     * update the app to License Map
     */
    private synchronized void _mapLicenses()
    {
        /* Create a new map of all of the valid licenses */
        ConcurrentHashMap<String, License> newMap = new ConcurrentHashMap<>();
        LinkedList<License> newList = new LinkedList<>();
        License license = null;

        if (this.settings.getLicenses() != null) {
            Iterator<License> iterator = this.settings.getLicenses().iterator();
            while ( iterator.hasNext() ) {
                try {
                    /**
                     * Complete Meta-data
                     */
                    license = iterator.next();
                    _setValidAndStatus(license);
                    if (license.getValid() != null && !license.getValid()) {
                        logger.warn("Removing invalid license from list: " + license);
                        iterator.remove();
                        continue;
                    }
                    License mappedLicense = new License(license);
                    
                    logger.info("Adding License: " + mappedLicense.getCurrentName() + " to Map. (valid: " + mappedLicense.getValid() + ")");
            
                    String identifier = mappedLicense.getCurrentName();
                    newMap.put(identifier, mappedLicense);
                    newList.add(mappedLicense);
                } catch (Exception e) {
                    logger.warn("Failed to load license: " + license, e);
                }
            }
        }

        this.licenseMap = newMap;
        this.licenseList = newList;
    }

    /**
     * Test license connectivity
     * 
     * @return if can connect to license server
     */
    private boolean _testLicenseConnectivity()
    {
        Socket socket = null;
        UriTranslation licenseUri = UvmContextFactory.context().uriManager().getUriTranslationByHost("license.untangle.com");

        String host = null;
        int port = 0;
        boolean connected = true;

        try {
            socket = new Socket();
            host = licenseUri.getHost() != null ? licenseUri.getHost(): "license.untangle.com";
            port = licenseUri.getPort() != -1 ? licenseUri.getPort() : 443;
            socket.connect(new InetSocketAddress(host, port), 30000);
        } catch (Exception e) {
            logger.error("Can't get to untangle license server");
            connected = false;
        } finally {
            try {
                if (socket != null) socket.close();
            } catch (Exception e) {
            }
        }

        return connected;
    }

    /**
     * Verify the validity of a license
     *
     * @param license License object for a subscription.
     * @return true if license is valid, false otherwise.
     */
    private boolean _isLicenseValid(License license)
    {
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
     *
     * @param data Array of bytes to convert to a hext string.
     * @return String of hex values for the passed byte array.
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
     *
     * @param newSettings LicenseSetttings to save.
     */
    private void _saveSettings(LicenseSettings newSettings)
    {
        if (logger.isDebugEnabled()) {
            Iterator<License> itr = newSettings.getLicenses().iterator();
            while ( itr.hasNext() ) {
                License license = itr.next();
                logger.debug("License being saved: " + license + " : " + license.getValid());
            }
        }

        /**
         * Save the settings
         */
        logger.debug("Running save settings\n");
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

        UvmContextFactory.context().hookManager().callCallbacks( HookManager.LICENSE_CHANGE, 1 );

    }
    
    /**
     * Returns an estimate of # devices on the network
     * This is not meant to be very accurate - it is just an estimate
     *
     * @return Number of estimated devices on the network.
     */
    private int _getEstimatedNumDevices()
    {
        return UvmContextFactory.context().hostTable().getCurrentActiveSize();
    }

    /**
     * Returns the url for the license server API
     *
     * @return license agreement.
     */
    private String _getLicenseUrl()
    {
        String urlStr = UvmContextFactory.context().uriManager().getUri(System.getProperty(LICENSE_URL_PROPERTY));
        
        if (urlStr == null)
            urlStr = UvmContextFactory.context().uriManager().getUri(DEFAULT_LICENSE_URL);

        return urlStr;
    }

    /**
     * Syncs the license server state with local state
     */
    private void _syncLicensesWithServer()
    {
        logger.info("Reloading licenses..." );

        synchronized (LicenseManagerImpl.this) {
            String oldSettingsString = this.settings.toJSONString(); // need old settings
            this.settings.getUserLicenseMessages().clear(); // clear messages out
            boolean connected = _testLicenseConnectivity();
            boolean downloadSucceeded = false;
            if (connected) {
                licenseServerConnectivity = true;
                downloadSucceeded = _downloadLicenses();
            } 

            // read licenses on failure
            if (!connected || !downloadSucceeded) {
                _readLicenseSettings();
                // initialize proper UserLicense message for connectivity
                if (!connected) {
                    logger.error("No license server connectivity, not downloading licenses");
                    licenseServerConnectivity = false;
                    // check don't have duplicate no license connectivity message
                    boolean noMessage = true;
                    Iterator<UserLicenseMessage> iterator = this.settings.getUserLicenseMessages().iterator();
                    while ( iterator.hasNext() ) {
                        UserLicenseMessage msg = iterator.next();
                        if (msg.getMessage().equals(NO_LICENSE_SERVER_CONNECTION_MESSAGE) && msg.getHasClosure() == false && msg.getType() == UserLicenseMessage.UserLicenseMessageType.ALERT) {
                            noMessage = false;
                        }

                        // remove CC message if exists
                        if (msg.getMessage().equals(NO_COMMAND_CENTER_ACCOUNT) && msg.getHasClosure() == false && msg.getType() == UserLicenseMessage.UserLicenseMessageType.INFO) {
                            iterator.remove();
                        }
                    }

                    // if no message about connection exists, then add it and save settings
                    if (noMessage) {
                        UserLicenseMessage noLicenseConnection = new UserLicenseMessage(NO_LICENSE_SERVER_CONNECTION_MESSAGE,
                                                                        false,
                                                                        UserLicenseMessage.UserLicenseMessageType.ALERT,
                                                                        false);
                        this.settings.getUserLicenseMessages().add(noLicenseConnection);
                        if (!this.settings.toJSONString().equals(oldSettingsString)) {
                            _saveSettings(this.settings);
                        }
                    }
                }
            } else {
                // download succeeded so show add the CC account message if not registered
                // and not restricted, and then map and save the settings
                if (connected && !UvmContextFactory.context().isRegistered() && !this.isRestricted()) {
                    logger.error("No connection to command center, not downloading licenses");

                    UserLicenseMessage noCCAccount = new UserLicenseMessage(NO_COMMAND_CENTER_ACCOUNT, false, UserLicenseMessage.UserLicenseMessageType.INFO, true);
                    this.settings.getUserLicenseMessages().add(noCCAccount);
                } else {
                    // if the deferred flag is set move it back to the auto install flag
                    AppManager appManager = UvmContextFactory.context().appManager();
                    if (appManager.isAutoInstallDeferredFlag()) {
                        appManager.setAutoInstallAppsFlag(true);
                        appManager.setAutoInstallDeferredFlag(false);
                    }
                }

                _mapLicenses();
                if (!this.settings.toJSONString().equals(oldSettingsString)) {
                    _saveSettings(this.settings);
                }
            }
            _runAppManagerSync();
        }

        logger.info("Reloading licenses... done" );
    }

    /**
     * Run app manager specifics to auto install and shutdown invalid items 
     */
    private void _runAppManagerSync() {
        logger.debug("Syncing to App Manager");
        AppManager appManager = UvmContextFactory.context().appManager();

        // auto install if auto install flag is set, if restricted or wizard is incomplete
        if (appManager.isAutoInstallAppsFlag() || isRestricted() || (!UvmContextFactory.context().isWizardComplete())) {
            logger.debug("Running auto install");
            if (appManager.isRestartingUnloaded() || autoInstallRunning) {
                logger.debug("Setting auto install");
                // don't instantiate apps while other apps are being loaded or already auto installing
                appManager.setAutoInstallAppsFlag(true);
            } else {
                logger.debug("Running auto install directly");
                autoInstallRunning = true;
                appManager.doAutoInstall();
                autoInstallRunning = false;
            }
        }

        if(!appManager.isRestartingUnloaded()) {
            appManager.shutdownAppsWithInvalidLicense();
        }
    }
    
    /**
     * Task to run the synchronoization routine. 
     */
    private class LicenseSyncTask implements Runnable
    {
        /**
         * Launch the license synchronize routine.
         */
        public void run()
        {
            _syncLicensesWithServer();

            /**
             * If the initial check is false and the sync we just called set the connectivity
             * flag, we stop the active pulse which was created using the short retry interval
             * and re-create using the normal update interval.
             */
            if ((initialLicenseCheck == false) && (getLicenseServerConnectivity() == true)) {
                logger.info("License connectivity restored - Switching to normal refresh pulse");
                initialLicenseCheck = true;
                pulse.stop();
                pulse = new Pulse("uvm-license", task, TIMER_DELAY);
                pulse.start();
            }
        }
    }
    
    /**
     * Set a license to valid.
     * 
     * @param license License object to set.
     */
    private void _setValidAndStatus(License license)
    {
        if (_isLicenseValid(license)) {
            license.setValid(Boolean.TRUE);
            license.setStatus("Valid");
        } else {
            license.setValid(Boolean.FALSE);
        }
    }

    /**
     * If passed value is null, return an empty string.  Otherwise return the object's string value.
     * 
     * @param  foo Passed value.
     * @return     String of the value.
     */
    private static String nullToEmptyStr( Object foo )
    {
        if ( foo == null )
            return "";
        else
            return foo.toString();
    }

    /**
     * Get the lienency gift value.
     * 
     * @return Number of lienency seats.
     */
    private static int getLienencyGift()
    {
        BufferedReader reader = null;
        int returnValue = 0;
        try {
            File giftFile = new File(LIENENCY_GIFT_FILE);
            if (!giftFile.exists())
                return 0;
            
            reader = new BufferedReader(new FileReader(giftFile));
            Integer i = Integer.parseInt(reader.readLine());
            if ( i == null )
                returnValue = 0;
            else
                returnValue = i;

        } catch (Exception x) {
            logger.warn("Exception",x);
            returnValue = 0;
        }finally{
            if(reader != null){
                try {
                    reader.close();
                } catch( Exception x ){
                    logger.warn("Exception",x);
                }
            }
        }

        return returnValue;
    }

    /**
     * Get the URL encoded parameters to describe this server
     * @return string
     */
    private String getServerParams()
    {
        int numDevices = _getEstimatedNumDevices();
        String model = UvmContextFactory.context().getApplianceModel();
        String uvmVersion = UvmContextFactory.context().getFullVersion();
        String serialNumber = UvmContextFactory.context().getServerSerialNumber();
        if (model != null) {
            try {
                model = URLEncoder.encode(model,"UTF-8");
            } catch (Exception e) {
                logger.warn("Failed to encode",e);
                model = null;
            }
        }
        return "uid=" + UvmContextFactory.context().getServerUID() +
            "&appliance=" + UvmContextFactory.context().isAppliance() +
            (model != null ? "&appliance-model=" + model : "") + 
            (serialNumber != null ? "&serialNumber=" + serialNumber : "") + 
            "&numDevices=" + numDevices +
            "&version=" + uvmVersion;
    }
}
