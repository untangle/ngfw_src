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

package com.untangle.uvm.client;

import java.io.IOException;

import com.untangle.uvm.BrandingManager;
import com.untangle.uvm.ConnectivityTester;
import com.untangle.uvm.NetworkManager;
import com.untangle.uvm.RemoteAppServerManager;
import com.untangle.uvm.ReportingManager;
import com.untangle.uvm.addrbook.AddressBook;
import com.untangle.uvm.logging.LoggingManager;
import com.untangle.uvm.networking.ping.PingManager;
import com.untangle.uvm.policy.PolicyManager;
import com.untangle.uvm.portal.RemotePortalManager;
import com.untangle.uvm.security.AdminManager;
import com.untangle.uvm.toolbox.ToolboxManager;
import com.untangle.uvm.node.RemoteIntfManager;
import com.untangle.uvm.node.RemoteShieldManager;
import com.untangle.uvm.node.NodeManager;
import com.untangle.uvm.user.RemotePhoneBook;

/**
 * Provides an interface to get major UVM components that are
 * accessible a remote client.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public interface UvmRemoteContext
{
    /**
     * Get the <code>ToolboxManager</code> singleton.
     *
     * @return the ToolboxManager.
     */
    ToolboxManager toolboxManager();

    /**
     * Get the <code>NodeManager</code> singleton.
     *
     * @return the NodeManager.
     */
    NodeManager nodeManager();

    /**
     * Get the <code>LoggingManager</code> singleton.
     *
     * @return the LoggingManager.
     */
    LoggingManager loggingManager();

    /**
     * Get the <code>PolicyManager</code> singleton.
     *
     * @return the PolicyManager.
     */
    PolicyManager policyManager();

    /**
     * Get the <code>AdminManager</code> singleton.
     *
     * @return the AdminManager.
     */
    AdminManager adminManager();

    /**
     * Get the <code>RemoteIntfManager</code> singleton.
     *
     * @return the RemoteIntfManager.
     */
    RemoteIntfManager intfManager();

    /**
     * Get the <code>PingManager</code> singleton.
     *
     * @return the PingManager.
     */
    PingManager pingManager();

    /**
     * Get the <code>NetworkManager</code> singleton.
     *
     * @return the NetworkManager.
     */
    NetworkManager networkManager();

    /** Get the <code>RemoteShieldManager</code> singleton.
     *
     * @return the ShieldManager.
     */
    RemoteShieldManager shieldManager();

    /**
     * Get the <code>ReportingManager</code> singleton.
     *
     * @return the ReportingManager.
     */
    ReportingManager reportingManager();

    /**
     * Get the <code>ConnectivityTester</code> singleton.
     *
     * @return the ConnectivityTester
     */
    ConnectivityTester getConnectivityTester();

    /**
     * Get the AppServerManager singleton for this instance
     *
     * @return the singleton
     */
    RemoteAppServerManager appServerManager();

    /**
     * Get the phonebook singleton
     * @return the singleton
     */
    RemotePhoneBook phoneBook();

    /**
     * Get the RemotePortalManager singleton for this instance
     *
     * @return the singleton
     */
    RemotePortalManager portalManager();


    /**
     * Get the AddressBook singleton for this instance
     *
     * @return the singleton
     */
    AddressBook appAddressBook();

    /**
     * The BrandingManager allows for customization of logo and
     * branding information.
     *
     * @return the BrandingManager.
     */
    BrandingManager brandingManager();

    /**
     * Save settings to local hard drive.
     *
     * @exception IOException if the save was unsuccessful.
     */
    void localBackup() throws IOException;

    /**
     * Save settings to USB key drive.
     *
     * @exception IOException if the save was unsuccessful.
     */
    void usbBackup() throws IOException;

    void shutdown();

    /**
     * Reboots the Untangle Server as if the right button menu was used
     * and confirmed.  Note that this currently will not reboot a
     * non-production (dev) box; this behavior may change in the
     * future.  XXX
     *
     */
    void rebootBox();

    // debugging / performance management
    void doFullGC();

    // making sure the client and uvm versions are the same
    String version();

    /**
     * Get the activation key.
     *
     * @return the activation key.
     */
    String getActivationKey();

    /**
     * Return true if running in a development environment.
     *
     * @return a <code>boolean</code> true if in development.
     */
    boolean isDevel();

    /**
     * Create a backup which the client can save to a local disk.  The
     * returned bytes are for a .tar.gz file, so it is a good idea to
     * either use a ".tar.gz" extension so basic validation can be
     * performed for {@link #restoreBackup restoreBackup}.
     *
     * @return the byte[] contents of the backup.
     *
     * @exception IOException if something goes wrong (a lot can go wrong,
     *            but it is nothing the user did to cause this).
     */
    byte[] createBackup() throws IOException;

    /**
     * Restore from a previous {@link #createBackup backup}.
     *
     *
     * @exception IOException something went wrong to prevent the
     *            restore (not the user's fault).
     *
     * @exception IllegalArgumentException if the provided bytes do not seem
     *            to have come from a valid backup (is the user's fault).
     */
    void restoreBackup(byte[] backupFileBytes)
        throws IOException, IllegalArgumentException;
}
