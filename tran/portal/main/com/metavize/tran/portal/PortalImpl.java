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
import com.metavize.mvvm.logging.LogEvent;
import com.metavize.mvvm.portal.Application;
import com.metavize.mvvm.portal.Bookmark;
import com.metavize.mvvm.portal.LocalApplicationManager;
import com.metavize.mvvm.portal.LocalPortalManager;
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
    private static final String CIFS_JS
        = "{\n"
        + "  openBookmark: function(portal, bookmark) {\n"
        + "    portal.showApplicationUrl('/browser?target=' + bookmark.target, bookmark);\n"
        + "  }\n"
        + "};\n";

    private static final String WEB_JS
        = "{\n"
        + "  openBookmark: function(portal, bookmark) {\n"
        + "    var o = portal.splitUrl(bookmark.target);"
        + "    portal.showApplicationUrl('/proxy/' + o.proto + '/' + o.host + o.path, bookmark);\n"
        + "  }\n"
        + "};\n";

    private static final String RDP_JS
        = "{\n"
        + "  openBookmark: function(portal, bookmark) {\n"
        + "    portal.showApplicationUrl('/rdp/rdp.jsp?t=' + bookmark.id, bookmark);\n"
        + "  }\n"
        + "};\n";

    private static final String VNC_JS
        = "{\n"
        + "  openBookmark: function(portal, bookmark) {\n"
        + "    portal.showApplicationUrl('/vnc/vnc.jsp?t=' + bookmark.id, bookmark);\n"
        + "  }\n"
        + "};\n";

    private final Logger logger = Logger.getLogger(PortalImpl.class);

    private final PipeSpec[] pipeSpecs = new PipeSpec[0];

    private final EventLogger<LogEvent> eventLogger;

    private Application browserApp;
    private Application proxyApp;
    private Application rdpApp;
    private Application vncApp;

    // constructors -----------------------------------------------------------

    public PortalImpl()
    {
        logger.debug("<init>");
        TransformContext tctx = getTransformContext();
        eventLogger = EventLoggerFactory.factory().getEventLogger(tctx);
     }

    private void deployWebAppIfRequired(Logger logger) {
        MvvmLocalContext mctx = MvvmContextFactory.context();
        AppServerManager asm = mctx.appServerManager();
        LocalPortalManager lpm = mctx.portalManager();
        LocalApplicationManager lam = lpm.applicationManager();

        if (asm.loadPortalApp("/browser", "browser")) {
            logger.debug("Deployed Browser web app");
        } else {
            logger.error("Unable to deploy Browser web app");
        }

        browserApp = lam.registerApplication("CIFS", "Network File Browser",
                                             null, null, 0, CIFS_JS);

        if (asm.loadPortalApp("/proxy", "proxy")) {
            logger.debug("Deployed Proxy web app");
        } else {
            logger.error("Unable to deploy Proxy web app");
        }

        proxyApp = lam.registerApplication("HTTP", "Web Proxy", null, null, 0,
                                           WEB_JS);

        Application.Destinator rdpDestinator = new Application.Destinator() {
                public String getDestinationHost(Bookmark bm) {
                    RdpBookmark rb = new RdpBookmark(bm);
                    return rb.getHost();
                }
                public int getDestinationPort(Bookmark bm) {
                    return 3389;
                }
            };

        rdpApp = lam.registerApplication("RDP", "Remote Desktop",
                                         rdpDestinator, null, 0, RDP_JS);

        if (asm.loadPortalApp("/rdp", "rdp")) {
            logger.debug("Deployed RDP Portal app");
        } else {
            logger.error("Unable to deploy RDP Portal app");
        }

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
                                         vncDestinator, null, 0, VNC_JS);

        if (asm.loadPortalApp("/vnc", "vnc")) {
            logger.debug("Deployed VNC Portal app");
        } else {
            logger.error("Unable to deploy VNC Portal app");
        }

        if (asm.loadPortalApp("/portal", "portal")) {
            logger.debug("Deployed Portal web app");
        } else {
            logger.error("Unable to deploy Portal web app");
        }

        asm.setRootWelcome("./portal/");
    }

    private void unDeployWebAppIfRequired(Logger logger) {
        MvvmLocalContext mctx = MvvmContextFactory.context();
        AppServerManager asm = mctx.appServerManager();
        LocalPortalManager lpm = mctx.portalManager();
        LocalApplicationManager lam = lpm.applicationManager();

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

        lam.deregisterApplication(browserApp);

        if (asm.unloadWebApp("/proxy")) {
            logger.debug("Unloaded Proxy web app");
        } else {
            logger.warn("Unable to unload Proxy web app");
        }

        lam.deregisterApplication(proxyApp);

        lam.deregisterApplication(rdpApp);

        if (asm.unloadWebApp("/rdp")) {
            logger.debug("Unloaded RDP Portal app");
        } else {
            logger.warn("Unable to unload RDP Portal app");
        }

        lam.deregisterApplication(vncApp);

        if (asm.unloadWebApp("/vnc")) {
            logger.debug("Unloaded VNC Portal app");
        } else {
            logger.warn("Unable to unload VNC Portal app");
        }
    }

    // Portal methods ---------------------------------------------------------

    // Transform methods ------------------------------------------------------

    public EventManager<LogEvent> getEventManager()
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
    
    protected void postStart() throws TransformStartException
    {
        logger.debug("postStart()");

        deployWebAppIfRequired(logger);
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
