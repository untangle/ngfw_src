/**
 * $Id: GoogleManagerImpl.java 41234 2015-09-12 00:47:13Z dmorris $
 */
package com.untangle.uvm;

import java.io.File;

import com.untangle.uvm.util.Pulse;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.json.JSONObject;

import static com.untangle.uvm.util.Constants.DOUBLE_SLASH;
import static com.untangle.uvm.util.Constants.SLASH;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * GoogleManagerImpl provides the API implementation of all RADIUS related functionality
 */
public class GoogleManagerImpl implements GoogleManager
{
    private static final String TOKEN_INFO_URL = "https://www.googleapis.com/oauth2/v3/tokeninfo";
    private static final String GOOGLE_DRIVE_PATH = "/var/lib/google-drive/";
    private static final String UVM_BIN_DIR = "uvm.bin.dir";

    private static final long DEFAULT_ACCESS_TOKEN_EXPIRES_IN_SEC = 3599; // just less than an hour
    private static final long REFRESH_THRESHOLD_IN_SEC = 50;
    private static final String TOKEN_REFRESHER_JOB_NAME = "GoogleDriveTokenRefresher";

    private static String RELAY_SERVER_URL = "https://auth-relay.edge.arista.com";

    private static final String SETTINGS_FILE_NAME = System.getProperty("uvm.settings.dir") + "/untangle-vm/" + "google.js";

    private final Logger logger = LogManager.getLogger(getClass());

    private final SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
    /**
     * This is just a copy of the current settings being used
     */
    private GoogleSettings settings;
    /**
     * This holds drive connector credentials required to handle oauth2 flow
     */
    private GoogleCloudApp cloudOAuth2App;
    private Pulse tokenRefreshJob = null;
    
    /**
     * Initialize Google authenticator.
     */
    protected GoogleManagerImpl()
    {
        GoogleSettings readSettings = null;
        try {
            readSettings = settingsManager.load( GoogleSettings.class, SETTINGS_FILE_NAME);
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to load settings:",e);
        }

        // initialize token refresh job with default interval, but don't start it right away, will start when we actually retrieve an access token
        tokenRefreshJob = new Pulse(TOKEN_REFRESHER_JOB_NAME, new RefreshAccessTokenJob(this), getTokenRefreshJobInterval(DEFAULT_ACCESS_TOKEN_EXPIRES_IN_SEC));

        // initialize drive connector oauth2 app credentials
        initializeGoogleCloudAppInstance();

        /**
         * If there are still no settings, just initialize
         */
        if (readSettings == null) {
            logger.warn("No settings found - Initializing new settings.");
            GoogleSettings defaultSettings = new GoogleSettings();
            defaultSettings.setVersion(1);
            this.setSettings(defaultSettings);
        }
        else {
            logger.debug("Loading Settings...");

            // NGFW-15108 Migrating from drive cli to programmatic drive implementation using REST
            if (readSettings.getVersion() == null) {
                // Transform older settings to v1 version and retrieve new access token based on refresh token
                migrateToV1SettingsVersion(readSettings);
            }
            this.setSettings(readSettings);
            if (logger.isDebugEnabled())
                logger.debug("Settings: {}", this.settings.toJSONString());

            startTokenRefreshJob(readSettings.getAccessTokenExpiresIn());
        }

        logger.info("Initialized GoogleManager");
    }

    /**
     * Migrate to v1 settings version.
     * Major change: Along with refresh token, settings now holds access token including its validity.
     * By using the refresh token we retrieve the access token so that existing drive configuration still works and users don't need to reconfigure the drive.
     * @param readSettings
     */
    private void migrateToV1SettingsVersion(GoogleSettings readSettings) {
        readSettings.setVersion(1);
        String refreshToken = readSettings.getDriveRefreshToken();
        if (StringUtils.isNotBlank(refreshToken)) {
            // encrypt the refresh token first
            readSettings.setEncryptedDriveRefreshToken(encrypt(refreshToken));
            handleTokenRefresh(readSettings);
            // no longer need the plaintext refresh token in settings
            readSettings.setDriveRefreshToken(null);
        } else {
            logger.info("No refresh token found while migrating to v1 settings version");
        }
    }

