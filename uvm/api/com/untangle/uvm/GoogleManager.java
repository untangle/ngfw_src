/**
 * $Id$
 */
package com.untangle.uvm;

/**
 * Describe interface <code>GoogleManager</code> here.
 */
public interface GoogleManager
{
    void refreshToken(GoogleSettings readSettings);

    GoogleSettings getSettings();
    void setSettings(GoogleSettings settings);

    boolean isGoogleDriveDisconnectedAbruptly();

    public boolean isGoogleDriveConnected();

    String getAppSpecificGoogleDrivePath(String appDirectory);

    public String getAuthorizationUrl(String windowProtocol, String windowLocation );

    public GoogleCloudApp getGoogleCloudApp();

    public String provideDriveCode(String code );
    public void disconnectGoogleDrive();
    public void migrateConfiguration( String refreshToken );

    int uploadToDrive(String filePath, String folderName) throws GoogleDriveOperationFailedException;
}
