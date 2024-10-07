/**
 * $Id$
 */
package com.untangle.uvm;

import org.json.JSONObject;

/**
 * Describe interface <code>GoogleManager</code> here.
 */
public interface GoogleManager
{
    GoogleSettings getSettings();
    void setSettings(GoogleSettings settings);
    public boolean isGoogleDriveConnected();
    public String getAuthorizationUrl( String windowProtocol, String windowLocation );

    GoogleCloudApp getGoogleCloudApp();

    JSONObject exchangeCodeForToken(String code) throws Exception;

    public String provideDriveCode(String code );
    public void disconnectGoogleDrive();
    public void migrateConfiguration( String refreshToken );
}