    /**
     * Executes refresh access token flow, retrieves new access token by using available refresh token.
     * Starts the
     * @param readSettings
     */
    @Override
    public void refreshToken(GoogleSettings readSettings) {
        if (readSettings == null || StringUtils.isBlank(readSettings.getEncryptedDriveRefreshToken())) {
            logger.warn("Refresh token not available. Unable to get new access token.");
            return;
        }
        logger.info("Refreshing the access token");
        // retrieve access token using the refresh token
        GoogleSettings newTokenObj = getAccessTokenByRefreshToken(PasswordUtil.getDecryptPassword(readSettings.getEncryptedDriveRefreshToken()));
        if (newTokenObj != null) {
            // a new refresh token is not returned in case of authorization grant_type=refresh_token
            readSettings.setEncryptedDriveAccessToken(newTokenObj.getEncryptedDriveAccessToken());
            readSettings.setAccessTokenExpiresIn(newTokenObj.getAccessTokenExpiresIn());
        } else {
            logger.warn("Unable to get access token from the refresh token. Drive reconfiguration would be required");
        }
    }

    /**
     * Retrieve new access token using the input refresh token
     * @param refreshToken
     * @return
     */
    private GoogleSettings getAccessTokenByRefreshToken(String refreshToken) {
        // construct request body required to get access token by using refresh token
        final String body = "client_id=" + this.cloudOAuth2App.getClientId() +
                "&client_secret=" + this.cloudOAuth2App.getClientSecret() +
                "&refresh_token=" + refreshToken +
                "&grant_type=refresh_token";

        return getTokensFromGoogle(body);
    }

    /**
     * Get Google Authenticator settings.
     *
     * @return Google authenticator settings
     */
    public GoogleSettings getSettings()
    {
        return this.settings;
    }

    /**
     * Configure Google authenticator settings.
     *
     * @param settings  Google authenticator settings.
     */
    public void setSettings( GoogleSettings settings )
    {
        /**
         * Save the settings
         */
        try {
            settingsManager.save(SETTINGS_FILE_NAME, settings );
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to save settings.",e);
            return;
        }

        this.settings = settings;
    }

    /**
     * Determine if google drive is connected by checking access token's validity. 'drive about' returns 400 bad request if token is invalid
     *
     * @return true if Google drive is configured, false otherwise.
     */
    public boolean isGoogleDriveConnected()
    {
        if (this.getSettings() == null)
            return false;

        boolean valid = false;
        if (StringUtils.isNotBlank(this.getSettings().getEncryptedDriveAccessToken()))
            valid = isTokenValid(PasswordUtil.getDecryptPassword(this.getSettings().getEncryptedDriveAccessToken()));

        if (valid)
            return true;

        if (StringUtils.isNotBlank(this.getSettings().getEncryptedDriveRefreshToken())) {
            logger.debug("Existing access token is not valid, refreshing.");
            // token is invalid, try refreshing it
            handleTokenRefresh(this.getSettings());
            setSettings(this.getSettings());
        } else {
            logger.debug("Refresh token is not present to retrieve a new access token");
            // no way to refresh the token, give up
            return false;
        }

        // check if token is valid now
        return isTokenValid(PasswordUtil.getDecryptPassword(this.getSettings().getEncryptedDriveAccessToken()));
    }

    /**
     * Refreshes the token and starts the token refresh job
     * @param settings
     */
    private void handleTokenRefresh(GoogleSettings settings) {
        refreshToken(settings);
        startTokenRefreshJob(settings.getAccessTokenExpiresIn());
    }

