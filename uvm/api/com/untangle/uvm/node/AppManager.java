/*
 * $Id$
 */
package com.untangle.uvm.node;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.untangle.uvm.node.AppManagerSettings;
import com.untangle.uvm.node.AppSettings;
import com.untangle.uvm.AppsView;

/**
 * Local interface for managing Node instances.
 */
public interface AppManager
{
    /**
     * Get the AppManager settings
     */
    AppManagerSettings getSettings();

    /**
     * Get <code>Node</code>s of all instantiated nodes.
     *
     * @return list of all node ids.
     */
    List<Node> appInstances();
    List<Node> nodeInstances(); //deprecated version
    List<Long> appInstancesIds();
    List<Long> nodeInstancesIds(); //deprecated version
    
    /**
     * Node instances by name.
     *
     * @param name name of the node.
     * @return tids of corresponding nodes.
     */
    List<Node> appInstances( String name );
    List<Node> nodeInstances( String name ); //deprecated version

    /**
     * Node instances by policy.
     *
     * @param policy policy of node.
     * @return tids of corresponding nodes.
     */
    List<Node> appInstances( Integer policyId );
    List<Node> nodeInstances( Integer policyId ); //deprecated version
    List<Long> appInstancesIds( Integer policyId );
    List<Long> nodeInstancesIds( Integer policyId ); //deprecated version

    /**
     * Node instances by name policy, this gets the nodes in the parents to.
     *
     * @param name name of node.
     * @param policy policy of node.
     * @return tids of corresponding nodes.
     */
    List<Node> appInstances( String name, Integer policyId );
    List<Node> nodeInstances( String name, Integer policyId ); //deprecated version

    /**
     * Node instances by name policy.
     *
     * @param name name of node.
     * @param policy policy of node.
     * @param parents true to fetch the nodes in the parents as well.
     * @return tids of corresponding nodes.
     */
    List<Node> appInstances( String name, Integer policyId, boolean parents );
    List<Node> nodeInstances( String name, Integer policyId, boolean parents ); //deprecated version

    /**
     * Get the <code>Node</code> for this appId
     *
     * @param appId of the instance.
     * @return the instance's <code>Node</code>.
     */
    Node app( Long appId );
    Node node( Long appId ); //deprecated version

    /**
     * Get the <code>Node</code> for a node instance;
     * if the are more than a node instance for the provided name,
     * the first node instance is returned.
     *
     * @param name of the node.
     * @return the instance's <code>Node</code>.
     */
    Node app( String name );
    Node node( String name ); //deprecated version
    
    /**
     * Create a new node instance under the given policy.  Note
     * that it is an error to specify a non-null policy for a service,
     * or a null policy for a non-service.
     *
     * @param name of the node.
     * @param policy the policy this instance is applied to.
     * @return the Node of the instance
     * @exception Exception if the instance cannot be created.
     */
    Node instantiate( String name, Integer policyId ) throws Exception;

    /**
     * Create a new node instance under the default policy, or in
     * the null policy if the node is a service.
     *
     * @param name of the node.
     * @return the Node of the instance
     * @exception Exception if the instance cannot be created.
     */
    Node instantiate( String name ) throws Exception;

    /**
     * Destroy a node instance.
     *
     * @param appId of instance to be destroyed.
     */
    void destroy( Long appId ) throws Exception;

    /**
     * Save the new target state of the specified node
     *
     * @param node instance 
     */
    void saveTargetState( Node node, AppSettings.AppState appState );

    /**
     * Get the runtime state for all nodes in one call.
     *
     * @return a <code>Map</code> from node ID to AppState for all nodes
     */
    Map<Long, AppSettings.AppState> allAppStates();
    Map<Long, AppSettings.AppState> allNodeStates(); //deprecated version
    
    /**
     * Returns true if the given node/app is instantiated in the rack
     * false otherwise
     *
     * Example arg: 'firewall'
     */
    boolean isInstantiated( String appName );
    
    /**
     * Get the appSettings for all nodes in one call.
     *
     * @return a <code>Map</code> from node ID to AppSettings for all nodes
     */
    Map<Long, AppSettings> getAllAppSettings();
    Map<Long, AppSettings> allNodeSettings(); // deprecated version

    /**
     * Get the appProperties for all installed nodes in one call.
     *
     * @return a <code>Map</code> from node ID to AppProperties for all installed nodes
     */
    Map<Long, AppProperties> getAllAppPropertiesMap();
    Map<Long, AppProperties> allNodeProperties(); // deprecated version
    
    /**
     * Get the appProperties for all nodes in one call.
     *
     * @return a <code>List</code> of AppProperties for all nodes
     */
    List<AppProperties> getAllAppProperties();
    List<AppProperties> getAllNodeProperties(); // deprecated version

    /**
     * Get the view of the apps/rack when the specified policy/rack is displayed
     *
     * @param p policy.
     * @return visible nodes for this policy.
     */
    AppsView getAppsView( Integer policyId );

    /**
     * Get the appsview for all policies
     *
     * @return visible nodes for every policy.
     */
    AppsView[] getAppsViews();

    
}
