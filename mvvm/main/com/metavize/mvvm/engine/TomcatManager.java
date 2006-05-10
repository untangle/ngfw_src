/*
 * Copyright (c) 2004, 2005, 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.engine;

import java.net.InetAddress;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Realm;
import org.apache.catalina.authenticator.BasicAuthenticator;
import org.apache.catalina.core.StandardDefaultContext;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.logger.FileLogger;
import org.apache.catalina.session.StandardManager;
import org.apache.catalina.startup.Embedded;
import org.apache.coyote.tomcat5.CoyoteConnector;
import org.apache.log4j.Logger;

/**
 * Wrapper around the Tomcat server embedded within the MVVM.
 *
 * Note that the rest of the MVVM should <b>not</b> access this code
 * directly.  Instead, please go through the {@link
 * com.metavize.mvvm.AppServerManager AppServerManager} interface.
 */
class TomcatManager
{
    private static final int NUM_TOMCAT_RETRIES = 15; //  5 minutes total
    private static final long TOMCAT_SLEEP_TIME = 20 * 1000; // 20 seconds
    private static final long REBIND_SLEEP_TIME = 1 * 1000; // 1 second
    private static final int NUM_REBIND_RETRIES = 5; //  10 seconds

    private static final  Logger logger = Logger.getLogger(TomcatManager.class);

    private final List<WebAppDescriptor> descriptors = new ArrayList<WebAppDescriptor>();

    private Embedded emb = null;
    private StandardHost baseHost;
    private Object modifyExternalSynch = new Object();

    private final String webAppRoot;
    private final String catalinaHome;
    private final String logDir;

    private final MvvmContextImpl mvvmContext;
    private final MvvmRealm mvvmRealm = new MvvmRealm();

    private String keystoreFile = "conf/keystore";
    private String keystorePass = "changeit";
    private String keyAlias = "tomcat";

    private CoyoteConnector defaultHTTPConnector;
    private CoyoteConnector defaultHTTPSConnector;
    private CoyoteConnector externalHTTPSConnector;

    // constructors -----------------------------------------------------------

    TomcatManager(MvvmContextImpl mvvmContext, String catalinaHome,
                  String webAppRoot, String logDir)
    {
        this.mvvmContext = mvvmContext;
        this.catalinaHome = catalinaHome;
        this.webAppRoot = webAppRoot;
        this.logDir = logDir;

        loadSystemApp("/session-dumper", "session-dumper");
        loadSystemApp("/webstart", "webstart");
        loadSystemApp("/store", "store");
        loadSystemApp("/reports", "reports");
    }

    // package protected methods ----------------------------------------------

    String getKeystoreFileName()
    {
        return this.keystoreFile;
    }

    String getKeystorePassword()
    {
        return this.keystorePass;
    }

    String getKeyAlias()
    {
        return this.keyAlias;
    }

    /**
     * Method sets the security info (cert) for this Tomcat.  If the
     * server is already started, this triggers a reset of the HTTPS
     * sockets.
     *
     * @param ksFile the KeyStore file
     * @param ksPass the password for the keystore file
     * @param ksAlias the alias within the KeyStore file
     */
    void setSecurityInfo(String ksFile, String ksPass, String ksAlias)
        throws Exception
    {
        this.keystoreFile = ksFile;
        this.keystorePass = ksPass;
        this.keyAlias = ksAlias;

        if (null != emb) {
            // XXX Some validation of the new Keystore data, so we
            // don't hose ourselves
            int port = 0;
            if (null != externalHTTPSConnector) {
                port = externalHTTPSConnector.getPort();
                destroyConnector(externalHTTPSConnector, "External HTTPS");
                externalHTTPSConnector = createConnector(port, true);
                startConnector(externalHTTPSConnector, "External HTTPS");
            }

            if (null != defaultHTTPSConnector) {
                port = defaultHTTPSConnector.getPort();
                destroyConnector(defaultHTTPSConnector, "Default HTTPS");
                defaultHTTPSConnector = createConnector(port, true);
                startConnector(defaultHTTPSConnector, "Default HTTPS");
            }
        }
    }

