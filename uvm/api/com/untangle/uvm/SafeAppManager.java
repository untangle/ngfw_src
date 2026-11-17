/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.uvm;

import java.util.List;
import java.util.Map;

import com.untangle.uvm.app.App;
import com.untangle.uvm.app.AppProperties;
import com.untangle.uvm.app.AppSettings;

/**
 * Reviewed callable operations for SafeAppManager.
 */
public interface SafeAppManager
{
    /**
     * app through the reviewed RPC contract.
     * @param appId The appId argument
     * @return The delegated result
     */
    App app(Long appId);

    /**
     * app through the reviewed RPC contract.
     * @param name The name argument
     * @return The delegated result
     */
    App app(String name);

    /**
     * appInstances through the reviewed RPC contract.
     * @return The delegated result
     */
    List<App> appInstances();

    /**
     * appInstancesIds through the reviewed RPC contract.
     * @return The delegated result
     */
    List<Long> appInstancesIds();

    /**
     * appInstancesIds through the reviewed RPC contract.
     * @param policyId The policyId argument
     * @return The delegated result
     */
    List<Long> appInstancesIds(Integer policyId);

    /**
     * destroy through the reviewed RPC contract.
     * @param appId The appId argument
     * @throws Exception If the operation fails
     */
    void destroy(Long appId) throws Exception;

    /**
     * getAllAppProperties through the reviewed RPC contract.
     * @return The delegated result
     */
    List<AppProperties> getAllAppProperties();

    /**
     * getAllAppSettings through the reviewed RPC contract.
     * @return The delegated result
     */
    Map<Long, AppSettings> getAllAppSettings();

    /**
     * getAppsView through the reviewed RPC contract.
     * @param policyId The policyId argument
     * @return The delegated result
     */
    AppsView getAppsView(Integer policyId);

    /**
     * getAppsViews through the reviewed RPC contract.
     * @return The delegated result
     */
    AppsView[] getAppsViews();

    /**
     * instantiate through the reviewed RPC contract.
     * @param name The name argument
     * @return The delegated result
     * @throws Exception If the operation fails
     */
    App instantiate(String name) throws Exception;

    /**
     * instantiate through the reviewed RPC contract.
     * @param name The name argument
     * @param policyId The policyId argument
     * @return The delegated result
     * @throws Exception If the operation fails
     */
    App instantiate(String name, Integer policyId) throws Exception;

    /**
     * isAutoInstallAppsFlag through the reviewed RPC contract.
     * @return The delegated result
     */
    boolean isAutoInstallAppsFlag();
}
