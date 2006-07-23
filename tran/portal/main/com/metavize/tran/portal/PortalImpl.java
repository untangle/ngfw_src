/*
 * Copyright (c) 2005, 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.portal;

import java.util.List;

import com.metavize.mvvm.AppServerManager;
import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.MvvmLocalContext;
import com.metavize.mvvm.logging.EventLogger;
import com.metavize.mvvm.logging.EventLoggerFactory;
import com.metavize.mvvm.logging.EventManager;
import com.metavize.mvvm.logging.SimpleEventFilter;
import com.metavize.mvvm.portal.Application;
import com.metavize.mvvm.portal.Bookmark;
import com.metavize.mvvm.portal.LocalApplicationManager;
import com.metavize.mvvm.portal.LocalPortalManager;
import com.metavize.mvvm.portal.PortalEvent;
import com.metavize.mvvm.portal.PortalSettings;
import com.metavize.mvvm.tapi.AbstractTransform;
import com.metavize.mvvm.tapi.PipeSpec;
import com.metavize.mvvm.tran.TransformContext;
import com.metavize.mvvm.tran.TransformStartException;
import com.metavize.mvvm.tran.TransformStopException;
import com.metavize.tran.portal.rdp.RdpBookmark;
import com.metavize.tran.portal.vnc.VncBookmark;
import org.apache.log4j.Logger;

public class PortalImpl extends AbstractTransform implements PortalTransform
{
    private static final String CIFS_JS_URL = "/browser/secure/app.js";
    private static final String WEB_JS_URL = "/proxy/app.js";
    private static final String RDP_JS_URL = "/rdp/app.js";
    private static final String VNC_JS_URL = "/vnc/app.js";

    private final Logger logger = Logger.getLogger(PortalImpl.class);

    private final PipeSpec[] pipeSpecs = new PipeSpec[0];

    private final EventLogger<PortalEvent> eventLogger;

    private Application browserApp;
    private Application proxyApp;
    private Application rdpApp;
    private Application vncApp;

    private LocalPortalManager lpm;

    // constructors -----------------------------------------------------------

    public PortalImpl()
    {
        MvvmLocalContext mctx = MvvmContextFactory.context();
        lpm = mctx.portalManager();
        logger.debug("<init>");
        TransformContext tctx = getTransformContext();
        eventLogger = lpm.getEventLogger(tctx);

        SimpleEventFilter ef = new PortalLoginoutFilter();
        eventLogger.addSimpleEventFilter(ef);
     }

    private void registerApps() {
        LocalApplicationManager lam = lpm.applicationManager();
        browserApp = lam.registerApplication("CIFS", "Network File Browser",
                                             "kaka",
                                             null, null, 0, CIFS_JS_URL);

        Application.Destinator httpDestinator = new Application.Destinator() {
                public String getDestinationHost(Bookmark bm) {
            // This isn't yet used, so we fake it for now. XXX
            return "localhost";
                }
                public int getDestinationPort(Bookmark bm) {
            // This isn't yet used, so we fake it for now. XXX
                    return 80;
                }
            };

        proxyApp = lam.registerApplication("HTTP", "Web Proxy", "poo poo",
                                           httpDestinator,
                                           null, 0, WEB_JS_URL);

        Application.Destinator rdpDestinator = new Application.Destinator() {
                public String getDestinationHost(Bookmark bm) {
                    RdpBookmark rb = new RdpBookmark(bm);
                    return rb.getHost();
                }
                public int getDestinationPort(Bookmark bm) {
                    return 3389;
                }
            };

        rdpApp = lam.registerApplication("RDP", "Remote Desktop", "potty",
                                         rdpDestinator, null, 0, RDP_JS_URL);

        Application.Destinator vncDestinator = new Application.Destinator() {
                public String getDestinationHost(Bookmark bm) {
                    VncBookmark rb = new VncBookmark(bm);
                    return rb.getHost();
                }
                public int getDestinationPort(Bookmark bm) {
                    VncBookmark vb = new VncBookmark(bm);
                    return 5900 + vb.getDisplayNumber();
                }
            };

        vncApp = lam.registerApplication("VNC", "Virtual Network Computing",
                                         "horse sense",
                                         vncDestinator, null, 0, VNC_JS_URL);
    }

    private void deployWebAppIfRequired(Logger logger) {
        MvvmLocalContext mctx = MvvmContextFactory.context();
        AppServerManager asm = mctx.appServerManager();

        if (asm.loadPortalApp("/browser", "browser")) {
            logger.debug("Deployed Browser web app");
        } else {
            logger.error("Unable to deploy Browser web app");
        }

        if (asm.loadPortalApp("/proxy", "proxy")) {
            logger.debug("Deployed Proxy web app");
        } else {
            logger.error("Unable to deploy Proxy web app");
        }
        if (asm.loadPortalApp("/vnc", "vnc")) {
            logger.debug("Deployed VNC Portal app");
        } else {
            logger.error("Unable to deploy VNC Portal app");
        }

        if (asm.loadPortalApp("/rdp", "rdp")) {
            logger.debug("Deployed RDP Portal app");
        } else {
            logger.error("Unable to deploy RDP Portal app");
        }

        if (asm.loadPortalApp("/portal", "portal")) {
            logger.debug("Deployed Portal web app");
        } else {
            logger.error("Unable to deploy Portal web app");
        }

        asm.setRootWelcome("./portal/");
    }

    private void deregisterApps() {
        LocalApplicationManager lam = lpm.applicationManager();
        lam.deregisterApplication(browserApp);
        lam.deregisterApplication(proxyApp);
        lam.deregisterApplication(rdpApp);
        lam.deregisterApplication(vncApp);
    }

    private void unDeployWebAppIfRequired(Logger logger) {
        MvvmLocalContext mctx = MvvmContextFactory.context();
        AppServerManager asm = mctx.appServerManager();

        asm.resetRootWelcome();

        if (asm.unloadWebApp("/portal")) {
            logger.debug("Unloaded Portal web app");
        } else {
            logger.warn("Unable to unload Portal web app");
        }

        if (asm.unloadWebApp("/browser")) {
            logger.debug("Unloaded Browser web app");
        } else {
            logger.warn("Unable to unload Browser web app");
        }

        if (asm.unloadWebApp("/proxy")) {
            logger.debug("Unloaded Proxy web app");
        } else {
            logger.warn("Unable to unload Proxy web app");
        }

        if (asm.unloadWebApp("/rdp")) {
            logger.debug("Unloaded RDP Portal app");
        } else {
            logger.warn("Unable to unload RDP Portal app");
        }

        if (asm.unloadWebApp("/vnc")) {
            logger.debug("Unloaded VNC Portal app");
        } else {
            logger.warn("Unable to unload VNC Portal app");
        }
    }

    // Portal methods ---------------------------------------------------------

    // Transform methods ------------------------------------------------------

    public EventManager<PortalEvent> getEventManager()
    {
        return eventLogger;
    }

    protected void initializeSettings() { }

    @Override
        protected void preStop() throws TransformStopException {
        super.preStop();
        logger.debug("preStop()");
        unDeployWebAppIfRequired(logger);
    }

    @Override
    protected void postStart() throws TransformStartException
    {
        logger.debug("postStart()");

        deployWebAppIfRequired(logger);
    }

    @Override
    protected void postInit(final String[] args) {
        eventLogger.start();
        registerApps();
    }

    @Override
    protected void preDestroy() {
        deregisterApps();
        eventLogger.stop();
    }

    // Portal methods ----------------------------------------------

    public List<Application> getApplications()
    {
        return MvvmContextFactory.context().portalManager().applicationManager().getApplications();
    }

    public List<String> getApplicationNames()
    {
        return MvvmContextFactory.context().portalManager().applicationManager().getApplicationNames();
    }

    public Application getApplication(String name)
    {
        return MvvmContextFactory.context().portalManager().applicationManager().getApplication(name);
    }

    public PortalSettings getPortalSettings()
    {
        return MvvmContextFactory.context().portalManager().getPortalSettings();
    }

    public void setPortalSettings(PortalSettings settings)
    {
        MvvmContextFactory.context().portalManager().setPortalSettings(settings);
    }

    // AbstractTransform methods ----------------------------------------------

    @Override
        protected PipeSpec[] getPipeSpecs()
    {
        return pipeSpecs;
    }

    // XXX soon to be deprecated ----------------------------------------------

    public Object getSettings() { return getPortalSettings(); }

    public void setSettings(Object settings) { setPortalSettings((PortalSettings)settings); }
}
