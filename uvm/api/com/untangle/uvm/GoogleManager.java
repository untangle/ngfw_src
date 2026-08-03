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

    public String getAuthorizationUrl(String windowProtocol, String windowLocation ) throws GoogleDriveOperationFailedException;

    public GoogleCloudApp getGoogleCloudApp();

    /**
     * Validate the one-shot OAuth state nonce previously issued by
     * {@link #getAuthorizationUrl}, then (on success) exchange the Google
     * OAuth code for Drive tokens. Both steps run atomically server-side.
     *
     * The nonce is the CSRF gate: single-use, expires after 10 minutes,
     * delivered to the admin's browser only as part of the
     * {@link #getAuthorizationUrl} return value (which an attacker cannot
     * read cross-origin). Any caller of this method without a valid nonce
     * is rejected, including RPC callers.
     *
     * @param nonce  one-shot nonce from the /gdrive callback path
     * @param code   OAuth authorization code returned by Google
     * @return null on success; error message on validation or exchange failure
     */
    public String provideDriveCode(String nonce, String code);

    public void disconnectGoogleDrive();
    public void migrateConfiguration( String refreshToken );

    int uploadToDrive(String filePath, String folderName) throws GoogleDriveOperationFailedException;
}
