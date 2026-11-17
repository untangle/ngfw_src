/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.uvm;

import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import org.json.JSONObject;

import com.untangle.uvm.generic.SystemSettingsGeneric;

/**
 * Concrete restricted facade; the full delegate is never exported.
 */
public final class SafeSystemManagerImpl implements SafeSystemManager
{
    private final Supplier<SystemManager> delegate;

    /**
     * Create the restricted facade.
     * @param delegate The internal delegate
     */
    SafeSystemManagerImpl(Supplier<SystemManager> delegate)
    {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * canUpgrade through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public Set<SystemManager.UpgradeFailures> canUpgrade()
    {
        return delegate.get().canUpgrade();
    }

    /**
     * checkDiskHealth through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public String checkDiskHealth()
    {
        return delegate.get().checkDiskHealth();
    }

    /**
     * downloadUpgrades through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public boolean downloadUpgrades()
    {
        return delegate.get().downloadUpgrades();
    }

    /**
     * getDecryptedPassword through the reviewed RPC contract.
     * @param encryptedPassword The encryptedPassword argument
     * @return The delegated result
     */
    @Override
    public String getDecryptedPassword(String encryptedPassword)
    {
        return delegate.get().getDecryptedPassword(encryptedPassword);
    }

    /**
     * getDeviceTemperatureInfo through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public String getDeviceTemperatureInfo()
    {
        return ((SystemManagerImpl) delegate.get()).getDeviceTemperatureInfo();
    }

    /**
     * getDownloadStatus through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public JSONObject getDownloadStatus()
    {
        return delegate.get().getDownloadStatus();
    }

    /**
     * getMilliseconds through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public long getMilliseconds()
    {
        return delegate.get().getMilliseconds();
    }

    /**
     * getSettings through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public SystemSettings getSettings()
    {
        return delegate.get().getSettings();
    }

    /**
     * getSystemSettingsV2 through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public SystemSettingsGeneric getSystemSettingsV2()
    {
        return delegate.get().getSystemSettingsV2();
    }

    /**
     * getTimeZones through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public String getTimeZones()
    {
        return delegate.get().getTimeZones();
    }

    /**
     * getUvmLogs through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public String getUvmLogs()
    {
        return delegate.get().getUvmLogs();
    }

    /**
     * isSkipDiskCheck through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public boolean isSkipDiskCheck()
    {
        return delegate.get().isSkipDiskCheck();
    }

    /**
     * logCriticalTemperature through the reviewed RPC contract.
     * @param temperatureErrors The temperatureErrors argument
     */
    @Override
    public void logCriticalTemperature(String temperatureErrors)
    {
        ((SystemManagerImpl) delegate.get()).logCriticalTemperature(temperatureErrors);
    }

    /**
     * logDiskCheckFailure through the reviewed RPC contract.
     * @param diskCheckErrors The diskCheckErrors argument
     */
    @Override
    public void logDiskCheckFailure(String diskCheckErrors)
    {
        delegate.get().logDiskCheckFailure(diskCheckErrors);
    }

    /**
     * setSettings through the reviewed RPC contract.
     * @param settings The settings argument
     */
    @Override
    public void setSettings(SystemSettings settings)
    {
        delegate.get().setSettings(settings);
    }

    /**
     * setSettings through the reviewed RPC contract.
     * @param settings The settings argument
     * @param dirtyRadiusFields The dirtyRadiusFields argument
     */
    @Override
    public void setSettings(SystemSettings settings, boolean dirtyRadiusFields)
    {
        delegate.get().setSettings(settings, dirtyRadiusFields);
    }

    /**
     * setSkipDiskCheck through the reviewed RPC contract.
     * @param skipDiskCheck The skipDiskCheck argument
     */
    @Override
    public void setSkipDiskCheck(boolean skipDiskCheck)
    {
        delegate.get().setSkipDiskCheck(skipDiskCheck);
    }

    /**
     * setSystemSettingsV2 through the reviewed RPC contract.
     * @param systemSettingsGeneric The systemSettingsGeneric argument
     */
    @Override
    public void setSystemSettingsV2(SystemSettingsGeneric systemSettingsGeneric)
    {
        delegate.get().setSystemSettingsV2(systemSettingsGeneric);
    }

    /**
     * upgrade through the reviewed RPC contract.
     */
    @Override
    public void upgrade()
    {
        delegate.get().upgrade();
    }

    /**
     * upgradesAvailable through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public boolean upgradesAvailable()
    {
        return delegate.get().upgradesAvailable();
    }

    /**
     * upgradesAvailable through the reviewed RPC contract.
     * @param forceUpdate The forceUpdate argument
     * @return The delegated result
     */
    @Override
    public boolean upgradesAvailable(boolean forceUpdate)
    {
        return delegate.get().upgradesAvailable(forceUpdate);
    }
}
