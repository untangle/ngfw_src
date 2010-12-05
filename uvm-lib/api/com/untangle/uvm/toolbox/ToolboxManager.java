/*
 * $HeadURL: svn://chef/work/src/uvm-lib/api/com/untangle/uvm/toolbox/ToolboxManager.java $
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
 * Manager for the Toolbox, which holds Mackages. A Mackage is all
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
    UpgradeStatus getUpgradeStatus(boolean doUpdate) throws MackageException, InterruptedException;

    /**
     * Returns true if the box can reach updates.untangle.com
     */
    boolean isUpgradeServerAvailable();

    /**
     * All known mackages.
     *
     * @return an array of <code>MackageDesc</code>s.
     */
    MackageDesc[] available();

    /**
     * All installed mackages.
     *
     * @return a <code>MackageDesc[]</code> value
     */
    MackageDesc[] installed();

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
     * @return a <code>MackageDesc[]</code> value
     */
    MackageDesc[] installedVisible();

    /**
     * Mackages available but not installed.
     *
     * @return a <code>MackageDesc[]</code> value
     */
    MackageDesc[] uninstalled();

    /**
     * Mackages installed but not up to date.
     *
     * @return a <code>MackageDesc[]</code> value
     */
    MackageDesc[] upgradable();

    /**
     * Mackages installed with latest version.
     *
     * @return a <code>MackageDesc[]</code> value
     */
    MackageDesc[] upToDate();

    /**
     * Get the MackageDesc for a node.
     *
     * @param name the name of the node.
     * @return the MackageDesc.
     */
    MackageDesc mackageDesc(String name);

    /**
     * Install a Mackage in the Toolbox.
     *
     * @param name the name of the Mackage.
     * @exception MackageInstallException when <code>name</code> cannot
     *     be installed.
     */
    void install(String name) throws MackageInstallException;

    /**
     * Install a Mackage in the Toolbox and instantiate in the Rack.
     *
     * @param name the name of the Mackage.
     * @param p the policy to install
     * @exception MackageInstallException when <code>name</code> cannot
     *     be installed.
     */
    void installAndInstantiate(String name, Policy p) throws MackageInstallException, DeployException;

    /**
     * Remove a Mackage from the toolbox.
     *
     * @param name the name of the Mackage.
     * @exception MackageUninstallException when <code>name</code> cannot
     *    be uninstalled.
     */
    void uninstall(String name) throws MackageUninstallException;

//     /**
//      * Updated the system package cache
//      *
//      * @param millis timeout in milliseconds
//      * @exception MackageException when timeout exceeded or an error occurs
//      */
//     void update(long millis) throws MackageException;

    /**
     * Updated the system package cache (default timeout)
     *
     * @exception MackageException when timeout exceeded or an error occurs
     */
    void update() throws MackageException;

    /**
     * Upgrade the system
     *
     * @exception MackageException when an error occurs
     */
    void upgrade() throws MackageException;

    /**
     * This function sends message to UI to initiate a install
     * The UI is responsible for actually initiating the install
     */
    void requestInstall(String mackageName);

    /**
     * This function sends message to UI to initiate a uninstall
     * The UI is responsible for actually initiating the uninstall
     */
    void requestUninstall(String mackageName);

    /**
     * Register the deployment of a Mackage at a particular URL.
     *
     * @param url location of the Mackage.
     * @throws DeployException if deployment fails.
     */
    void register(String name) throws MackageInstallException;

    /**
     * Register the deployment of a Mackage at a particular URL.
     *
     * @param url location of the Mackage.
     * @throws DeployException if deployment fails.
     */
    void unregister(String mackageName) throws MackageInstallException;

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
