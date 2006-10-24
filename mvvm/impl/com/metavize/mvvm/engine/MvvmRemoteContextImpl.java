/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle Networks, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.engine;

import java.io.IOException;

import com.metavize.mvvm.ArgonManager;
import com.metavize.mvvm.ConnectivityTester;
import com.metavize.mvvm.NetworkManager;
import com.metavize.mvvm.RemoteAppServerManager;
import com.metavize.mvvm.ReportingManager;
import com.metavize.mvvm.addrbook.AddressBook;
import com.metavize.mvvm.api.RemoteIntfManager;
import com.metavize.mvvm.api.RemoteShieldManager;
import com.metavize.mvvm.client.MvvmRemoteContext;
import com.metavize.mvvm.logging.LoggingManager;
import com.metavize.mvvm.networking.ping.PingManager;
import com.metavize.mvvm.policy.PolicyManager;
import com.metavize.mvvm.portal.RemotePortalManager;
import com.metavize.mvvm.security.AdminManager;
import com.metavize.mvvm.toolbox.ToolboxManager;
import com.metavize.mvvm.tran.TransformManager;

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

    public AddressBook appAddressBook()
    {
        return context.appAddressBook();
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
