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

    /**
     * Default constructor
     */
    public GoogleCloudApp() {
        super();
    }

    /**
     * Parameterized constructor
     * @param appId
     * @param clientId
     * @param clientSecret
     * @param scopes
     * @param redirectUrl
     */
    public GoogleCloudApp(String appId, String clientId, String clientSecret, String scopes, String redirectUrl) {
        super(clientId, clientSecret, scopes, redirectUrl);
        this.appId = appId;
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
}
