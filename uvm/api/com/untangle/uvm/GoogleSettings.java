/**
 * $Id: GoogleSettings.java,v 1.00 2016/04/02 22:31:04 dmorris Exp $
 */
package com.untangle.uvm;

import org.json.JSONObject;
import org.json.JSONString;

/**
 * Settings for Google
 */
@SuppressWarnings({"deprecation", "serial"})
public class GoogleSettings implements java.io.Serializable, JSONString
{
    // TODO set a default value after the next release
    private Integer version;

    @Deprecated(forRemoval = true, since = "18")
    private String driveRefreshToken = null;

    private String googleDriveRootDirectory = "NGFW Backups";

    private String encryptedDriveAccessToken = null;

    private String encryptedDriveRefreshToken = null;

    private Integer accessTokenExpiresIn;

    private Long accessTokenIssuedAt;

    /**
     * Default constructor
     */
    public GoogleSettings() { }

    /**
     * Get encrypted access token
     * @return
     */
    public String getEncryptedDriveAccessToken() {
        return encryptedDriveAccessToken;
    }

    /**
     * Set encrypted access token
     * @param encryptedDriveAccessToken
     */
    public void setEncryptedDriveAccessToken(String encryptedDriveAccessToken) {
        this.encryptedDriveAccessToken = encryptedDriveAccessToken;
    }

    /**
     * Set token expiry (in seconds)
     * @return
     */
    public Integer getAccessTokenExpiresIn() {
        return accessTokenExpiresIn;
    }

    /**
     * Get token expiry (in seconds)
     * @param accessTokenExpiresIn
     */
    public void setAccessTokenExpiresIn(Integer accessTokenExpiresIn) {
        this.accessTokenExpiresIn = accessTokenExpiresIn;
    }

    /**
     * Get encrypted refresh token
     * @return
     */
    public String getEncryptedDriveRefreshToken() {
        return encryptedDriveRefreshToken;
    }

    /**
     * Set encrypted refresh token
     * @param encryptedDriveRefreshToken
     */
    public void setEncryptedDriveRefreshToken(String encryptedDriveRefreshToken) {
        this.encryptedDriveRefreshToken = encryptedDriveRefreshToken;
    }

    public String getDriveRefreshToken() { return driveRefreshToken; }
    public void setDriveRefreshToken(String newValue) { this.driveRefreshToken = newValue; }

    public String getGoogleDriveRootDirectory() {
        return googleDriveRootDirectory;
    }

    public void setGoogleDriveRootDirectory(String googleDriveRootDirectory) {
        this.googleDriveRootDirectory = googleDriveRootDirectory;
    }

    /**
     * Get settings version.
     *
     * @return
     *      Current settings version.
     */
    public Integer getVersion() { return this.version; }
    /**
     * Set setting settings.
     *
     * @param newValue
     *      Set newversion.
     */
    public void setVersion( Integer newValue ) { this.version = newValue; }

    /**
     * Get access token obtained at (epoch in milliseconds)
     * @return
     */
    public Long getAccessTokenIssuedAt() {
        return accessTokenIssuedAt;
    }

    /**
     * Set access token obtained at (epoch in milliseconds)
     * @param accessTokenIssuedAt
     */
    public void setAccessTokenIssuedAt(Long accessTokenIssuedAt) {
        this.accessTokenIssuedAt = accessTokenIssuedAt;
    }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    /**
     * Clears the attributes (root directory will only be cleared if it set empty explicitly)
     */
    public void clear() {
        this.driveRefreshToken = null;
        this.encryptedDriveAccessToken = null;
        this.encryptedDriveRefreshToken = null;
        this.accessTokenExpiresIn = null;
        this.accessTokenIssuedAt = null;
    }
}
