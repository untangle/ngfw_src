/*
 * $HeadURL: svn://chef/work/src/uvm/api/com/untangle/uvm/toolbox/ToolboxManager.java $
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
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
     * All known mackages.
     *
     * @return an array of <code>PackageDesc</code>s.
     */
    PackageDesc[] available();

    /**
     * All installed mackages.
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
     * All installed mackages, which are visible within the GUI
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

//     /**
//      * Updated the system package cache
//      *
//      * @param millis timeout in milliseconds
//      * @exception PackageException when timeout exceeded or an error occurs
//      */
//     void update(long millis) throws PackageException;

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
