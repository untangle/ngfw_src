/*
 * $Id: ToolboxManager.java,v 1.00 2011/09/24 20:53:51 dmorris Exp $
 */
package com.untangle.uvm.toolbox;

import com.untangle.uvm.node.DeployException;
import com.untangle.uvm.policy.Policy;

/**
 * Manager for the Toolbox, which holds Packages. A Package is all
 * data concerning a Node that is not related to any particular
 * node instance.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public interface ToolboxManager
{
    /**
     * Get the view of the rack for a policy.
     *
     * @param p policy.
     * @return visible nodes for this policy.
     */
    RackView getRackView(Policy p);

    /**
     * Returns the current apt state of the system
     *
     * @param doUpdate will force an apt-get update before returning the state
     * @return UgradeStatus the current state
     */
    UpgradeStatus getUpgradeStatus(boolean doUpdate) throws PackageException, InterruptedException;

    /**
     * Returns true if the box can reach updates.untangle.com
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
     * All installed packages, which are visible within the GUI
     *
     * @return a <code>PackageDesc[]</code> value
     */
    PackageDesc[] installedVisible();

    /**
     * Packages available but not installed.
     *
     * @return a <code>PackageDesc[]</code> value
     */
    PackageDesc[] uninstalled();

    /**
     * Packages installed but not up to date.
     *
     * @return a <code>PackageDesc[]</code> value
     */
    PackageDesc[] upgradable();

    /**
     * Packages installed with latest version.
     *
     * @return a <code>PackageDesc[]</code> value
     */
    PackageDesc[] upToDate();

    /**
     * Get the PackageDesc for a node.
     *
     * @param name the name of the node.
     * @return the PackageDesc.
     */
    PackageDesc packageDesc(String name);

    /**
     * Install a Package in the Toolbox.
     *
     * @param name the name of the Package.
     * @exception PackageInstallException when <code>name</code> cannot
     *     be installed.
     */
    void install(String name) throws PackageInstallException;

    /**
     * Install a Package in the Toolbox and instantiate in the Rack.
     *
     * @param name the name of the Package.
     * @param p the policy to install
     * @exception PackageInstallException when <code>name</code> cannot
     *     be installed.
     */
    void installAndInstantiate(String name, Policy p) throws PackageInstallException, DeployException;

    /**
     * Remove a Package from the toolbox.
     *
     * @param name the name of the Package.
     * @exception PackageUninstallException when <code>name</code> cannot
     *    be uninstalled.
     */
    void uninstall(String name) throws PackageUninstallException;

    /**
     * Updated the system package cache (default timeout)
     *
     * @exception PackageException when timeout exceeded or an error occurs
     */
    void update() throws PackageException;

    /**
     * Upgrade the system
     *
     * @exception PackageException when an error occurs
     */
    void upgrade() throws PackageException;

    /**
     * This function sends message to UI to initiate a install
     * The UI is responsible for actually initiating the install
     */
    void requestInstall(String packageName);

    /**
     * This function sends message to UI to initiate a uninstall
     * The UI is responsible for actually initiating the uninstall
     */
    void requestUninstall(String packageName);

    /**
     * Register the deployment of a Package at a particular URL.
     *
     * @param url location of the Package.
     * @throws DeployException if deployment fails.
     */
    void register(String name) throws PackageInstallException;

    /**
     * Register the deployment of a Package at a particular URL.
     *
     * @param url location of the Package.
     * @throws DeployException if deployment fails.
     */
    void unregister(String packageName) throws PackageInstallException;

    /**
     * save Upgrade Settings
     */
    void setUpgradeSettings(UpgradeSettings u);

    /**
     * get Upgrade Settings
     *
     * @return 
     */
    UpgradeSettings getUpgradeSettings();
}
