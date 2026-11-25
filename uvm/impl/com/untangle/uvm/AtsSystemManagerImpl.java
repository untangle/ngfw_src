package com.untangle.uvm;

import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import org.json.JSONObject;

import com.untangle.uvm.SystemManager.UpgradeFailures;
import com.untangle.uvm.generic.SystemSettingsGeneric;

/**
 * Composed ATS facade; internal delegates are never exported.
 */
public final class AtsSystemManagerImpl implements AtsSystemManager
{
    private final SafeSystemManager safe;
    private final Supplier<SystemManager> delegate;

    /**
     * Create the composed ATS facade.
     * @param safe The normal Safe facade
     * @param delegate The internal delegate
     */
    AtsSystemManagerImpl(SafeSystemManager safe, Supplier<SystemManager> delegate)
    {
        this.safe = Objects.requireNonNull(safe, "safe");
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<UpgradeFailures> canUpgrade()
    {
        return safe.canUpgrade();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String checkDiskHealth()
    {
        return safe.checkDiskHealth();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean downloadUpgrades()
    {
        return safe.downloadUpgrades();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDecryptedPassword(String encryptedPassword)
    {
        return safe.getDecryptedPassword(encryptedPassword);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDeviceTemperatureInfo()
    {
        return safe.getDeviceTemperatureInfo();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONObject getDownloadStatus()
    {
        return safe.getDownloadStatus();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getMilliseconds()
    {
        return safe.getMilliseconds();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SystemSettings getSettings()
    {
        return safe.getSettings();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SystemSettingsGeneric getSystemSettingsV2()
    {
        return safe.getSystemSettingsV2();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTimeZones()
    {
        return safe.getTimeZones();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUvmLogs()
    {
        return safe.getUvmLogs();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSkipDiskCheck()
    {
        return safe.isSkipDiskCheck();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void logCriticalTemperature(String temperatureErrors)
    {
        safe.logCriticalTemperature(temperatureErrors);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void logDiskCheckFailure(String diskCheckErrors)
    {
        safe.logDiskCheckFailure(diskCheckErrors);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSettings(SystemSettings settings)
    {
        safe.setSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSettings(SystemSettings settings, boolean dirtyRadiusFields)
    {
        safe.setSettings(settings, dirtyRadiusFields);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSkipDiskCheck(boolean skipDiskCheck)
    {
        safe.setSkipDiskCheck(skipDiskCheck);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSystemSettingsV2(SystemSettingsGeneric systemSettingsGeneric)
    {
        safe.setSystemSettingsV2(systemSettingsGeneric);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void upgrade()
    {
        safe.upgrade();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean upgradesAvailable()
    {
        return safe.upgradesAvailable();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean upgradesAvailable(boolean forceUpdate)
    {
        return safe.upgradesAvailable(forceUpdate);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getEncryptedPassword(String password)
    {
        return delegate.get().getEncryptedPassword(password);
    }
}
