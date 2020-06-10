/**
 * $Id$
 */

package com.untangle.uvm;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import org.apache.http.client.utils.URIBuilder;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;

/**
 * The Manager for system-based url translations
 */
public class UriManagerImpl implements UriManager
{
    private static final Integer SettingsCurrentVersion = 2;

    private String SettingsFileName = "";
    private UriManagerSettings settings = null;

    private Map<String,String> UriMap = new HashMap<>();
    private Map<String,UriTranslation> HostUriTranslations = new HashMap<>();

    private final Logger logger = Logger.getLogger(this.getClass());

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
        UvmContextFactory.context().syncSettings().run(SettingsFileName);
        UvmContextFactory.context().hookManager().callCallbacksSynchronous( HookManager.URIS_SETTINGS_CHANGE, this.settings);
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

        LinkedList<UriTranslation> uriTranslations = new LinkedList<>();

        UriTranslation uriTranslation = new UriTranslation();
        uriTranslation.setUri("https://ids.untangle.com/suricatasignatures{version}.tar.gz");
        uriTranslations.add(uriTranslation);

        uriTranslation = new UriTranslation();
        uriTranslation.setUri("https://labs.untangle.com/Utility/v1/mac");
        uriTranslations.add(uriTranslation);

        uriTranslation = new UriTranslation();
        uriTranslation.setUri("https://auth-relay.untangle.com/callback.php");
        uriTranslations.add(uriTranslation);

        uriTranslation = new UriTranslation();
        uriTranslation.setUri("https://telemetry.untangle.com/ngfw/v1/infection");
        uriTranslations.add(uriTranslation);

        uriTranslation = new UriTranslation();
        uriTranslation.setUri("http://updates.untangle.com/");
        uriTranslations.add(uriTranslation);

        uriTranslation = new UriTranslation();
        uriTranslation.setUri("https://cmd.untangle.com/");
        uriTranslations.add(uriTranslation);

        uriTranslation = new UriTranslation();
        uriTranslation.setUri("https://license.untangle.com/license.php");
        uriTranslations.add(uriTranslation);

        uriTranslation = new UriTranslation();
        uriTranslation.setUri("https://classify.untangle.com/v1/md5s");
        uriTranslations.add(uriTranslation);

        uriTranslation = new UriTranslation();
        uriTranslation.setUri("http://bd.untangle.com/");
        uriTranslations.add(uriTranslation);

        uriTranslation = new UriTranslation();
        uriTranslation.setUri("https://boxbackup.untangle.com/boxbackup/backup.php");
        uriTranslations.add(uriTranslation);

        uriTranslation = new UriTranslation();
        uriTranslation.setUri("https://boxbackup.untangle.com/api/index.php");
        uriTranslations.add(uriTranslation);

        uriTranslation = new UriTranslation();
        uriTranslation.setUri("https://downloads.untangle.com/download.php");
        uriTranslations.add(uriTranslation);

        uriTranslation = new UriTranslation();
        uriTranslation.setUri("http://translations.untangle.com/");
        uriTranslations.add(uriTranslation);

        uriTranslation = new UriTranslation();
        uriTranslation.setUri("https://queue.untangle.com/");
        uriTranslations.add(uriTranslation);

        uriTranslation = new UriTranslation();
        uriTranslation.setUri("https://untangle.com/api/v1/appliance/OnSettingsUpdate");
        uriTranslations.add(uriTranslation);

        // On one hand, this is probably better handled as a host, but since this is being released
        // as a date-release for 15.0 and the update is not going to everyone, adding
        // multiple getXHots() will fail for those non-updated units.
        uriTranslation = new UriTranslation();
        uriTranslation.setUri("https://supssh.untangle.com/");
        uriTranslations.add(uriTranslation);

        uriTranslation = new UriTranslation();
        uriTranslation.setUri("https://sshrelay.untangle.com/");
        uriTranslations.add(uriTranslation);

        settings.setUriTranslations(uriTranslations);

        return settings;
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
        List<UriTranslation> uriTranslations = settings.getUriTranslations();

        UriTranslation uriTranslation = new UriTranslation();
        uriTranslation.setUri("https://boxbackup.untangle.com/api/index.php");
        uriTranslations.add(uriTranslation);

        uriTranslation = new UriTranslation();
        uriTranslation.setUri("http://translations.untangle.com/");
        uriTranslations.add(uriTranslation);

        uriTranslation = new UriTranslation();
        uriTranslation.setUri("https://queue.untangle.com/");
        uriTranslations.add(uriTranslation);

        uriTranslation = new UriTranslation();
        uriTranslation.setUri("https://untangle.com/api/v1/appliance/OnSettingsUpdate");
        uriTranslations.add(uriTranslation);

        uriTranslation = new UriTranslation();
        uriTranslation.setUri("https://supssh.untangle.com/");
        uriTranslations.add(uriTranslation);

        uriTranslation = new UriTranslation();
        uriTranslation.setUri("https://sshrelay.untangle.com/");
        uriTranslations.add(uriTranslation);

        settings.setUriTranslations(uriTranslations);

        settings.setVersion(SettingsCurrentVersion);
        this.setSettings( settings );
    }
}