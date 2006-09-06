/*
 * Copyright (c) 2003, 2004, 2005, 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.client;

import java.io.IOException;

import com.metavize.mvvm.AppServerManager;
import com.metavize.mvvm.ConnectivityTester;
import com.metavize.mvvm.NetworkManager;
import com.metavize.mvvm.NetworkingManager;
import com.metavize.mvvm.api.RemoteShieldManager;
import com.metavize.mvvm.ReportingManager;
import com.metavize.mvvm.addrbook.AddressBook;
import com.metavize.mvvm.logging.LoggingManager;
import com.metavize.mvvm.policy.PolicyManager;
import com.metavize.mvvm.portal.RemotePortalManager;
import com.metavize.mvvm.security.AdminManager;
import com.metavize.mvvm.toolbox.ToolboxManager;
import com.metavize.mvvm.tran.TransformManager;

/**
 * Provides an interface to get major MVVM components that are
 * accessible a remote client.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 */
public interface MvvmRemoteContext
{
    /**
     * Get the <code>ToolboxManager</code> singleton.
     *
     * @return the ToolboxManager.
     */
    ToolboxManager toolboxManager();

    /**
     * Get the <code>TransformManager</code> singleton.
     *
     * @return the TransformManager.
     */
    TransformManager transformManager();

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
     * Get the <code>NetworkingManager</code> singleton.
     *
     * @return the NetworkingManager.
     */
    NetworkingManager networkingManager();

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
    AppServerManager appServerManager();

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
     * Reboots the EdgeGuard box as if the right button menu was used
     * and confirmed.  Note that this currently will not reboot a
     * non-production (dev) box; this behavior may change in the
     * future.  XXX
     *
     */
    void rebootBox();

    // debugging / performance management
    void doFullGC();

    // making sure the client and mvvm versions are the same
    String version();

    /**
     * Create a backup which the client can save to a local disk.  The
     * returned bytes are for a .tar.gz file, so it is a good idea to
     * either use a ".tar.gz" extension or ".metavizebk" extension so
     * basic validation can be performed for {@link #restoreBackup
     * restoreBackup).
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
