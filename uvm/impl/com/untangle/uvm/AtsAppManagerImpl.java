package com.untangle.uvm;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import com.untangle.uvm.app.App;
import com.untangle.uvm.app.AppManager;
import com.untangle.uvm.app.AppProperties;
import com.untangle.uvm.app.AppSettings;

/**
 * Composed ATS facade; internal delegates are never exported.
 */
public final class AtsAppManagerImpl implements AtsAppManager
{
    private final SafeAppManager safe;
    private final Supplier<AppManager> delegate;

    /**
     * Create the composed ATS facade.
     * @param safe The normal Safe facade
     * @param delegate The internal delegate
     */
    AtsAppManagerImpl(SafeAppManager safe, Supplier<AppManager> delegate)
    {
        this.safe = Objects.requireNonNull(safe, "safe");
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public App app(Long appId)
    {
        return safe.app(appId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public App app(String name)
    {
        return safe.app(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<App> appInstances()
    {
        return safe.appInstances();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Long> appInstancesIds()
    {
        return safe.appInstancesIds();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Long> appInstancesIds(Integer policyId)
    {
        return safe.appInstancesIds(policyId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy(Long appId) throws Exception
    {
        safe.destroy(appId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<AppProperties> getAllAppProperties()
    {
        return safe.getAllAppProperties();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Long, AppSettings> getAllAppSettings()
    {
        return safe.getAllAppSettings();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AppsView getAppsView(Integer policyId)
    {
        return safe.getAppsView(policyId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AppsView[] getAppsViews()
    {
        return safe.getAppsViews();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public App instantiate(String name) throws Exception
    {
        return safe.instantiate(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public App instantiate(String name, Integer policyId) throws Exception
    {
        return safe.instantiate(name, policyId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAutoInstallAppsFlag()
    {
        return safe.isAutoInstallAppsFlag();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isInstantiated(String appName)
    {
        return delegate.get().isInstantiated(appName);
    }
}
