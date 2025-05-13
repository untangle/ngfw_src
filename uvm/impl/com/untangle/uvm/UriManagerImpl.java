/**
 * $Id$
 */

package com.untangle.uvm;

import java.io.File;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import org.apache.hc.core5.net.URIBuilder;

/**
 * The Manager for system-based url translations
 */
public class UriManagerImpl implements UriManager
{
    private static final Integer SettingsCurrentVersion = 6;

    private static final String URIS_OVERRIDE_FILE_NAME = System.getProperty("uvm.conf.dir") + "/uris_override.js";

    private String SettingsFileName = "";
    private UriManagerSettings settings = null;

    private Map<String,String> UriMap = new HashMap<>();
    private Map<String,UriTranslation> HostUriTranslations = new HashMap<>();

    private final Logger logger = LogManager.getLogger(this.getClass());

    /**
     * Constructor
     */
    protected UriManagerImpl()
    {
        this.SettingsFileName = System.getProperty("uvm.conf.dir") + "/" + "uris.js";
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        UriManagerSettings readSettings = null;

        try {
            readSettings = settingsManager.load(UriManagerSettings.class, this.SettingsFileName);
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to load settings:", e);
        }

        /**
         * If there are still no settings, just initialize
         */
        if (readSettings == null) {
            logger.warn("No settings found - Initializing new settings.");
            this.setSettings(defaultSettings());
        } else {
            updateSettings(readSettings);
            this.settings = readSettings;

            logger.debug("Loading Settings: " + this.settings.toJSONString());
        }
        buildMap();
        this.syncUriSettings();
    }

    /**
     * Get the settings
     *
     * @return The settings
     */
    public UriManagerSettings getSettings()
    {
        return this.settings;
    }

    /**
     * Set the settings
     *
     * @param newSettings
     *        The new settings
     */
    public void setSettings(final UriManagerSettings newSettings)
    {
        /**
         * Save the settings
         */
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        try {
            settingsManager.save(this.SettingsFileName, newSettings);
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to save settings.", e);
            return;
        }

        /**
         * Change current settings
         */
        this.settings = newSettings;
        try {
            logger.debug("New Settings: \n" + new org.json.JSONObject(this.settings).toString(2));
        } catch (Exception e) {
        }
        buildMap();

        /**
         * Now actually sync the settings to the system
         */
        this.syncUriSettings();
    }

    /**
     * Get built URI string based on key uri.
     * @param uri String of url to lookup.
     * @return String of translated url.
     */
    public String getUri(String uri)
    {
        String translatedUri = null;
        synchronized(this.UriMap){
            translatedUri = UriMap.get(uri);
        }
        if(translatedUri == null){
            translatedUri = uri;
        }
        return translatedUri;
    }

    /**
     * Break URI to get host, get URI that matchs host, rebuild URI with path
     * @param uri String of url to lookup.
     * @return String of translated url.
     */
    public String getUriWithPath(String uri)
    {
        String translatedUri = uri;
        URIBuilder uriBuilder = null;
        String host = null;
        try{
            uriBuilder = new URIBuilder(uri);
            host = uriBuilder.getHost();
            logger.warn("got host=" + host);
        }catch(Exception e){
            logger.warn("*** Unable to create URIBuilder", e);
        }
        if( uriBuilder != null && host != null){
            logger.warn("looking for ut=");
            UriTranslation ut = null;
            synchronized(this.UriMap){
                ut = this.HostUriTranslations.get(host);
            }
            if( ut != null){
                logger.warn("found ");
                if(ut.getScheme() != null){
                    uriBuilder.setScheme(ut.getScheme());
                }
                if(ut.getHost() != null){
                    uriBuilder.setHost(ut.getHost());
                }
                if(ut.getPort() != null){
                    uriBuilder.setPort(ut.getPort());
                }
                translatedUri = uriBuilder.toString();
            }
        }

        return translatedUri;
    }

    /**
     * Retrieve URI settings by hostname.
     * @param host of host of uri  to lookup.
     * @return UriTranslation of matching host or null if no match.
     */
    public UriTranslation getUriTranslationByHost(String host)
    {
        UriTranslation ut = null;
        synchronized(this.UriMap){
            ut = this.HostUriTranslations.get(host);
        }
        return ut;
    }