    /**
     * Makes API call to check the access token validity
     * @param accessToken
     * @return
     */
    private boolean isTokenValid(String accessToken) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(TOKEN_INFO_URL + "?access_token=" + accessToken);
            int responseCode = httpClient.execute(request, HttpResponse::getCode);
            logger.debug("Checking access token validity");
            return responseCode == 200;
        } catch (Exception e) {
            logger.warn("Failed to check access token validity", e);
        }
        return false;
    }

    /**
     * Returns app specific google drive path
     * This directory path = GOOGLE_DRIVE_ROOT_DIRECTORY + File.separator + appDirectory
     * returns null if google drive root directory is not available
     * returns only GOOGLE_DRIVE_ROOT_DIRECTORY if appDirectory is blank
     * @param appDirectory app specific subdirectory under the root directory where files are stored
     * @return
     */
    @Override
    public String getAppSpecificGoogleDrivePath(String appDirectory) {
        if (StringUtils.isEmpty(this.settings.getGoogleDriveRootDirectory())) {
            return null;
        }
        if (StringUtils.isBlank(appDirectory)) {
            return this.settings.getGoogleDriveRootDirectory();
        }
        return this.settings.getGoogleDriveRootDirectory() + File.separator + appDirectory;
    }

    /**
     * This returns the URL that the user should visit and click allow for the google connector app to be authorized.
     * Once the user clicks the allow button, they will be redirected to Untangle with the redirect_url. The untangle redirect_url
     * will redirect them to their local server gdrive servlet (the IP is passed in the state variable).
     * The servlet will later call provideDriveCode() with the token
     *
     * @param windowProtocol TCP/IP protocol to use.
     * @param windowLocation domain/hostname
     * @return Built URL
     */
    public String getAuthorizationUrl( String windowProtocol, String windowLocation )
    {
        try {
            URIBuilder builder = new URIBuilder(this.cloudOAuth2App.getAuthUri());
            String state = windowProtocol + DOUBLE_SLASH + windowLocation + SLASH + "gdrive" + SLASH + "gdrive";
            builder.setParameter("client_id", this.cloudOAuth2App.getClientId());
            builder.setParameter("redirect_uri", this.cloudOAuth2App.getRedirectUrl());
            builder.setParameter("response_type", "code");
            builder.setParameter("scope", this.cloudOAuth2App.getScopes());
            builder.setParameter("access_type", "offline");
            builder.setParameter("state", state);
            builder.setParameter("approval_prompt", "force");
            return builder.toString();
        } catch (Exception e) {
            logger.error("Failed to construct authorization URL.",e);
            return null;
        }
    }

    /**
     * Returns app configuration of the google drive connector app
     * @return GoogleCloudApp instance
     */
    @Override
    public GoogleCloudApp getGoogleCloudApp() {
        if (this.cloudOAuth2App == null) {
            initializeGoogleCloudAppInstance();
        }
        return this.cloudOAuth2App;
    }

    /**
     * Initialize google oauth2 app instance
     * This is instance holds all the details required for oauth2 flow.
     */
    private void initializeGoogleCloudAppInstance() {
        try {
            String appId = UvmContextFactory.context().execManager().execOutput(System.getProperty(UVM_BIN_DIR) + "/ut-google-drive-helper.sh appId " + GOOGLE_DRIVE_PATH);
            String encryptedApiKey = UvmContextFactory.context().execManager().execOutput(System.getProperty(UVM_BIN_DIR) + "/ut-google-drive-helper.sh encryptedApiKey " + GOOGLE_DRIVE_PATH);
            String clientId = UvmContextFactory.context().execManager().execOutput(System.getProperty(UVM_BIN_DIR) + "/ut-google-drive-helper.sh clientId " + GOOGLE_DRIVE_PATH);
            String encryptedClientSecret = UvmContextFactory.context().execManager().execOutput(System.getProperty(UVM_BIN_DIR) + "/ut-google-drive-helper.sh encryptedClientSecret " + GOOGLE_DRIVE_PATH);
            String scopes = UvmContextFactory.context().execManager().execOutput(System.getProperty(UVM_BIN_DIR) + "/ut-google-drive-helper.sh scopes " + GOOGLE_DRIVE_PATH);
            String redirectUri = UvmContextFactory.context().execManager().execOutput(System.getProperty(UVM_BIN_DIR) + "/ut-google-drive-helper.sh redirectUri " + GOOGLE_DRIVE_PATH);
            String authUri = UvmContextFactory.context().execManager().execOutput(System.getProperty(UVM_BIN_DIR) + "/ut-google-drive-helper.sh authUri " + GOOGLE_DRIVE_PATH);
            String tokenUri = UvmContextFactory.context().execManager().execOutput(System.getProperty(UVM_BIN_DIR) + "/ut-google-drive-helper.sh tokenUri " + GOOGLE_DRIVE_PATH);

            this.cloudOAuth2App = new GoogleCloudApp(appId, encryptedApiKey, clientId, encryptedClientSecret, scopes, redirectUri, RELAY_SERVER_URL, authUri, tokenUri);
        } catch (Exception ex) {
            logger.warn("Failed to load oauth2 app instance:", ex);
        }
    }

    /**
     * This launches the google drive command line app and provides the code.
     * The google drive app will then fetch and save the refreshToken for future use.
     *
     * This also reads the refershToken and saves it in settings
     *
     * @param code the code to send.
     * @return null on success or the error string
     */
    public String provideDriveCode( String code )
    {
        logger.info("Providing code [{}] to exchange for the access token", code);

        // construct request body required to retrieve new access and refresh tokens
        String body = "code=" + code +
                "&client_id=" + this.cloudOAuth2App.getClientId() +
                "&client_secret=" + this.cloudOAuth2App.getClientSecret() +
                "&redirect_uri=" + this.cloudOAuth2App.getRedirectUrl() +
                "&grant_type=authorization_code";

        GoogleSettings newTokenObj = getTokensFromGoogle(body);
        // should get access and refresh tokens
        if (newTokenObj != null && StringUtils.isNotBlank(newTokenObj.getEncryptedDriveRefreshToken()) && StringUtils.isNotBlank(newTokenObj.getEncryptedDriveAccessToken())) {
            logger.info("Encrypted Refresh Token: {}", newTokenObj.getEncryptedDriveRefreshToken());

            GoogleSettings currentSettings = this.getSettings();

            currentSettings.setEncryptedDriveAccessToken(newTokenObj.getEncryptedDriveAccessToken());
            currentSettings.setAccessTokenExpiresIn(newTokenObj.getAccessTokenExpiresIn());
            currentSettings.setEncryptedDriveRefreshToken(newTokenObj.getEncryptedDriveRefreshToken());
            // reset the root directory value in order to be selected again as per new token (new user)
            currentSettings.setGoogleDriveRootDirectory(null);
            // save the settings along with the new tokens
            setSettings( currentSettings );

            startTokenRefreshJob(currentSettings.getAccessTokenExpiresIn());

        } else {
            logger.warn("Unable to retrieve tokens");
            return "Unable to retrieve tokens";
        }
        return null;
    }

    /**
     * Starts token refresh job
     * @param expiresIn
     */
    private void startTokenRefreshJob(long expiresIn) {
        tokenRefreshJob.stop();
        // Reinitialize pulse instance if token expiry is different from the default expiry time
        if (expiresIn != DEFAULT_ACCESS_TOKEN_EXPIRES_IN_SEC)
            tokenRefreshJob = new Pulse(TOKEN_REFRESHER_JOB_NAME, new RefreshAccessTokenJob(this), getTokenRefreshJobInterval(expiresIn));
        tokenRefreshJob.start();
    }

    /**
     * Get token from google as per input request body
     * @param body
     * @return GoogleSettings object by reading the token API response body
     */
    private GoogleSettings getTokensFromGoogle(String body) {
        GoogleSettings tokenObj = null;
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(this.cloudOAuth2App.getTokenUri());
            request.setHeader("Content-Type", ContentType.APPLICATION_FORM_URLENCODED);
            request.setEntity(new StringEntity(body, ContentType.APPLICATION_FORM_URLENCODED));

            String responseBody = httpClient.execute(request, response -> EntityUtils.toString(response.getEntity(), UTF_8));
            JSONObject json = new JSONObject(responseBody);
            if (!json.has("access_token")) {
                // access token is missing, request must have failed
                throw new Exception(responseBody);
            }

            tokenObj = new GoogleSettings();
            tokenObj.setEncryptedDriveAccessToken(encrypt(json.getString("access_token")));
            tokenObj.setAccessTokenExpiresIn(json.getInt("expires_in"));
            // refresh token is only present when authorization grant_type=authorization_code
            if (json.has("refresh_token"))
                tokenObj.setEncryptedDriveRefreshToken(encrypt(json.getString("refresh_token")));

        } catch (Exception e) {
            logger.warn("Failed to get token", e);
        }
        return tokenObj;
    }

    /**
     * Returns the interval (in milliseconds) at which token refresh job must run.
     * Calculates by subtracting REFRESH_THRESHOLD_IN_SECONDS from the input expiresIn value
     * @param expiresIn
     * @return
     */
    private static long getTokenRefreshJobInterval(long expiresIn) {
        return (expiresIn - REFRESH_THRESHOLD_IN_SEC) * 1000;
    }

    /**
     * Disconnect Google drive by clearing all previous settings
     */
    public void disconnectGoogleDrive()
    {
        GoogleSettings googleSettings = getSettings();
        googleSettings.clear();
        setSettings( googleSettings );

        tokenRefreshJob.stop();
    }

    /**
     * Called by Directory Connector to migrate the existing configuration from
     * there to here now that Google Drive support has moved to the base system
     *
     * @param refreshToken - The refresh token
     */
    public void migrateConfiguration(String refreshToken)
    {
        GoogleSettings googleSettings = getSettings();
        googleSettings.setDriveRefreshToken( refreshToken );
        if (googleSettings.getVersion() == null)
            migrateToV1SettingsVersion(googleSettings);
        setSettings( googleSettings );
    }

    /**
     * Returns the encrypted string by calling PasswordUtil.getEncryptPassword()
     * @param plainText
     * @return
     */
    private String encrypt(String plainText) {
        return PasswordUtil.getEncryptPassword(plainText);
    }


    /**
     * Token refresh record class
     * @param manager
     */
    private record RefreshAccessTokenJob(GoogleManager manager) implements Runnable {

        /**
         * run method
         */
        public void run() {
            GoogleSettings settings = manager.getSettings();
                if (settings != null) {
                    manager.refreshToken(settings);
                    manager.setSettings(settings);
                }
            }
        }
}
