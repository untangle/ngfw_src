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

package com.untangle.uvm.engine;

import java.io.IOException;

import com.untangle.uvm.ArgonManager;
import com.untangle.uvm.BrandingManager;
import com.untangle.uvm.ConnectivityTester;
import com.untangle.uvm.NetworkManager;
import com.untangle.uvm.RemoteAppServerManager;
import com.untangle.uvm.ReportingManager;
import com.untangle.uvm.addrbook.AddressBook;
import com.untangle.uvm.node.RemoteIntfManager;
import com.untangle.uvm.node.RemoteShieldManager;
import com.untangle.uvm.client.UvmRemoteContext;
import com.untangle.uvm.logging.LoggingManager;
import com.untangle.uvm.networking.ping.PingManager;
import com.untangle.uvm.policy.PolicyManager;
import com.untangle.uvm.security.AdminManager;
import com.untangle.uvm.toolbox.ToolboxManager;
import com.untangle.uvm.node.NodeManager;
import com.untangle.uvm.user.RemotePhoneBook;

class UvmRemoteContextImpl implements UvmRemoteContext
{
    private final UvmContextImpl context;

    // constructors -----------------------------------------------------------

    UvmRemoteContextImpl(UvmContextImpl context)
    {
        this.context = context;
    }

    // UvmRemoteContext methods ----------------------------------------------

    public ToolboxManager toolboxManager()
    {
        return context.toolboxManager();
    }

    public NodeManager nodeManager()
    {
        return context.remoteNodeManager();
    }

    public LoggingManager loggingManager()
    {
        return context.loggingManager();
    }

    public PolicyManager policyManager()
    {
        return context.remotePolicyManager();
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
        return context.appRemoteAddressBook();
    }

    public BrandingManager brandingManager()
    {
        return context.brandingManager();
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

    public boolean isDevel()
    {
        return context.isDevel();
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
