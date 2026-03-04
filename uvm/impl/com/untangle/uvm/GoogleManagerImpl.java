/**
 * $Id: GoogleManagerImpl.java 41234 2015-09-12 00:47:13Z dmorris $
 */
package com.untangle.uvm;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import com.untangle.uvm.util.Pulse;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.FormBodyPartBuilder;
import org.apache.hc.client5.http.entity.mime.HttpMultipartMode;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.entity.mime.StringBody;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static com.untangle.uvm.util.Constants.DOUBLE_SLASH;
import static com.untangle.uvm.util.Constants.SLASH;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * GoogleManagerImpl provides the API implementation of all RADIUS related functionality
 */
public class GoogleManagerImpl implements GoogleManager
{
    private static final String GOOGLE_DRIVE_PATH = "/var/lib/google-drive/";
    private static final String UVM_BIN_DIR = "uvm.bin.dir";

    private static final long DEFAULT_ACCESS_TOKEN_EXPIRES_IN_SEC = 3599; // just less than an hour
    private static final long REFRESH_THRESHOLD_IN_SEC = 50;
    private static final String TOKEN_REFRESHER_JOB_NAME = "GoogleDriveTokenRefresher";
    private static final String DRIVE_FILES_API = "https://www.googleapis.com/drive/v3/files";
    private static final String DRIVE_UPLOAD_FILES_API = "https://www.googleapis.com/upload/drive/v3/files";

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
            GoogleSettings defaultSettings = getDefaultGoogleSettings();
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
            //  no need to explicitly call setSettings again since it is done in previous steps

