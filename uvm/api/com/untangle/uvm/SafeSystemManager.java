/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.uvm;

import java.util.Set;

import org.json.JSONObject;

import com.untangle.uvm.generic.SystemSettingsGeneric;

/**
 * Reviewed callable operations for SafeSystemManager.
 */
public interface SafeSystemManager
{
    /**
     * canUpgrade through the reviewed RPC contract.
     * @return The delegated result
     */
    Set<SystemManager.UpgradeFailures> canUpgrade();

    /**
     * checkDiskHealth through the reviewed RPC contract.
     * @return The delegated result
     */
    String checkDiskHealth();

    /**
     * downloadUpgrades through the reviewed RPC contract.
     * @return The delegated result
     */
    boolean downloadUpgrades();

    /**
     * getDecryptedPassword through the reviewed RPC contract.
     * @param encryptedPassword The encryptedPassword argument
     * @return The delegated result
     */
    String getDecryptedPassword(String encryptedPassword);

    /**
     * getDeviceTemperatureInfo through the reviewed RPC contract.
     * @return The delegated result
     */
    String getDeviceTemperatureInfo();

    /**
     * getDownloadStatus through the reviewed RPC contract.
     * @return The delegated result
     */
    JSONObject getDownloadStatus();

    /**
     * getMilliseconds through the reviewed RPC contract.
     * @return The delegated result
     */
    long getMilliseconds();

    /**
     * getSettings through the reviewed RPC contract.
     * @return The delegated result
     */
    SystemSettings getSettings();

    /**
     * getSystemSettingsV2 through the reviewed RPC contract.
     * @return The delegated result
     */
    SystemSettingsGeneric getSystemSettingsV2();

    /**
     * getTimeZones through the reviewed RPC contract.
     * @return The delegated result
     */
    String getTimeZones();

    /**
     * getUvmLogs through the reviewed RPC contract.
     * @return The delegated result
     */
    String getUvmLogs();

    /**
     * isSkipDiskCheck through the reviewed RPC contract.
     * @return The delegated result
     */
    boolean isSkipDiskCheck();

    /**
     * logCriticalTemperature through the reviewed RPC contract.
     * @param temperatureErrors The temperatureErrors argument
     */
    void logCriticalTemperature(String temperatureErrors);

    /**
     * logDiskCheckFailure through the reviewed RPC contract.
     * @param diskCheckErrors The diskCheckErrors argument
     */
    void logDiskCheckFailure(String diskCheckErrors);

    /**
     * setSettings through the reviewed RPC contract.
     * @param settings The settings argument
     */
    void setSettings(SystemSettings settings);

    /**
     * setSettings through the reviewed RPC contract.
     * @param settings The settings argument
     * @param dirtyRadiusFields The dirtyRadiusFields argument
     */
    void setSettings(SystemSettings settings, boolean dirtyRadiusFields);

    /**
     * setSkipDiskCheck through the reviewed RPC contract.
     * @param skipDiskCheck The skipDiskCheck argument
     */
    void setSkipDiskCheck(boolean skipDiskCheck);

    /**
     * setSystemSettingsV2 through the reviewed RPC contract.
     * @param systemSettingsGeneric The systemSettingsGeneric argument
     */
    void setSystemSettingsV2(SystemSettingsGeneric systemSettingsGeneric);

    /**
     * upgrade through the reviewed RPC contract.
     */
    void upgrade();

    /**
     * upgradesAvailable through the reviewed RPC contract.
     * @return The delegated result
     */
    boolean upgradesAvailable();

    /**
     * upgradesAvailable through the reviewed RPC contract.
     * @param forceUpdate The forceUpdate argument
     * @return The delegated result
     */
    boolean upgradesAvailable(boolean forceUpdate);
}
