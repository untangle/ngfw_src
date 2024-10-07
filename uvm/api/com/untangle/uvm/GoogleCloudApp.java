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

    /**
     * Default constructor
     */
    public GoogleCloudApp() {
        super();
    }

    /**
     * Parameterized constructor
     * @param appId
     * @param apiKey
     * @param clientId
     * @param clientSecret
     * @param scopes
     * @param redirectUrl
     * @param relayServerUrl
     */
    public GoogleCloudApp(String appId, String apiKey, String clientId, String clientSecret, String scopes, String redirectUrl, String relayServerUrl) {
        super(clientId, clientSecret, scopes, redirectUrl);
        this.appId = appId;
        this.apiKey = apiKey;
        this.relayServerUrl = relayServerUrl;
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
}
