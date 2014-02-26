/**
 * $Id$
 */
package com.untangle.uvm;

/**
 * the System Manager API
 */
public interface SystemManager
{
    SystemSettings getSettings();

    void setSettings(SystemSettings settings);

    String getPublicUrl();

    boolean downloadUpgrades();

    org.json.JSONObject getDownloadStatus();

    boolean upgradesAvailable();

    boolean upgradesAvailable( boolean forceUpdate );

    void upgrade();
}