    /**
     * Build cache of built URIs.
     */
    private void buildMap()
    {
        synchronized(this.UriMap){
            this.UriMap = new HashMap<>();
            this.HostUriTranslations = new HashMap<>();
            URIBuilder uriBuilder = null;
            String keyHost = null;
            UriTranslation utExpanded;
            if(settings.getUriTranslations() != null){
                for(UriTranslation ut : settings.getUriTranslations()){
                    utExpanded = new UriTranslation();
                    try{
                        uriBuilder = new URIBuilder(ut.getUri());
                        keyHost = uriBuilder.getHost();
                        utExpanded.setUri(ut.getUri());
                        if(ut.getScheme() != null){
                            uriBuilder.setScheme(ut.getScheme());
                        }
                        utExpanded.setScheme(uriBuilder.getScheme());
                        if(ut.getHost() != null){
                            uriBuilder.setHost(ut.getHost());
                        }
                        utExpanded.setHost(uriBuilder.getHost());
                        if(ut.getPort() != null){
                            uriBuilder.setPort(ut.getPort());
                        }
                        utExpanded.setPort(uriBuilder.getPort());
                        if(ut.getPath() != null){
                            uriBuilder.setPath(ut.getPath());
                        }
                        utExpanded.setPath(uriBuilder.getPath());
                        if(ut.getQuery() != null){
                            uriBuilder.setCustomQuery(ut.getQuery());
                        }
                    }catch(Exception e){
                        logger.warn("*** Unable to create URIBuilder", e);
                    }
                    this.UriMap.put(ut.getUri(), uriBuilder.toString());
                    if(keyHost != null){
                        this.HostUriTranslations.put(keyHost, utExpanded);
                    }
                }
            }
        }
    }

    /**
     * Initialize URI manager settings
     *
     * @return UriManagerSettings with default values.
     */
    private UriManagerSettings defaultSettings()
    {
        UriManagerSettings settings = new UriManagerSettings();
        settings.setUriTranslations(getDefaultUris());
        mergeOverrideSettings(settings);
        return settings;
    }

    /**
     * Get List of Default Uri's
     *
     * @return List<UriTranslation> with default Uris.
     */
    private List<UriTranslation> getDefaultUris() {
        LinkedList<UriTranslation> uriTranslations = new LinkedList<>();

        UriTranslation uriTranslation = new UriTranslation();
        uriTranslation.setUri("https://ids.edge.arista.com/suricatasignatures.tar.gz");
        uriTranslations.add(uriTranslation);

        uriTranslation = new UriTranslation();
        uriTranslation.setUri("https://labs.edge.arista.com/Utility/v1/mac");
        uriTranslations.add(uriTranslation);

        uriTranslation = new UriTranslation();
        uriTranslation.setUri("https://auth-relay.edge.arista.com/callback.php");
        uriTranslations.add(uriTranslation);

        uriTranslation = new UriTranslation();
        uriTranslation.setUri("https://auth.edge.arista.com/v1/CheckTokenAccess");
        uriTranslations.add(uriTranslation);

        uriTranslation = new UriTranslation();
        uriTranslation.setUri("https://telemetry.edge.arista.com/ngfw/v1/infection");
        uriTranslations.add(uriTranslation);

        uriTranslation = new UriTranslation();
        uriTranslation.setUri("http://updates.edge.arista.com/");
        uriTranslations.add(uriTranslation);

        uriTranslation = new UriTranslation();
        uriTranslation.setUri("https://cmd.edge.arista.com/");
        uriTranslations.add(uriTranslation);

        uriTranslation = new UriTranslation();
        uriTranslation.setUri("https://license.edge.arista.com/license.php");
        uriTranslations.add(uriTranslation);

        uriTranslation = new UriTranslation();
        uriTranslation.setUri("https://classify.edge.arista.com/v1/md5s");
        uriTranslations.add(uriTranslation);

        uriTranslation = new UriTranslation();
        uriTranslation.setUri("http://bd.edge.arista.com/");
        uriTranslations.add(uriTranslation);

        uriTranslation = new UriTranslation();
        uriTranslation.setUri("https://boxbackup.edge.arista.com/boxbackup/backup.php");
        uriTranslations.add(uriTranslation);

        uriTranslation = new UriTranslation();
        uriTranslation.setUri("https://boxbackup.edge.arista.com/api/index.php");
        uriTranslations.add(uriTranslation);

        uriTranslation = new UriTranslation();
        uriTranslation.setUri("https://downloads.edge.arista.com/download.php");
        uriTranslations.add(uriTranslation);

        uriTranslation = new UriTranslation();
        uriTranslation.setUri("http://translations.edge.arista.com/");
        uriTranslations.add(uriTranslation);

        uriTranslation = new UriTranslation();
        uriTranslation.setUri("https://queue.edge.arista.com/");
        uriTranslations.add(uriTranslation);

        uriTranslation = new UriTranslation();
        uriTranslation.setUri("https://edge.arista.com/api/v1/appliance/OnSettingsUpdate");
        uriTranslations.add(uriTranslation);

        // On one hand, this is probably better handled as a host, but since this is being released
        // as a date-release for 15.0 and the update is not going to everyone, adding
        // multiple getXHots() will fail for those non-updated units.
        uriTranslation = new UriTranslation();
        uriTranslation.setUri("https://supssh.edge.arista.com/");
        uriTranslations.add(uriTranslation);

        uriTranslation = new UriTranslation();
        uriTranslation.setUri("https://edge.arista.com/api/v1");
        uriTranslations.add(uriTranslation);

        uriTranslation = new UriTranslation();
        uriTranslation.setUri("https://launchpad.edge.arista.com/");
        uriTranslations.add(uriTranslation);

        uriTranslation = new UriTranslation();
        uriTranslation.setUri("https://wiki.edge.arista.com/get.php");
        uriTranslations.add(uriTranslation);

        uriTranslation = new UriTranslation();
        uriTranslation.setUri("https://auth-relay.edge.arista.com");
        uriTranslations.add(uriTranslation);

        return uriTranslations;
    }