            if (logger.isDebugEnabled() && getSettings() != null)
                logger.debug("Settings: {}", getSettings().toJSONString());
        }

        logger.info("Initialized GoogleManager");
    }

    /**
     * Default google settings
     * @return
     */
    private static GoogleSettings getDefaultGoogleSettings() {
        GoogleSettings defaultSettings = new GoogleSettings();
        defaultSettings.setVersion(1);
        return defaultSettings;
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
        if (settings == null) {
            logger.warn("Input settings object is null. Initializing with default settings");
            settings = getDefaultGoogleSettings();
        }
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
     * Check if previously configured google drive has disconnected abruptly
     * We must have a valid access token to be able to stay connected to google drive.
     * A true validity test includes google's /tokeninfo API call to check if it is really valid.
     * But this API call would be expensive to make frequently. Instead, we can rely on token's expiry time to check validity.
     *  a. If we identify token has expired that means the token refresh job must have failed. Conclude abrupt disconnection.
     *  b. If there are no tokens in the settings then that means either google drive was never configured or was disconnected by the admin.
     *     Don't treat this as abrupt disconnection.
     * @return true for abrupt disconnection, false otherwise
     */
    @Override
    public boolean isGoogleDriveDisconnectedAbruptly() {
        GoogleSettings setObj = this.getSettings();
        if (setObj == null ||
                (StringUtils.isEmpty(setObj.getEncryptedDriveRefreshToken()) && StringUtils.isEmpty(setObj.getEncryptedDriveAccessToken()))) {
            // we don't have tokens, drive was not in a configured state. No abrupt disconnection.
            return false;
        }
        // if the token is no longer valid that means refresh job must have failed, means drive was disconnected
        return !isTokenValid(setObj.getAccessTokenIssuedAt(), setObj.getAccessTokenExpiresIn());
    }

    /**
     * Determine if google drive is connected by checking access token's validity. Compare tokens validity based on its retrieved time and expiry.
     *
     * @return true if Google drive is configured, false otherwise.
     */
    public boolean isGoogleDriveConnected()
    {
        GoogleSettings setObj = this.getSettings();
        if (setObj == null) {
            tokenRefreshJob.stopIfRunning();
            return false;
        }

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
            tokenRefreshJob.stopIfRunning();
            return false;
        }

        // check if token is valid now
        valid = isTokenValid(setObj.getAccessTokenIssuedAt(), setObj.getAccessTokenExpiresIn());
        if (!valid) {
            // still token is invalid, auto refresh job must have failed (or eventually would fail considering token refresh has failed).
            // Stop the refresh job.
            tokenRefreshJob.stopIfRunning();
        }
        return valid;
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
     * returns GOOGLE_DRIVE_ROOT_DIRECTORY if appDirectory is blank
     * In case when appDirectory and GOOGLE_DRIVE_ROOT_DIRECTORY are not available, then return /, which represents root level for google drive
     * This means, files are to be uploaded directly at the root level on google drive.
     * Returns null if settings is null
     * @param appDirectory app specific subdirectory under the root directory where files are stored
     * @return
     */
    @Override
    public String getAppSpecificGoogleDrivePath(String appDirectory) {
        if (getSettings() == null)
            return null;

        if (StringUtils.isBlank(appDirectory)) {
            return getSettings().getGoogleDriveRootDirectory();
        }
        return getSettings().getGoogleDriveRootDirectory() + File.separator + appDirectory;
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
            logger.info("Authorization URL: {}", builder);
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
            if (currentSettings != null) {
                copyTokenAttributes(newTokenObj, currentSettings);
            } else {
                logger.warn("Current settings object is null");
                // current settings is null, new token object becomes our new settings
                currentSettings = newTokenObj;
            }
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
        tokenRefreshJob.stopIfRunning();
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
                logger.warn("Failed to get access token, response:{}", responseBody);
                return null;
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
        if (googleSettings != null)
            googleSettings.clear();
        setSettings( googleSettings );

        tokenRefreshJob.stopIfRunning();
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

        if (googleSettings == null) {
            // in case if null settings, initialize settings with default and set version to null so as to trigger following migration flows
            googleSettings = getDefaultGoogleSettings();
            googleSettings.setVersion(null);
        }
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
     * Returns decrypted access token present in current settings object, null if settings object is null
     * @return
     */
    private String getAccessTokenFromSettings() {
        if (this.getSettings() == null) {
            logger.warn("Settings object is null");
            return null;
        }
        return PasswordUtil.getDecryptPassword(this.getSettings().getEncryptedDriveAccessToken());
    }

    /**
     * Resolve drive folder for the input folderPath by returning the folderId.
     * 1. folderPath can be nested directories separated by '/'.
     *    For ex., '/first level/second level/reports'. Here folderId of the last directory (in this case 'reports') is returned.
     * 2. Creates the folder if it is not found on google drive.
     * 3. Starts finding/creating folders under the input parentId.
     * 4. If folderPath=/, then resolved folderId is same as input parentId.
     *    Caller has to ensure the input parentId is 'root' in order to correctly represent '/' as a root directory
     * 5. If input folderPath is not available then same input parentId is returned.
     * @param folderPath
     * @param parentId
     * @return resolved folderId for the last folder name from input folderPath (separated by SLASH)
     * @throws GoogleDriveOperationFailedException if input parentId is missing or drive get/create API fails
     */
    private String resolveOrCreateFolderPath(String folderPath, String parentId) throws GoogleDriveOperationFailedException {
        if (StringUtils.isEmpty(parentId)) {
            throw new GoogleDriveOperationFailedException("No parent available to upload the folder for folderPath=" + folderPath);
        }
        if (StringUtils.isBlank(folderPath)) {
            logger.info("folderPath is not available, returning parentId={}", parentId);
            return parentId;
        }
        logger.info("Resolving folderId for {} under parentId {}", folderPath, parentId);

        // handle directory hierarchy separated by SLASH
        String[] folders = folderPath.split(SLASH);

        for (String folderName : folders) {
            folderName = folderName.trim();
            if (StringUtils.isEmpty(folderName)) {
                // skip to the next item in the iterator
                continue;
            }

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
                    logger.debug("Found folderName {} with folderId {}", folderName, folderId);
                    parentId = folderId;
                    // go to next item in the iterator and find its folderId
                    continue;
                }
                // 2. Folder doesn't exist, create it
                parentId = createDriveFolder(folderName, parentId);
            } catch (Exception e) {
                // folderName does not exist, nor we are able to create it, stop here
                throw new GoogleDriveOperationFailedException("Failed to get drive folderId, folderName=" + folderName + ", parentId=" + parentId, e);
            }

        }
        logger.info("Resolved Drive folderId for folderPath {} is {}", folderPath, parentId);
        return parentId;
    }

    /**
     * Creates a drive folder (google drive refers to files and folders as 'file' entity)
     * Folder is created under input parentId
     *
     * Throws GoogleDriveOperationFailedException if,
     *  1)either folderName or parentId is missing, 2) folder id is not returned after create API, 3) any exception occurred during create API call
     * @param folderName name of the folder
     * @param parentId parent under which the folder has to be created, (for root level folders, the parentId is 'root')
     * @return google's folderId
     * @throws GoogleDriveOperationFailedException
     */
    private String createDriveFolder(String folderName, String parentId) throws GoogleDriveOperationFailedException {

        if (StringUtils.isBlank(folderName) || StringUtils.isBlank(parentId)) {
            throw new GoogleDriveOperationFailedException("Not enough inputs to create drive folder, folderName=" + folderName + ", parentId=" + parentId);
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
                throw new GoogleDriveOperationFailedException("Failed to create drive folder, folderName=" + folderName + ", parentId=" + parentId + ", response="  + responseBody);
            }
            folderId = json.getString("id");

            logger.info("Drive folder created for folderName={}, folderId={}, ", folderName, folderId);
            return folderId;
        } catch (Exception e) {
            throw new GoogleDriveOperationFailedException(e.getMessage(), e.getCause());
        }
    }

    /**
     * Upload the file located at filePath to google drive under the parentFolder.
     * folderId of the parentFolder is resolved before the upload.
     *
     * Returns the exit code of the whole upload operation.
     *
     * Throws GoogleDriveOperationFailedException if,
     *  1) filePath missing, 2) Any JSON exception occurred during file metadata construction, 3) upload API fails
     * @param filePath
     * @param parentFolder
     * @return 0 indicating successful upload, 99 for the failure.
     * @throws GoogleDriveOperationFailedException
     */
    @Override
    public int uploadToDrive(String filePath, String parentFolder) throws GoogleDriveOperationFailedException {
        if (StringUtils.isBlank(filePath)) {
            throw new GoogleDriveOperationFailedException("File path not present, not uploading");
        }
        // resolve folderId for the input parentFolder, this is where the file will be uploaded
        // look for the parentFolder under the selected root directory
        String folderId = resolveOrCreateFolderPath(parentFolder, "root");
        logger.info("Uploading file {} to google drive under parent folder {} with folderId {}", filePath, parentFolder, folderId);

        File file = new File(filePath);
        String url = DRIVE_UPLOAD_FILES_API + "?uploadType=multipart";
        HttpPost request = new HttpPost(url);
        request.setHeader("Authorization", "Bearer " + getAccessTokenFromSettings());

        // File metadata, i.e. file name and the parent folder of the file
        JSONObject metadata = new JSONObject();
        try {
            metadata.put("name", file.getName());
            metadata.put("parents", new JSONArray().put(folderId));
        } catch (JSONException e) {
            throw new GoogleDriveOperationFailedException("JSON Exception", e);
        }

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.STRICT);

        // Construct multipart body
        builder.addPart(
                FormBodyPartBuilder.create()
                        .setName("metadata")
                        .setBody(new StringBody(metadata.toString(), ContentType.APPLICATION_JSON))
                        .build()
        );

        builder.addBinaryBody("file", file, ContentType.APPLICATION_OCTET_STREAM, file.getName());
        request.setEntity(builder.build());

        Integer x = null;
        try {
            x = executePost(request);
        } catch (GoogleDriveOperationFailedException e) {
            throw e;
        } catch (Exception e) {
            throw new GoogleDriveOperationFailedException("Failed to upload file, filePath:" + filePath + ", folderId: " + folderId, e);
        }
        if (x != null) return x;
        return 99;
    }

    /**
     * Makes the HTTP POST request
     * @param request
     * @return 0 for if API returns either 200 or 201 status code
     * @throws ParseException
     * @throws IOException
     * @throws GoogleDriveOperationFailedException if the API status code different from 200 or 201
     */
    private Integer executePost(HttpPost request) throws ParseException, IOException, GoogleDriveOperationFailedException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            try (ClassicHttpResponse response = client.executeOpen(null, request, HttpClientContext.create())) {
                String body = EntityUtils.toString(response.getEntity());
                int code = response.getCode();
                if (code == 200 || code == 201) {
                    if (logger.isDebugEnabled())
                        logger.debug("Upload successful: {}", body);
                    return 0;
                } else {
                    throw new GoogleDriveOperationFailedException(code, body, null);
                }
            }
        }
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
