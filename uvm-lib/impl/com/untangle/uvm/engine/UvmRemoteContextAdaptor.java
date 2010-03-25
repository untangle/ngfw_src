/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.uvm.engine;

import java.io.IOException;

import com.untangle.uvm.ArgonManager;
import com.untangle.uvm.MailSender;
import com.untangle.uvm.RemoteAppServerManager;
import com.untangle.uvm.RemoteBrandingManager;
import com.untangle.uvm.RemoteConnectivityTester;
import com.untangle.uvm.RemoteLanguageManager;
import com.untangle.uvm.RemoteNetworkManager;
import com.untangle.uvm.RemoteSkinManager;
import com.untangle.uvm.RemoteOemManager;
import com.untangle.uvm.addrbook.RemoteAddressBook;
import com.untangle.uvm.benchmark.RemoteBenchmarkManager;
import com.untangle.uvm.client.RemoteUvmContext;
import com.untangle.uvm.license.RemoteLicenseManager;
import com.untangle.uvm.logging.RemoteLoggingManager;
import com.untangle.uvm.message.RemoteMessageManager;
import com.untangle.uvm.node.RemoteIntfManager;
import com.untangle.uvm.node.RemoteNodeManager;
import com.untangle.uvm.policy.RemotePolicyManager;
import com.untangle.uvm.reports.RemoteReportingManager;
import com.untangle.uvm.security.RemoteAdminManager;
import com.untangle.uvm.toolbox.RemoteToolboxManager;
import com.untangle.uvm.toolbox.RemoteUpstreamManager;

/**
 * Adapts UvmContextImpl to RemoteUvmContext.
 *
 * @author <a href="mailto:amread@nyx.net">Aaron Read</a>
 * @version 1.0
 */
class RemoteUvmContextAdaptor implements RemoteUvmContext
{
    private final UvmContextImpl context;

    // constructors -----------------------------------------------------------

    RemoteUvmContextAdaptor(UvmContextImpl context)
    {
        this.context = context;
    }

    // RemoteUvmContext methods ----------------------------------------------

    public RemoteToolboxManager toolboxManager()
    {
        return context.toolboxManager();
    }

    public RemoteUpstreamManager upstreamManager()
    {
        return context.upstreamManager();
    }

    public RemoteNodeManager nodeManager()
    {
        return context.remoteNodeManager();
    }

    public RemoteLoggingManager loggingManager()
    {
        return context.loggingManager();
    }

    public RemotePolicyManager policyManager()
    {
        return context.remotePolicyManager();
    }

    public RemoteAdminManager adminManager()
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

    public RemoteNetworkManager networkManager()
    {
        return context.remoteNetworkManager();
    }

    public RemoteReportingManager reportingManager()
    {
        return context.reportingManager();
    }

    public RemoteConnectivityTester getRemoteConnectivityTester()
    {
        return context.getRemoteConnectivityTester();
    }

    public MailSender mailSender()
    {
        return context.mailSender();
    }

    public RemoteAppServerManager appServerManager()
    {
        return context.remoteAppServerManager();
    }

    public RemoteAddressBook appAddressBook()
    {
        return context.appRemoteAddressBook();
    }

    public RemoteBrandingManager brandingManager()
    {
        return context.brandingManager();
    }

    public RemoteSkinManager skinManager()
    {
        return context.skinManager();
    }

    public RemoteMessageManager messageManager()
    {
        return context.messageManager();
    }

    public RemoteLanguageManager languageManager()
    {
        return context.languageManager();
    }

    public RemoteLicenseManager licenseManager()
    {
        return context.remoteLicenseManager();
    }
    
    public RemoteBenchmarkManager benchmarkManager()
    {
        return context.localBenchmarkManager();
    }

    public RemoteOemManager oemManager()
    {
        return context.oemManager();
    }
    
    public void localBackup() throws IOException
    {
        context.localBackup();
    }

    public void usbBackup() throws IOException
    {
        context.usbBackup();
    }

    public void syncConfigFiles()
    {
        context.syncConfigFiles();
    }

    public void shutdown()
    {
        context.shutdown();
    }

    public void rebootBox()
    {
        context.rebootBox();
    }

    public void shutdownBox()
    {
        context.shutdownBox();
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

    public boolean isUntangleAppliance()
    {
        return context.isUntangleAppliance();
    }

    public boolean isInsideVM()
    {
        return context.isInsideVM();
    }

    public String installationType()
    {
        return context.installationType();
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

    public void restoreBackup(String fileName)
        throws IOException, IllegalArgumentException
    {
        context.restoreBackup(fileName);
    }

    public boolean loadRup()
    {
        return context.loadRup();
    }

    public String setProperty(String key, String value)
    {
        return context.setProperty(key, value);
    }
}