    boolean loadPortalApp(String urlBase, String rootDir)
    {
        PortalManagerImpl pm = mvvmContext.portalManager();
        Realm realm = pm.getPortalRealm();
        BasicAuthenticator auth = pm.newPortalAuthenticator();
        return loadWebApp(urlBase, rootDir, realm, auth);
    }

    boolean loadSystemApp(String urlBase, String rootDir)
    {
        MvvmAuthenticator mvvmAuth = new MvvmAuthenticator();
        return loadWebApp(urlBase, rootDir, mvvmRealm, mvvmAuth);
    }

    boolean loadInsecureApp(String urlBase, String rootDir)
    {
        return loadWebApp(urlBase, rootDir, null, null);
    }

    boolean unloadWebApp(String contextRoot)
    {
        try {
            if (null != baseHost) {
                Container c = baseHost.findChild(contextRoot);
                if (null != c) {
                    baseHost.removeChild(c);
                    return true;
                }
            }
        } catch(Exception ex) {
            logger.error("Unable to unload web app \"" +
                         contextRoot + "\"", ex);
        }
        return false;
    }

    void rebindExternalHttpsPort(int port)
        throws Exception
    {
        /* Synchronize on the external thread */
        synchronized(this.modifyExternalSynch) {
            doRebindExternalHttpsPort(port);
        }
    }

    /**
     * Gives no exceptions, even if Tomcat was never started.
     */
    void stopTomcat()
    {
        try {
            if (null != emb) {
                emb.stop();
            }
        } catch (LifecycleException exn) {
            logger.debug(exn);
        }
    }