    /**
     * Determine if override file exists.
     *
     * @return If override file exists, return true.  False otherwise.
     */
    private boolean overrideExists()
    {
        File overrideUrisFile = new File(URIS_OVERRIDE_FILE_NAME);
        return overrideUrisFile.exists();
    }

    /**
     * Merge override settings (if they exist) into current settings.
     *
     * @param settings UriManagerSettings to merge
     * @return boolean where if settings changed due to merge, true.  Otherwise if no changes, false.
     */
    private boolean mergeOverrideSettings(UriManagerSettings settings)
    {
        boolean updated = false;

        File overrideUrisFile = new File(URIS_OVERRIDE_FILE_NAME);
        if ( overrideExists() ){
            SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
            UriManagerSettings overrideSettings;
            try {
                overrideSettings = settingsManager.load(UriManagerSettings.class, URIS_OVERRIDE_FILE_NAME);
            } catch (SettingsManager.SettingsException e) {
                logger.warn("Failed to load override settings:", e);
                return updated;
            }

            // Only update if different
            if(!overrideSettings.getDnsTestHost().equals(settings.getDnsTestHost())){
                settings.setDnsTestHost(overrideSettings.getDnsTestHost());
                updated = true;
            }
            if(!overrideSettings.getTcpTestHost().equals(settings.getTcpTestHost())){
                settings.setTcpTestHost(overrideSettings.getTcpTestHost());
                updated = true;
            }
            for(UriTranslation overrideUt : overrideSettings.getUriTranslations()){
                for(UriTranslation ut : settings.getUriTranslations()){
                    if (ut.getUri().equals(overrideUt.getUri())) {
                        if(!ut.equals(overrideUt)){
                            // Different URI translation
                            ut.setScheme(overrideUt.getScheme());
                            ut.setHost(overrideUt.getHost());
                            ut.setPath(overrideUt.getPath());
                            ut.setQuery(overrideUt.getQuery());
                            ut.setPort(overrideUt.getPort());
                            updated = true;
                        }
                        break;
                    }
                }
            }
        }

        return updated;
    }

    /**
     * Update settings.
     * @param settings UriManagerSettings to update.
     */
    private void updateSettings(UriManagerSettings settings)
    {
        if(settings.getVersion() >= SettingsCurrentVersion){
            return;
        }

        List<UriTranslation> uriTranslations = getDefaultUris();       

        // Setting default uri's on upgrade as 
        // We need to update domains of all Uri's NGFW-14960 | NGFW-15067
        settings.setUriTranslations(uriTranslations);
        settings.setVersion(SettingsCurrentVersion);
        this.setSettings( settings );
    }

    /**
     * Call sync settings on uri settings.
     */
    private void syncUriSettings(){
        UvmContextFactory.context().syncSettings().run(SettingsFileName);
        UvmContextFactory.context().hookManager().callCallbacksSynchronous( HookManager.URIS_SETTINGS_CHANGE, this.settings);
    }

}
