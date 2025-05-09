/**
 * $Id$
 */
package com.untangle.uvm;

import java.io.Serial;

/**
 * Google app configuration
 */
public class GoogleCloudApp extends CloudApp {
    @Serial
    private static final long serialVersionUID = 1L;

    private String appId;

    private String apiKey;

    private String relayServerUrl;

    private String authUri;

    private String tokenUri;

    /**
     * Default constructor
     */
    public GoogleCloudApp() {
        super();
    }

    /**
     * Parameterized constructor (decrypts the encrypted params and initialize their plaintext counterparts)
     * @param appId
     * @param encryptedApiKey
     * @param clientId
     * @param encryptedClientSecret
     * @param scopes
     * @param redirectUri
     * @param relayServerUrl
     */
    public GoogleCloudApp(String appId, String encryptedApiKey, String clientId, String encryptedClientSecret, String scopes, String redirectUri, String relayServerUrl) {
        super(clientId, PasswordUtil.getDecryptPassword(encryptedClientSecret), scopes, redirectUri);
        this.appId = appId;
        this.apiKey = PasswordUtil.getDecryptPassword(encryptedApiKey);
        this.relayServerUrl = relayServerUrl;
    }


    /**
     * Parameterized constructor (decrypts the encrypted params and initialize their plaintext counterparts)
     * @param appId
     * @param encryptedApiKey
     * @param clientId
     * @param encryptedClientSecret
     * @param scopes
     * @param redirectUri
     * @param relayServerUrl
     * @param authUri
     * @param tokenUri
     */
    public GoogleCloudApp(String appId, String encryptedApiKey, String clientId, String encryptedClientSecret, String scopes, String redirectUri, String relayServerUrl, String authUri, String tokenUri) {
        this(appId, encryptedApiKey, clientId, encryptedClientSecret, scopes, redirectUri, relayServerUrl);
        this.authUri = authUri;
        this.tokenUri = tokenUri;
    }

    /**
     * Get appId (Project Number)
     * @return
     */
    public String getAppId() {
        return appId;
    }

    /**
     * Set appId
     * @param appId
     */
    public void setAppId(String appId) {
        this.appId = appId;
    }

    /**
     * Get apiKey
     * @return
     */
    public String getApiKey() {
        return apiKey;
    }

    /**
     * Set apiKey
     * @param apiKey
     */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * Set apiKey by decrypting the param value
     * @param encryptedApiKey
     */
    public void decryptAndSetApiKey(String encryptedApiKey) {
        this.apiKey = PasswordUtil.getDecryptPassword(encryptedApiKey);
    }

    /**
     * Get relay server
     * @return
     */
    public String getRelayServerUrl() {
        return relayServerUrl;
    }

    /**
     * Set relay server
     * @param relayServerUrl
     */
    public void setRelayServerUrl(String relayServerUrl) {
        this.relayServerUrl = relayServerUrl;
    }

    /**
     * Get auth uri
     * @return
     */
    public String getAuthUri() {
        return authUri;
    }

    /**
     * Set auth uri
     * @param authUri
     */
    public void setAuthUri(String authUri) {
        this.authUri = authUri;
    }

    /**
     * get token uri
     * @return
     */
    public String getTokenUri() {
        return tokenUri;
    }

    /**
     * set token uri
     * @param tokenUri
     */
    public void setTokenUri(String tokenUri) {
        this.tokenUri = tokenUri;
    }
}
