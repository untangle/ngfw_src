/**
 * $Id$
 */
package com.untangle.uvm;

import org.json.JSONObject;
import org.json.JSONString;

import java.io.Serial;


/**
 * Abstract class for cloud app configuration
 */
public abstract class CloudApp implements java.io.Serializable, JSONString {

    @Serial
    private static final long serialVersionUID = 1L;
    private String clientId;
    private String clientSecret;
    private String scopes;
    private String redirectUrl;

    public CloudApp() {
        super();
    }

    public CloudApp(String clientId, String clientSecret, String scopes, String redirectUrl) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.scopes = scopes;
        this.redirectUrl = redirectUrl;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public void decryptAndSetClientSecret(String encryptedClientSecret) {
        this.clientSecret = PasswordUtil.getDecryptPassword(encryptedClientSecret);
    }

    public String getScopes() {
        return scopes;
    }

    public void setScopes(String scopes) {
        this.scopes = scopes;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    @Override
    public String toJSONString() {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
