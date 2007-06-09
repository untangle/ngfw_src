/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.uvm.toolbox;

import java.net.URL;
import java.util.List;

import com.untangle.uvm.MessageQueue;
import com.untangle.uvm.node.DeployException;

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
     * Returns install or upgrade events. When install or upgrade is
     * called, a key is returned that allows the client to get a list
     * of events since the client last called getProgress(). The end
     * of events is signaled by an element of type
     * <code>InstallComplete</code>. A call to this method after such
     * an element is returned will result in a
     * <code>RuntimeException</code>.
     *
     * @param key returned from the install or upgrade method.
     * @return list of events since the last call to getProgress().
     */
    List<InstallProgress> getProgress(long key);

    /**
     * Install a Mackage in the Toolbox.
     *
     * @param name the name of the Mackage.
     * @exception MackageInstallException when <code>name</code> cannot
     *     be installed.
     */
    long install(String name) throws MackageInstallException;

    /**
     * Remove a Mackage from the toolbox.
     *
     * @param name the name of the Mackage.
     * @exception MackageUninstallException when <code>name</code> cannot
     *    be uninstalled.
     */
    void uninstall(String name) throws MackageUninstallException;

    void update(long millis) throws MackageException;

    void update() throws MackageException;

    long upgrade() throws MackageException;

    void enable(String mackageName) throws MackageException;

    void disable(String mackageName) throws MackageException;

    void extraName(String mackageName, String extraName);

    void requestInstall(String mackageName);

    MessageQueue<ToolboxMessage> subscribe();

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

    void setUpgradeSettings(UpgradeSettings u);

    UpgradeSettings getUpgradeSettings();
}
