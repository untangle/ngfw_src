/*
 * $Id: AptManager.java,v 1.00 2011/09/24 20:53:51 dmorris Exp $
 */
package com.untangle.uvm.apt;

/**
 * Manager for the Toolbox, which holds Packages. A Package is all
 * data concerning a Node that is not related to any particular
 * node instance.
 */
public interface AptManager
{
    /**
     * Get the view of the rack when the specified rack is the current rack
     *
     * @param p policy.
     * @return visible nodes for this policy.
     */
    RackView getRackView( Long policyId );

    /**
     * Returns the current apt state of the system
     *
     * @param doUpdate will force an apt-get update before returning the state
     * @return UgradeStatus the current state
     */
    UpgradeStatus getUpgradeStatus( boolean doUpdate ) throws Exception, InterruptedException;

    /**
     * Returns true if the server can resolve and reach updates.untangle.com
     */
    boolean isUpgradeServerAvailable();

    /**
     * All known packages.
     *
     * @return an array of <code>PackageDesc</code>s.
     */
    PackageDesc[] available();

    /**
     * All installed packages.
     *
     * @return a <code>PackageDesc[]</code> value
     */
    PackageDesc[] installed();

    /**
     * Tests if a package is installed.
     *
     * @param name of the package.
     * @return true if the package is installed.
     */
    boolean isInstalled(String name);

    /**
     * Packages installed but not up to date.
     *
     * @return a <code>PackageDesc[]</code> value
     */
    PackageDesc[] upgradable();

    /**
     * Get the PackageDesc for a node.
     *
     * @param name the name of the node.
     * @return the PackageDesc.
     */
    PackageDesc packageDesc( String name );

    /**
     * Install a Package in the Toolbox.
     *
     * @param name the name of the Package.
     * @exception Exception when <code>name</code> cannot
     *     be installed.
     */
    void install( String name ) throws Exception;

    /**
     * Install a Package in the Toolbox and instantiate in the Rack.
     *
     * @param name the name of the Package.
     * @param p the policy to install
     * @exception Exception when <code>name</code> cannot
     *     be installed.
     */
    void installAndInstantiate(String name, Long policyId) throws Exception;

    /**
     * Updated the system package cache (default timeout)
     *
     * @exception Exception when timeout exceeded or an error occurs
     */
    void update() throws Exception;

    /**
     * Upgrade the system
     *
     * @exception Exception when an error occurs
     */
    void upgrade() throws Exception;

    /**
     * This function sends message to UI to initiate a install
     * The UI is responsible for actually initiating the install
     */
    void requestInstall(String packageName);

    /**
     * Register the deployment of a Package at a particular URL.
     *
     * @param url location of the Package.
     * @throws Exception if deployment fails.
     */
    void register(String name) throws Exception;

    /**
     * Register the deployment of a Package at a particular URL.
     *
     * @param url location of the Package.
     * @throws Exception if deployment fails.
     */
    void unregister(String packageName) throws Exception;
}