    // XXX exception handling
    synchronized void startTomcat(InvokerBase invokerBase,
                                  int internalHTTPPort,
                                  int internalHTTPSPort,
                                  int externalHTTPSPort)
        throws Exception
    {
        // Change for 4.0: Put the Tomcat class loader insdie the MVVM
        // class loader.

        // jdi 8/30/04 -- canonical host name depends on ordering of
        // /etc/hosts
        String hostname = "localhost";

        // set default logger and realm
        FileLogger fileLog = new FileLogger();
        fileLog.setDirectory(logDir);
        fileLog.setPrefix("tomcat");
        fileLog.setSuffix(".log");
        fileLog.setTimestamp(true);
        // fileLog.setVerbosityLevel("DEBUG");

        emb = new Embedded(fileLog, mvvmRealm);
        emb.setCatalinaHome(catalinaHome);

        // create an Engine
        Engine baseEngine = emb.createEngine();

        // set Engine properties
        baseEngine.setName("tomcat");
        baseEngine.setDefaultHost(hostname);
        baseEngine.setParentClassLoader(Thread.currentThread().getContextClassLoader());

        // Set up the Default Context
        StandardDefaultContext sdc = new StandardDefaultContext();
        sdc.setAllowLinking(true);
        baseEngine.addDefaultContext(sdc);

        // create Host
        baseHost = (StandardHost)emb
            .createHost(hostname, webAppRoot);
        baseHost.setUnpackWARs(true);
        baseHost.setDeployOnStartup(true);
        baseHost.setAutoDeploy(true);
        baseHost.getPipeline().addValve(new org.apache.catalina.authenticator.SingleSignOn());

        // add host to Engine
        baseEngine.addChild(baseHost);

        // create root Context
        Context ctx = emb.createContext("", webAppRoot + "/ROOT");
        StandardManager mgr = new StandardManager();
        mgr.setPathname(null); /* disable session persistence */
        ctx.setManager(mgr);
        ctx.setManager(new StandardManager());

        // add context to host
        baseHost.addChild(ctx);

        // create application Context
        ctx = emb.createContext("/http-invoker", "http-invoker");
        ctx.setPrivileged(true);
        baseHost.addChild(ctx);
        ctx.getServletContext().setAttribute("invoker", invokerBase);
        mgr = new StandardManager();
        mgr.setPathname(null); /* disable session persistence */
        ctx.setManager(mgr);

        // Load the webapps which were requested before the
        // system started-up.
        for (WebAppDescriptor desc : descriptors) {
            loadWebAppImpl(desc.urlBase, desc.relativeRoot, desc.realm,
                           desc.auth);
        }

        // add new Engine to set of
        // Engine for embedded server
        emb.addEngine(baseEngine);

        // create Connectors
        defaultHTTPConnector = createConnector(internalHTTPPort, false);
        defaultHTTPSConnector = createConnector(internalHTTPSPort, true);

        /* Start the outside https server */
        if (externalHTTPSPort != internalHTTPSPort) {
            externalHTTPSConnector = createConnector(externalHTTPSPort, true);
        }

        // start operation
        try {
            emb.start();
        } catch (LifecycleException exn) {
            Throwable wrapped = exn.getThrowable();
            // Note -- right now wrapped is always null!  Thus the
            // following horror:
            boolean isAddressInUse = isAIUExn(exn);
            if (isAddressInUse) {
                Runnable tryAgain = new Runnable() {
                        public void run() {
                            int i;
                            for (i = 0; i < NUM_TOMCAT_RETRIES; i++) {
                                try {
                                    logger.warn("could not start Tomcat (address in use), sleeping 20 and trying again");
                                    Thread.sleep(TOMCAT_SLEEP_TIME);
                                    try {
                                        emb.stop();
                                    } catch (LifecycleException exn) {
                                        logger.warn(exn, exn);
                                    }
                                    emb.start();
                                    logger.info("Tomcat successfully started");
                                    break;
                                } catch (InterruptedException x) {
                                    return;
                                } catch (LifecycleException x) {
                                    boolean isAddressInUse = isAIUExn(x);
                                    if (!isAddressInUse) {
                                        Main.fatalError("Starting Tomcat", x);
                                        return;
                                    }
                                }
                            }
                            if (i == NUM_TOMCAT_RETRIES)
                                Main.fatalError("Unable to start Tomcat after " +
                                                NUM_TOMCAT_RETRIES
                                                + " tries, giving up",
                                                null);
                        }
                    };
                new Thread(tryAgain, "Tomcat starter").start();
            } else {
                // Something else, just die die die.
                throw exn;
            }
        }
    }

    String generateAuthNonce(InetAddress clientAddr, Principal user)
    {
        return mvvmRealm.generateAuthNonce(clientAddr, user);
    }

    // private classes --------------------------------------------------------

    private static class WebAppDescriptor
    {
        final String urlBase;
        final String relativeRoot;
        final Realm realm;
        final BasicAuthenticator auth;

        WebAppDescriptor(String base, String rr, Realm realm,
                         BasicAuthenticator auth)
        {
            this.urlBase = base;
            this.relativeRoot = rr;
            this.realm = realm;
            this.auth = auth;
        }
    }

    // private methods --------------------------------------------------------

    private synchronized boolean loadWebApp(String urlBase, String rootDir,
                                            Realm realm,
                                            BasicAuthenticator auth)
    {
        if (null == emb) {
            // haven't started yet
            WebAppDescriptor wad = new WebAppDescriptor(urlBase, rootDir,
                                                        realm, auth);
            descriptors.add(wad);
            return true;
        } else {
            return loadWebAppImpl(urlBase, rootDir, realm, auth);
        }
    }

