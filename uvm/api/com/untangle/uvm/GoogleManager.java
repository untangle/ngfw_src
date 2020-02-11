/**
 * $Id$
 */
package com.untangle.uvm;

/**
 * Describe interface <code>GoogleManager</code> here.
 */
public interface GoogleManager
{
    GoogleSettings getSettings();
    void setSettings(GoogleSettings settings);
    public boolean isGoogleDriveConnected();
    public String getAuthorizationUrl( String windowProtocol, String windowLocation );
    public String provideDriveCode( String code );
    public void disconnectGoogleDrive();
}
