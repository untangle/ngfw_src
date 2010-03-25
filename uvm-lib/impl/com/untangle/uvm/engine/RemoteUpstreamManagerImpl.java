/*
 * $HeadURL: svn://chef/work/src/uvm-lib/impl/com/untangle/uvm/engine/RemoteToolboxManagerImpl.java $
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.toolbox.MackageDesc;
import com.untangle.uvm.toolbox.MackageInstallException;
import com.untangle.uvm.toolbox.MackageUninstallException;
import com.untangle.uvm.toolbox.RemoteToolboxManager;
import com.untangle.uvm.toolbox.RemoteUpstreamManager;
import com.untangle.uvm.toolbox.UpstreamService;

/**
 * Implements RemoteUpstreamManager.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
class RemoteUpstreamManagerImpl implements RemoteUpstreamManager
{
    private static final Object LOCK = new Object();

    private static final File UPSTREAM_SERVICES_FILE;
    private static final String NO_PACKAGE = "NONE";

    private RemoteToolboxManager toolboxMgr;

    private final Logger logger = Logger.getLogger(getClass());

    private static RemoteUpstreamManagerImpl UPSTREAM_MANAGER;

    // There are few enough upstream services that we don't bother
    // with a Map.
    private List<UpstreamService> services; 

    private RemoteUpstreamManagerImpl(RemoteToolboxManager toolboxMgr) {
        this.toolboxMgr = toolboxMgr;

        refresh();
    }

    // Called at init time and whenever refreshToolbox runs.
    void refresh() {
        MackageDesc[] installed = toolboxMgr.installed();
        services = new ArrayList<UpstreamService>();

        // Each line in upstream-services that is not a comment is of
        // the form: service package
        try {
            BufferedReader r = new BufferedReader(new FileReader(UPSTREAM_SERVICES_FILE));
            String line, sname, spack;
            String[] namepack;
            UpstreamService serv;
            boolean enabled;
            while ((line = r.readLine()) != null) {
                line.trim();
                if (line.length() == 0 || line.charAt(0) == '#')
                    continue;
                namepack = line.split("\\s+");
                if (namepack.length == 0) {
                    continue;
                } else if (namepack.length == 1) {
                    // No package
                    sname = namepack[0];
                    logger.debug("Adding new always enabled upstream service: " + sname);
                    serv = new UpstreamService(sname, true, null);
                } else if (namepack.length == 2 || namepack[2].charAt(0) == '#') {
                    enabled = false;
                    sname = namepack[0];
                    spack = namepack[1];
                    if (spack.equals(NO_PACKAGE)) {
                        logger.debug("Adding new always enabled upstream service: " + sname);
                        serv = new UpstreamService(sname, true, null);
                    } else {
                        for (MackageDesc md : installed) {
                            logger.debug("checking " + spack + " against " + md.getName());
                            if (spack.equals(md.getName())) {
                                enabled = true;
                                break;
                            }
                        }
                        logger.debug("Adding new upstream service: " + sname +
                                     " conditional on " + spack + ", currently " +
                                     (enabled ? "enabled" : "disabled"));
                        serv = new UpstreamService(sname, enabled, spack);
                    }
                } else {
                    logger.error("Weird upstream service line ignored: " + line);
                    continue;
                }
                services.add(serv);
            }
        } catch (IOException x) {
            logger.error("Unable to read upstream-services", x);
        }
    }

    static RemoteUpstreamManagerImpl upstreamManager()
    {
        if (null == UPSTREAM_MANAGER) {
            synchronized (LOCK) {
                if (null == UPSTREAM_MANAGER) {
                    UPSTREAM_MANAGER =
                        new RemoteUpstreamManagerImpl(LocalUvmContextFactory.context().toolboxManager());
                }
            }
        }
        return UPSTREAM_MANAGER;
    }

    // RemoteUpstreamManager implementation ------------------------------------

    // all known services
    public String[] allServiceNames()
    {
        String[] result = new String[services.size()];
        int i = 0;
        for (UpstreamService service : services) {
            result[i++] = service.name();
        }
        return result;
    }

    public UpstreamService getService(String name)
    {
        for (UpstreamService service : services) {
            if (name.equals(service.name()))
                return service;
        }
        return null;
    }

    public void enableService(String name)
        throws IllegalArgumentException, MackageInstallException
    {
        UpstreamService service = getService(name);
        if (service == null)
            throw new IllegalArgumentException("Service " + service + " not found");
        if (service.enabled())
            return;
        String configPackage = service.configPackage();
        if (configPackage == null)
            return;
        logger.info("Enabling upstream service: " + name);
        toolboxMgr.install(configPackage);

        // Finally since we didn't throw a Mackage exception, change the
        // service to enabled.
        services.remove(service);
        services.add(new UpstreamService(name, true, configPackage));
    }

    public void disableService(String name)
        throws IllegalArgumentException, MackageUninstallException
    {
        UpstreamService service = getService(name);
        if (service == null)
            throw new IllegalArgumentException("Service " + service + " not found");
        if (!service.enabled())
            return;
        String configPackage = service.configPackage();
        if (configPackage == null)
            throw new IllegalArgumentException("Service " + service + " has no configPackage");
        logger.info("Disabling upstream service: " + name);
        toolboxMgr.uninstall(configPackage);

        // Finally since we didn't throw a Mackage exception, change the
        // service to disabled.
        services.remove(service);
        services.add(new UpstreamService(name, false, configPackage));
    }


    static {
        String cd = System.getProperty("uvm.conf.dir");
        UPSTREAM_SERVICES_FILE = new File(cd, "upstream-services");
    }

    // Test
    public static void main(String[] args) {
        BufferedReader in = new BufferedReader(new java.io.InputStreamReader(System.in));
        try {
            String line = in.readLine();
            String[] strs = line.split("\\s+");
            for (String str : strs) {
                System.out.println("'" + str + "'");
            }
        } catch (IOException x) { }
        //        RemoteUpstreamManager mgr =
        //             new RemoteUpstreamManagerImpl(RemoteToolboxManagerImpl.toolboxManager());
    }
}
