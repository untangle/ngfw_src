/**
 * $Id: GoogleManagerImpl.java 41234 2015-09-12 00:47:13Z dmorris $
 */
package com.untangle.uvm;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

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
import org.json.JSONArray;
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
    private static final String DRIVE_FILES_API = "https://www.googleapis.com/drive/v3/files";

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
                // Transform older settings to v1 version
                transformToV1SettingsVersion(readSettings);
            }
            // gracefully refresh the token after every restart so that the token validity and the refresh job interval never goes out of sync
            handleTokenRefresh(readSettings);

            // During restart, set root directoryId onlt if it is not available
            if (StringUtils.isEmpty(readSettings.getGoogleDriveRootDirectoryId()))
                setRootDirectoryId(readSettings);
            //  no need to explicitly call setSettings again since it is done in previous steps

            if (logger.isDebugEnabled())
                logger.debug("Settings: {}", this.settings.toJSONString());
        }

        logger.info("Initialized GoogleManager");
    }

    /**
     * Sets folderId of the selected root directory. Requires google drive to be connected
     * @param readSettings
     */
    private void setRootDirectoryId(GoogleSettings readSettings) {
        if (isGoogleDriveConnected()) {
            // set root folderId, don't create folder since it has to be created by user and selected from UI using Picker
            String rootFolderId = resolveOrCreateFolderPath(readSettings.getGoogleDriveRootDirectory(), false, "root");
            readSettings.setGoogleDriveRootDirectoryId(rootFolderId);
            setSettings(readSettings);
        }
    }

    /**
     * Migrate to v1 settings version.
     * Along with refresh token, settings now holds access token including its validity.
     * @param readSettings
     */
    private void transformToV1SettingsVersion(GoogleSettings readSettings) {
        readSettings.setVersion(1);
        String refreshToken = readSettings.getDriveRefreshToken();
        if (StringUtils.isNotBlank(refreshToken)) {
            // encrypt the refresh token first
            readSettings.setEncryptedDriveRefreshToken(encrypt(refreshToken));
            // no longer need the plaintext refresh token in settings
            readSettings.setDriveRefreshToken(null);
        } else {
            logger.warn("No refresh token found while migrating to v1 settings version");
        }
    }

    /**
     * Executes refresh access token flow, retrieves new access token by using available refresh token.
     * Saves the settings object by calling setSettings
     * @param setObj
     */
    @Override
    public void refreshToken(GoogleSettings setObj) {
        if (setObj == null || StringUtils.isBlank(setObj.getEncryptedDriveRefreshToken())) {
            logger.warn("Refresh token not available. Unable to get new access token.");
            return;
        }
        logger.debug("Refreshing the access token");
        // retrieve access token using the refresh token
        GoogleSettings newTokenObj = getAccessTokenByRefreshToken(PasswordUtil.getDecryptPassword(setObj.getEncryptedDriveRefreshToken()));
        if (newTokenObj != null) {
            copyTokenAttributes(newTokenObj, setObj);
        } else {
            logger.warn("Unable to get access token from the refresh token. Drive reconfiguration would be required");
        }
        setSettings(setObj);
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
     * Determine if google drive is connected by checking access token's validity. Compare tokens validity based on its retrieved time and expiry.
     *
     * @return true if Google drive is configured, false otherwise.
     */
    public boolean isGoogleDriveConnected()
    {
        GoogleSettings setObj = this.getSettings();
        if (setObj == null)
            return false;

        boolean valid = false;
        if (StringUtils.isNotBlank(setObj.getEncryptedDriveAccessToken()))
            valid = isTokenValid(setObj.getAccessTokenIssuedAt(), setObj.getAccessTokenExpiresIn());

        if (valid)
            return true;

        if (StringUtils.isNotBlank(setObj.getEncryptedDriveRefreshToken())) {
            logger.debug("Existing access token is not valid, refreshing it.");
            // token is invalid, try refreshing it
            handleTokenRefresh(setObj);
        } else {
            logger.debug("Refresh token is not present to retrieve a new access token");
            // no way to refresh the token, give up
            return false;
        }

        // check if token is valid now
        return isTokenValid(setObj.getAccessTokenIssuedAt(), setObj.getAccessTokenExpiresIn());
    }

    /**
     * Refreshes the token, sets the new token to input settings object and starts the token refresh job
     * @param settings
     */
    private void handleTokenRefresh(GoogleSettings settings) {
        if (settings != null && StringUtils.isNotBlank(settings.getEncryptedDriveRefreshToken())) {
            refreshToken(settings);
            startTokenRefreshJob(settings.getAccessTokenExpiresIn());
        }
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
     * Checks the token validity by comparing it with the current timestamp
     * Validity = token issued time + token expires in, i.e. tokenIssuedAt (in milliseconds) + (expiresInSec (in seconds) * 1000)
     * If this validity is greater than the current epoch then we conclude that the token is valid
     * @param tokenIssuedAt
     * @param expiresInSec
     * @return
     */
    private boolean isTokenValid(Long tokenIssuedAt, Integer expiresInSec) {
        if (tokenIssuedAt == null || expiresInSec == null) {
            logger.warn("Not enough inputs to check the token validity");
            return false;
        }
        long adjustedTime = tokenIssuedAt + (expiresInSec * 1000);
        long currentTime = Instant.now().toEpochMilli();
        return adjustedTime > currentTime;
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
     * Returns folderId of the app directory. Creates the directory if not present.
     * Since app directory is nested under a parent directory, we retrieve the folderId by passing the relevant parent information
     * @param appDirectory
     * @return
     */
    @Override
    public String getAppSpecificGoogleDriveFolderId(String appDirectory) {
        // for app directories, root directory present in google settings is always the parent.
        return resolveOrCreateFolderPath(appDirectory, true, this.getSettings().getGoogleDriveRootDirectoryId());
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
            logger.info("Authorization URL: {}", builder.toString());
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
            copyTokenAttributes(newTokenObj, currentSettings);
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
     * Copies available token attributes
     * @param from
     * @param to
     */
    private static void copyTokenAttributes(GoogleSettings from, GoogleSettings to) {
        to.setEncryptedDriveAccessToken(from.getEncryptedDriveAccessToken());
        to.setAccessTokenExpiresIn(from.getAccessTokenExpiresIn());
        to.setAccessTokenIssuedAt(from.getAccessTokenIssuedAt());

        // a new refresh token is not returned in case of authorization grant_type=refresh_token
        if (StringUtils.isNotBlank(from.getEncryptedDriveRefreshToken()))
            to.setEncryptedDriveRefreshToken(from.getEncryptedDriveRefreshToken());
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
        logger.debug("Token refresh job started");
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

            logger.debug("Received a new access token");
            tokenObj = new GoogleSettings();
            tokenObj.setEncryptedDriveAccessToken(encrypt(json.getString("access_token")));
            tokenObj.setAccessTokenExpiresIn(json.getInt("expires_in"));
            tokenObj.setAccessTokenIssuedAt(Instant.now().toEpochMilli());
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
            transformToV1SettingsVersion(googleSettings);

        // refresh the token, call setSettings and start refresh job
        handleTokenRefresh(googleSettings);
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
     * Returns decrypted access token present in current settings object
     * @return
     */
    private String getAccessTokenFromSettings() {
        return PasswordUtil.getDecryptPassword(this.getSettings().getEncryptedDriveAccessToken());
    }

    /**
     * Resolve drive folder by returning the folderId. Creates the folder if createFolder is true.
     * Starts finding folderId/creating folders under the input parentId.
     * If folderPath is not available then same input parentId is returned.
     *
     * Returns null if createFolder is false and given folder is not found on drive.
     * @param folderPath
     * @param createFolder
     * @param parentId
     * @return
     */
    private String resolveOrCreateFolderPath(String folderPath, boolean createFolder, String parentId) {
        if (StringUtils.isBlank(folderPath)) {
            logger.info("folderPath is not available, returning parentId={}", parentId);
            return parentId;
        }
        logger.info("Resolving folderId for the folderPath:{} under parentId:{}", folderPath, parentId);
        String[] folders = folderPath.split(SLASH);

        for (String folderName : folders) {
            folderName = folderName.trim();
            // 1. Check if folder exists
            String query = String.format("name='%s' and mimeType='application/vnd.google-apps.folder' and '%s' in parents and trashed=false",
                    folderName, parentId);
            String url = DRIVE_FILES_API + "?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8)
                    + "&fields=files(id,name)";

            String folderId;
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpGet request = new HttpGet(url);
                request.setHeader("Authorization", "Bearer " + getAccessTokenFromSettings());

                String responseBody = httpClient.execute(request, response -> EntityUtils.toString(response.getEntity(), UTF_8));
                JSONArray files = new JSONObject(responseBody).optJSONArray("files");
                if (files != null && files.length() > 0) {
                    folderId = files.getJSONObject(0).getString("id");
                    logger.info("Found folderName:{}, folderId:{}", folderName, folderId);
                    parentId = folderId;
                    // go to next item in the iterator and find its folderId
                    continue;
                }
                if (!createFolder) {
                    logger.warn("Folder {} not found", folderName);
                    return null;
                }
                // 2. Folder doesn't exist, create it
                parentId = createDriveFolder(folderName, parentId);

            } catch (Exception e) {
                logger.warn("Failed to get create drive folder, folderName:{}, parentId:{}", folderName, parentId, e);
            }

        }
        logger.info("Drive folder resolved folderPath:{}, folderId:{}", folderPath, parentId);
        return parentId;
    }

    /**
     * Creates a drive folder (btw google drive refers to files and folders as 'file' entity)
     * Returns the folderId otherwise null if either any input param is missing or folder creation request fails
     * @param folderName name of the folder
     * @param parentId parent under which the folder has to be created, (for root level folders, the parentId is 'root')
     * @return
     */
    private String createDriveFolder(String folderName, String parentId) {

        if (StringUtils.isBlank(folderName) || StringUtils.isBlank(parentId)) {
            logger.warn("Not enough inputs to create drive folder, folderName:{}, parentId:{}", folderName, parentId);
            return null;
        }

        String folderId = null;

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            JSONObject metadata = new JSONObject();
            metadata.put("name", folderName);
            metadata.put("mimeType", "application/vnd.google-apps.folder");
            metadata.put("parents", new JSONArray().put(parentId));

            HttpPost request = new HttpPost(DRIVE_FILES_API);
            request.setHeader("Content-Type", ContentType.APPLICATION_JSON);
            request.setHeader("Authorization", "Bearer " + getAccessTokenFromSettings());
            request.setEntity(new StringEntity(metadata.toString(), ContentType.APPLICATION_JSON));

            String responseBody = httpClient.execute(request, response -> EntityUtils.toString(response.getEntity(), UTF_8));
            JSONObject json = new JSONObject(responseBody);
            if (!json.has("id")) {
                // failed to get id
                throw new Exception(responseBody);
            }
            folderId = json.getString("id");

            logger.info("Drive folder created folderName:{}, folderId:{}, ", folderName, folderId);
        } catch (Exception e) {
            logger.warn("Failed to get create drive folder, folderName:{}, folderId:{}", folderName, folderId, e);
        }
        return folderId;
    }

    /**
     * Token refresh task
     */
    private class RefreshAccessTokenJob implements Runnable {

        /**
         * Google manager reference
         */
        private final GoogleManager manager;

        /**
         * Parameterized constructor
         * @param manager
         */
        public RefreshAccessTokenJob(GoogleManager manager) {
            this.manager = manager;
        }

        /**
         * run task
         */
        public void run() {
            GoogleSettings currentSettings = manager.getSettings();
                if (settings != null) {
                    manager.refreshToken(currentSettings);
                }
            }
        }
}