    private boolean loadWebAppImpl(String urlBase, String rootDir,
                                   Realm realm, BasicAuthenticator auth)
    {
        String fqRoot = webAppRoot + "/" + rootDir;

        try {
            Context ctx = emb.createContext(urlBase, fqRoot);
            if (null != realm) {
                ctx.setRealm(realm);
            }
            StandardManager mgr = new StandardManager();
            mgr.setPathname(null); /* disable session persistence */
            ctx.setManager(mgr);
            if (null != auth) {
                Pipeline pipe = ctx.getPipeline();
                auth.setDisableProxyCaching(false);
                pipe.addValve(auth);
            }
            baseHost.addChild(ctx);
            return true;
        } catch(Exception ex) {
            logger.error("Unable to deploy webapp \"" + urlBase
                         + "\" from directory \"" + fqRoot + "\"", ex);
            return false;
        }
    }

    private void doRebindExternalHttpsPort(int port) throws Exception
    {
        logger.debug("Rebinding the HTTPS port");

        if (port == 80 || port == 0 || port > 0xFFFF) {
            throw new Exception("Cannot bind external to port 80");
        }

        /* If there was a failed attempt, retry, startExternal will
         * only be null */
        if (null != externalHTTPSConnector
            && externalHTTPSConnector.getPort() == port) {
            logger.info("External is already bound to port: " + port);
            return;
        }
        destroyConnector(externalHTTPSConnector, "External HTTPS");
        externalHTTPSConnector = null;

        /* If it is not the default port, then rebind it */
        if (null == defaultHTTPSConnector
            || defaultHTTPSConnector.getPort() != port) {
            logger.info("Rebinding external server to " + port);
            externalHTTPSConnector = createConnector(port, true);
            if (!startConnector(externalHTTPSConnector, "External HTTPS")) {
                throw new Exception("Unable to bind to: " + port);
            }
        }
    }

    /**
     * Helper method - no synchronization
     */
    private CoyoteConnector createConnector(int port, boolean secure)
    {
        CoyoteConnector ret = (CoyoteConnector)emb
            .createConnector((InetAddress)null, port, secure);

        if (secure) {
            ret.setKeystoreFile(this.keystoreFile);
            ret.setKeystorePass(this.keystorePass);
            ret.setKeyAlias(this.keyAlias);
        }

        emb.addConnector(ret);
        return ret;
    }

    private boolean destroyConnector(CoyoteConnector connector,
                                     String nameForLog)
    {
        /* Need to change the port */
        if (null != connector) {
            logger.info("Removing connector on port " + connector.getPort());
            emb.removeConnector(connector);
            try {
                connector.stop();
            } catch (Exception e) {
                logger.error("Unable to stop externalConnector", e);
                return false;
            }
        }
        return true;
    }

    private boolean startConnector(CoyoteConnector connector, String logName)
    {
        for (int i = 0; i < NUM_REBIND_RETRIES; i++) {
            try {
                connector.start();
                return true;
            } catch (LifecycleException ex) {
                if (i == NUM_REBIND_RETRIES) {
                    logger.error("Exception starting connector \"" + logName
                                 + "\" on port " + connector.getPort(), ex);
                    return false;
                }
            } catch (Exception ex) {
                logger.error("Exception starting connector \"" + logName
                             + "\" on port " + connector.getPort(), ex);
                return false;
            }

            try {
                Thread.sleep(REBIND_SLEEP_TIME);
            } catch (InterruptedException exn) {
                // XXX exit mechanism
                logger.warn("Interrupted, breaking");
                return false;
            }
        }
        logger.error("Unable to start connector \"" + logName + "\" on port "
                     + connector.getPort() + " after " + NUM_REBIND_RETRIES
                     + " attempts sleeping " + REBIND_SLEEP_TIME
                     + " between start attempts");
        return false;
    }

    private boolean isAIUExn(LifecycleException exn)
    {
        Throwable wrapped = exn.getThrowable();
        String msg = exn.getMessage();
        if (wrapped != null && wrapped instanceof java.net.BindException)
            // Never happens right now. XXX
            return true;
        if (msg.contains("address already in use"))
            return true;
        return false;
    }
}
