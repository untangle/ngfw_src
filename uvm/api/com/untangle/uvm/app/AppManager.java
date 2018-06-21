/**
 * $Id$
 */
package com.untangle.uvm.app;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.untangle.uvm.app.AppManagerSettings;
import com.untangle.uvm.app.AppSettings;
import com.untangle.uvm.AppsView;

/**
 * Local interface for managing App instances.
 */
public interface AppManager
{
    /**
     * Get the AppManager settings
     */
    AppManagerSettings getSettings();

    /**
     * Get <code>App</code>s of all instantiated apps.
     *
     * @return list of all app ids.
     */
    List<App> appInstances();
    List<App> nodeInstances(); //deprecated version
    List<Long> appInstancesIds();
    List<Long> nodeInstancesIds(); //deprecated version
    
    /**
     * App instances by name.
     *
     * @param name name of the app.
     * @return tids of corresponding apps.
     */
    List<App> appInstances( String name );
    List<App> nodeInstances( String name ); //deprecated version

    /**
     * App instances by policy.
     *
     * @param policy policy of app.
     * @return tids of corresponding apps.
     */
    List<App> appInstances( Integer policyId );
    List<App> nodeInstances( Integer policyId ); //deprecated version
    List<Long> appInstancesIds( Integer policyId );
    List<Long> nodeInstancesIds( Integer policyId ); //deprecated version

    /**
     * App instances by name policy, this gets the apps in the parents to.
     *
     * @param name name of app.
     * @param policy policy of app.
     * @return tids of corresponding apps.
     */
    List<App> appInstances( String name, Integer policyId );
    List<App> nodeInstances( String name, Integer policyId ); //deprecated version

    /**
     * App instances by name policy.
     *
     * @param name name of app.
     * @param policy policy of app.
     * @param parents true to fetch the apps in the parents as well.
     * @return tids of corresponding apps.
     */
    List<App> appInstances( String name, Integer policyId, boolean parents );
    List<App> nodeInstances( String name, Integer policyId, boolean parents ); //deprecated version

    /**
     * Get the <code>App</code> for this appId
     *
     * @param appId of the instance.
     * @return the instance's <code>App</code>.
     */
    App app( Long appId );
    App node( Long appId ); //deprecated version

    /**
     * Get the <code>App</code> for a app instance;
     * if the are more than a app instance for the provided name,
     * the first app instance is returned.
     *
     * @param name of the app.
     * @return the instance's <code>App</code>.
     */
    App app( String name );
    App node( String name ); //deprecated version
    
    /**
     * Create a new app instance under the given policy.  Note
     * that it is an error to specify a non-null policy for a service,
     * or a null policy for a non-service.
     *
     * @param name of the app.
     * @param policy the policy this instance is applied to.
     * @return the App of the instance
     * @exception Exception if the instance cannot be created.
     */
    App instantiate( String name, Integer policyId ) throws Exception;

    /**
     * Create a new app instance under the default policy, or in
     * the null policy if the app is a service.
     *
     * @param name of the app.
     * @return the App of the instance
     * @exception Exception if the instance cannot be created.
     */
    App instantiate( String name ) throws Exception;

    /**
     * Destroy a app instance.
     *
     * @param appId of instance to be destroyed.
     */
    void destroy( Long appId ) throws Exception;

    /**
     * Save the new target state of the specified app
     *
     * @param app instance 
     */
    void saveTargetState( App app, AppSettings.AppState appState );

    /**
     * Get the runtime state for all apps in one call.
     *
     * @return a <code>Map</code> from app ID to AppState for all apps
     */
    Map<Long, AppSettings.AppState> allAppStates();
    Map<Long, AppSettings.AppState> allNodeStates(); //deprecated version
    
    /**
     * Returns true if the given app/app is instantiated in the rack
     * false otherwise
     *
     * Example arg: 'firewall'
     */
    boolean isInstantiated( String appName );
    
    /**
     * Get the appSettings for all apps in one call.
     *
     * @return a <code>Map</code> from app ID to AppSettings for all apps
     */
    Map<Long, AppSettings> getAllAppSettings();
    Map<Long, AppSettings> allNodeSettings(); // deprecated version

    /**
     * Get the appProperties for all installed apps in one call.
     *
     * @return a <code>Map</code> from app ID to AppProperties for all installed apps
     */
    Map<Long, AppProperties> getAllAppPropertiesMap();
    Map<Long, AppProperties> allNodeProperties(); // deprecated version
    
    /**
     * Get the appProperties for all apps in one call.
     *
     * @return a <code>List</code> of AppProperties for all apps
     */
    List<AppProperties> getAllAppProperties();
    List<AppProperties> getAllNodeProperties(); // deprecated version

    /**
     * Get the view of the apps/rack when the specified policy/rack is displayed
     *
     * @param p policy.
     * @return visible apps for this policy.
     */
    AppsView getAppsView( Integer policyId );

    /**
     * Get the appsview for all policies
     *
     * @return visible apps for every policy.
     */
    AppsView[] getAppsViews();

    
}
