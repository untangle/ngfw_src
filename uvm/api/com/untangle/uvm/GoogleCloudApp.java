package com.untangle.uvm;

public class GoogleCloudApp extends CloudApp {

    private String appId;

    public GoogleCloudApp(String appId) {
        this.appId = appId;
    }

    public GoogleCloudApp(String appId, String clientId, String clientSecret, String scopes, String redirectUrl) {
        super(clientId, clientSecret, scopes, redirectUrl);
        this.appId = appId;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }
}
