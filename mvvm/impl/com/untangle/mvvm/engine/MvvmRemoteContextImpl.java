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

package com.untangle.mvvm.engine;

import java.io.IOException;

import com.untangle.mvvm.ArgonManager;
import com.untangle.mvvm.BrandingManager;
import com.untangle.mvvm.ConnectivityTester;
import com.untangle.mvvm.NetworkManager;
import com.untangle.mvvm.RemoteAppServerManager;
import com.untangle.mvvm.ReportingManager;
import com.untangle.mvvm.addrbook.AddressBook;
import com.untangle.mvvm.api.RemoteIntfManager;
import com.untangle.mvvm.api.RemoteShieldManager;
import com.untangle.mvvm.client.MvvmRemoteContext;
import com.untangle.mvvm.logging.LoggingManager;
import com.untangle.mvvm.networking.ping.PingManager;
import com.untangle.mvvm.policy.PolicyManager;
import com.untangle.mvvm.portal.RemotePortalManager;
import com.untangle.mvvm.security.AdminManager;
import com.untangle.mvvm.toolbox.ToolboxManager;
import com.untangle.mvvm.tran.TransformManager;
import com.untangle.mvvm.user.RemotePhoneBook;

class MvvmRemoteContextImpl implements MvvmRemoteContext
{
    private final MvvmContextImpl context;

    // constructors -----------------------------------------------------------

    MvvmRemoteContextImpl(MvvmContextImpl context)
    {
        this.context = context;
    }

    // MvvmRemoteContext methods ----------------------------------------------

    public ToolboxManager toolboxManager()
    {
        return context.toolboxManager();
    }

    public TransformManager transformManager()
    {
        return context.remoteTransformManager();
    }

    public LoggingManager loggingManager()
    {
        return context.loggingManager();
    }

    public PolicyManager policyManager()
    {
        return context.policyManager();
    }

    public AdminManager adminManager()
    {
        return context.adminManager();
    }

    public ArgonManager argonManager()
    {
        return context.argonManager();
    }

    public RemoteIntfManager intfManager()
    {
        return context.remoteIntfManager();
    }

    public NetworkManager networkManager()
    {
        return context.remoteNetworkManager();
    }

    public PingManager pingManager()
    {
        return context.pingManager();
    }

    public RemoteShieldManager shieldManager()
    {
        return context.remoteShieldManager();
    }

    public ReportingManager reportingManager()
    {
        return context.reportingManager();
    }

    public ConnectivityTester getConnectivityTester()
    {
        return context.getConnectivityTester();
    }

    public RemoteAppServerManager appServerManager()
    {
        return context.remoteAppServerManager();
    }

    public RemotePhoneBook phoneBook()
    {
        return context.remotePhoneBook();
    }

    public AddressBook appAddressBook()
    {
        return context.appAddressBook();
    }

    public BrandingManager brandingManager()
    {
        return context.brandingManager();
    }

    public RemotePortalManager portalManager()
    {
        return context.remotePortalManager();
    }

    public void localBackup() throws IOException
    {
        context.localBackup();
    }

    public void usbBackup() throws IOException
    {
        context.usbBackup();
    }

    public void shutdown()
    {
        context.shutdown();
    }

    public void rebootBox()
    {
        context.rebootBox();
    }

    public void doFullGC()
    {
        context.doFullGC();
    }

    public String version()
    {
        return context.version();
    }

    public String getActivationKey()
    {
        return context.getActivationKey();
    }

    public byte[] createBackup() throws IOException
    {
        return context.createBackup();
    }

    public void restoreBackup(byte[] backupBytes)
        throws IOException, IllegalArgumentException
    {
        context.restoreBackup(backupBytes);
    }
}
