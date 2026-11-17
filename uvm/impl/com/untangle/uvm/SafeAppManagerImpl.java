/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
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
 * Restricted AppManager facade with original App return values.
 */
public final class SafeAppManagerImpl implements SafeAppManager
{
    private final Supplier<AppManager> delegate;

    /**
     * Create the restricted facade.
     * @param delegate The internal delegate
     */
    SafeAppManagerImpl(Supplier<AppManager> delegate)
    {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * app through the reviewed RPC contract.
     * @param appId The appId argument
     * @return The delegated result
     */
    @Override
    public App app(Long appId)
    {
        return delegate.get().app(appId);
    }

    /**
     * app through the reviewed RPC contract.
     * @param name The name argument
     * @return The delegated result
     */
    @Override
    public App app(String name)
    {
        return delegate.get().app(name);
    }

    /**
     * appInstances through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public List<App> appInstances()
    {
        return delegate.get().appInstances();
    }

    /**
     * appInstancesIds through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public List<Long> appInstancesIds()
    {
        return delegate.get().appInstancesIds();
    }

    /**
     * appInstancesIds through the reviewed RPC contract.
     * @param policyId The policyId argument
     * @return The delegated result
     */
    @Override
    public List<Long> appInstancesIds(Integer policyId)
    {
        return delegate.get().appInstancesIds(policyId);
    }

    /**
     * destroy through the reviewed RPC contract.
     * @param appId The appId argument
     * @throws Exception If the operation fails
     */
    @Override
    public void destroy(Long appId) throws Exception
    {
        delegate.get().destroy(appId);
    }

    /**
     * getAllAppProperties through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public List<AppProperties> getAllAppProperties()
    {
        return delegate.get().getAllAppProperties();
    }

    /**
     * getAllAppSettings through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public Map<Long, AppSettings> getAllAppSettings()
    {
        return delegate.get().getAllAppSettings();
    }

    /**
     * getAppsView through the reviewed RPC contract.
     * @param policyId The policyId argument
     * @return The delegated result
     */
    @Override
    public AppsView getAppsView(Integer policyId)
    {
        return delegate.get().getAppsView(policyId);
    }

    /**
     * getAppsViews through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public AppsView[] getAppsViews()
    {
        return delegate.get().getAppsViews();
    }

    /**
     * instantiate through the reviewed RPC contract.
     * @param name The name argument
     * @return The delegated result
     * @throws Exception If the operation fails
     */
    @Override
    public App instantiate(String name) throws Exception
    {
        return delegate.get().instantiate(name);
    }

    /**
     * instantiate through the reviewed RPC contract.
     * @param name The name argument
     * @param policyId The policyId argument
     * @return The delegated result
     * @throws Exception If the operation fails
     */
    @Override
    public App instantiate(String name, Integer policyId) throws Exception
    {
        return delegate.get().instantiate(name, policyId);
    }

    /**
     * isAutoInstallAppsFlag through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public boolean isAutoInstallAppsFlag()
    {
        return delegate.get().isAutoInstallAppsFlag();
    }
}
