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

import java.net.InetAddress;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import com.untangle.mvvm.util.AdministrationOutsideAccessValve;
import com.untangle.mvvm.util.ReportingOutsideAccessValve;
import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Realm;
import org.apache.catalina.Valve;
import org.apache.catalina.authenticator.AuthenticatorBase;
import org.apache.catalina.authenticator.SingleSignOn;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.session.StandardManager;
import org.apache.catalina.startup.Embedded;
import org.apache.coyote.http11.Http11BaseProtocol;
import org.apache.log4j.Logger;

/**
 * Wrapper around the Tomcat server embedded within the MVVM.
 */
class TomcatManager
{
    private static final int NUM_TOMCAT_RETRIES = 15; //  5 minutes total
    private static final long TOMCAT_SLEEP_TIME = 20 * 1000; // 20 seconds
    private static final long REBIND_SLEEP_TIME = 1 * 1000; // 1 second
    private static final int NUM_REBIND_RETRIES = 5; //  10 seconds

    private static final String STANDARD_WELCOME = "webstart";

    private final Logger logger = Logger.getLogger(getClass());

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

    private Context rootContext;
    private String welcomeFile = STANDARD_WELCOME;

    private Connector defaultHTTPConnector;
    private Connector defaultHTTPSConnector;
    private Connector internalOpenHTTPSConnector; /* Sessions on this port are unrestricted */
    private Connector externalHTTPSConnector;

    // constructors -----------------------------------------------------------

    TomcatManager(MvvmContextImpl mvvmContext, String catalinaHome,
                  String webAppRoot, String logDir)
    {
        this.mvvmContext = mvvmContext;
        this.catalinaHome = catalinaHome;
        this.webAppRoot = webAppRoot;
        this.logDir = logDir;

        /* rbs: according to the javadoc, each valve object can only be assigned to one container,
         * otherwise it is supposed to throw an IllegalStateException */
        loadSystemApp("/session-dumper", "session-dumper", new WebAppOptions(new AdministrationOutsideAccessValve()));
        loadSystemApp("/webstart", "webstart", new WebAppOptions(new AdministrationOutsideAccessValve()));
        loadSystemApp("/onlinestore", "onlinestore", new WebAppOptions(new AdministrationOutsideAccessValve()));
        loadSystemApp("/reports", "reports", new WebAppOptions(true,new ReportingOutsideAccessValve()));
        loadSystemApp("/wmi", "wmi", new WebAppOptions(new WMIServerValve()));
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
        AuthenticatorBase auth = pm.newPortalAuthenticator();
        // Need a large timeout since we handle that ourselves.
        WebAppOptions options = new WebAppOptions(false, 24*60);
        return loadWebApp(urlBase, rootDir, realm, auth, options);
    }

    boolean loadSystemApp(String urlBase, String rootDir, WebAppOptions options)
    {
        MvvmAuthenticator mvvmAuth = new MvvmAuthenticator();
        return loadWebApp(urlBase, rootDir, mvvmRealm, mvvmAuth, options);
    }

    boolean loadSystemApp(String urlBase, String rootDir) {
        return loadSystemApp(urlBase, rootDir, new WebAppOptions());
    }

    boolean loadInsecureApp(String urlBase, String rootDir)
    {
        return loadWebApp(urlBase, rootDir, null, null);
    }

    boolean loadInsecureApp(String urlBase, String rootDir, Valve valve)
    {
        return loadWebApp(urlBase, rootDir, null, null, valve);
    }


    boolean unloadWebApp(String contextRoot)
    {
        try {
            if (null != baseHost) {
                Container c = baseHost.findChild(contextRoot);
                if (null != c) {
                    logger.info("Removing web app " + contextRoot);
                    baseHost.removeChild(c);
                    try {
                        ((StandardContext)c).destroy();
                    } catch (Exception x) {
                        logger.warn("Exception destroying web app \"" +
                                    contextRoot + "\"", x);
                    }
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
    synchronized void startTomcat(HttpInvoker httpInvoker,
                                  int internalHTTPPort,
                                  int internalHTTPSPort,
                                  int externalHTTPSPort,
                                  int internalOpenHTTPSPort)
        throws Exception
    {
        // Change for 4.0: Put the Tomcat class loader insdie the MVVM
        // class loader.
        ClassLoader mvvmCl = Thread.currentThread().getContextClassLoader();
        ClassLoader tomcatParent = new TomClassLoader(mvvmCl);
        try {
            // Entering Tomcat ClassLoader ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            Thread.currentThread().setContextClassLoader(tomcatParent);

            // jdi 8/30/04 -- canonical host name depends on ordering of
            // /etc/hosts
            String hostname = "localhost";

            // set default logger and realm
            // FileLogger fileLog = new FileLogger();
            // fileLog.setDirectory(logDir);
            // fileLog.setPrefix("tomcat");
            // fileLog.setSuffix(".log");
            // fileLog.setTimestamp(true);
            // fileLog.setVerbosityLevel("DEBUG");

            emb = new Embedded(/* fileLog, */ mvvmRealm);
            emb.setCatalinaHome(catalinaHome);

            // create an Engine
            Engine baseEngine = emb.createEngine();

            // set Engine properties
            baseEngine.setName("tomcat");
            baseEngine.setDefaultHost(hostname);

            baseEngine.setParentClassLoader(tomcatParent);

            // create Host
            baseHost = (StandardHost)emb
                .createHost(hostname, webAppRoot);
            baseHost.setUnpackWARs(true);
            baseHost.setDeployOnStartup(true);
            baseHost.setAutoDeploy(true);
            baseHost.setErrorReportValveClass("com.untangle.mvvm.engine.MvvmErrorReportValve");
            OurSingleSignOn ourSsoWorkaroundValve = new OurSingleSignOn();
            /* XXXXX Hackstered to get single sign on to ignore certain contexts */
            SingleSignOn ssoValve = new SpecialSingleSignOn( mvvmContext, "/session-dumper", "/webstart", "", "/reports",
                                                             "/onlinestore" );
            // ssoValve.setRequireReauthentication(true);
            baseHost.getPipeline().addValve(ourSsoWorkaroundValve);
            baseHost.getPipeline().addValve(ssoValve);

            // add host to Engine
            baseEngine.addChild(baseHost);

            // create root Context
            rootContext = emb.createContext("", webAppRoot + "/ROOT");
            StandardManager mgr = new StandardManager();
            mgr.setPathname(null); /* disable session persistence */
            rootContext.setManager(mgr);
            rootContext.setManager(new StandardManager());
            setRootWelcome(welcomeFile);

            // add context to host
            baseHost.addChild(rootContext);

            // create application Context
            StandardContext ctx = (StandardContext)emb.createContext("/http-invoker", "http-invoker");
            mgr = new StandardManager();
            mgr.setPathname(null); /* disable session persistence */
            ctx.setManager(mgr);

            /* Add a valve to block outside access */
            ctx.addValve(new AdministrationOutsideAccessValve());

            /* Moved after adding the valve */
            baseHost.addChild(ctx);
            ctx.getServletContext().setAttribute("invoker", httpInvoker);

            // Load the webapps which were requested before the
            // system started-up.
            for (WebAppDescriptor desc : descriptors) {
                loadWebAppImpl(desc.urlBase, desc.relativeRoot, desc.realm,
                               desc.auth, desc.options);
            }

            // add new Engine to set of
            // Engine for embedded server
            emb.addEngine(baseEngine);

            // create Connectors
            defaultHTTPConnector = createConnector(internalHTTPPort, false);
            defaultHTTPSConnector = createConnector(internalHTTPSPort, true);
            internalOpenHTTPSConnector = createConnector(internalOpenHTTPSPort, true);

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
                                            MvvmContextImpl.getInstance().fatalError("Starting Tomcat", x);
                                            return;
                                        }
                                    }
                                }
                                if (i == NUM_TOMCAT_RETRIES)
                                    MvvmContextImpl.getInstance().fatalError("Unable to start Tomcat after " +
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
        } finally {
            Thread.currentThread().setContextClassLoader(mvvmCl);
            // restored classloader ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        }

    }

    String generateAuthNonce(InetAddress clientAddr, Principal user)
    {
        return mvvmRealm.generateAuthNonce(clientAddr, user);
    }

    void resetRootWelcome()
    {
        setRootWelcome(STANDARD_WELCOME);
    }

    synchronized void setRootWelcome(String welcomeFile)
    {
        this.welcomeFile = welcomeFile;
    }

    String getRootWelcome()
    {
        return welcomeFile;
    }

    // private classes --------------------------------------------------------

    private static class WebAppDescriptor
    {
        final String urlBase;
        final String relativeRoot;
        final Realm realm;
        final AuthenticatorBase auth;
        final WebAppOptions options;

        WebAppDescriptor(String base, String rr, Realm realm,
                         AuthenticatorBase auth, WebAppOptions options)
        {
            this.urlBase = base;
            this.relativeRoot = rr;
            this.realm = realm;
            this.auth = auth;
            this.options = options;
        }
    }

    private static class WebAppOptions
    {
        public static final int DEFAULT_SESSION_TIMEOUT = 30;

        final boolean allowLinking;
        final int sessionTimeout; // Minutes
        final Valve valve;

        WebAppOptions() {
            this(false, DEFAULT_SESSION_TIMEOUT);
        }

        WebAppOptions(Valve valve) {
            this(false, valve);
        }

        WebAppOptions(boolean allowLinking) {
            this(allowLinking, DEFAULT_SESSION_TIMEOUT);
        }

        WebAppOptions(boolean allowLinking, Valve valve) {
            this(allowLinking, DEFAULT_SESSION_TIMEOUT, valve);
        }

        WebAppOptions(boolean allowLinking, int sessionTimeout) {
            this(allowLinking, sessionTimeout, null);
        }

        WebAppOptions(boolean allowLinking, int sessionTimeout, Valve valve) {
            this.allowLinking = allowLinking;
            this.sessionTimeout = sessionTimeout;
            this.valve  = valve;

        }
    }


    // private methods --------------------------------------------------------

    /**
     * Loads the web application.  If Tomcat is not yet running, schedules it for
     * later loading.
     *
     * @param urlBase a <code>String</code> value
     * @param rootDir a <code>String</code> value
     * @param realm a <code>Realm</code> value
     * @param auth an <code>AuthenticatorBase</code> value
     * @return a <code>boolean</code> value
     */
    private synchronized boolean loadWebApp(String urlBase,
                                            String rootDir,
                                            Realm realm,
                                            AuthenticatorBase auth,
                                            WebAppOptions options)
    {
        if (null == emb) {
            // haven't started yet
            WebAppDescriptor wad = new WebAppDescriptor(urlBase, rootDir,
                                                        realm, auth, options);
            descriptors.add(wad);
            return true;
        } else {
            return loadWebAppImpl(urlBase, rootDir, realm, auth, options);
        }
    }

    private boolean loadWebApp(String urlBase,
                               String rootDir,
                               Realm realm,
                               AuthenticatorBase auth) {
        return loadWebApp(urlBase, rootDir, realm, auth, new WebAppOptions());
    }

    private boolean loadWebApp(String urlBase,
                               String rootDir,
                               Realm realm,
                               AuthenticatorBase auth,
                               Valve valve) {
        return loadWebApp(urlBase, rootDir, realm, auth, new WebAppOptions(valve));
    }

    private boolean loadWebAppImpl(String urlBase, String rootDir,
                                   Realm realm, AuthenticatorBase auth, WebAppOptions options)
    {
        String fqRoot = webAppRoot + "/" + rootDir;

        logger.info("Adding web app " + fqRoot);
        try {
            StandardContext ctx = (StandardContext) emb.createContext(urlBase, fqRoot);
            if (options.allowLinking)
                ctx.setAllowLinking(true);
            ctx.setSessionTimeout(options.sessionTimeout);
            if (null != realm) {
                ctx.setRealm(realm);
            }

            StandardManager mgr = new StandardManager();
            mgr.setPathname(null); /* disable session persistence */
            ctx.setManager(mgr);

            /* This should be the first valve */
            if (null != options.valve) ctx.addValve(options.valve);

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

        /* This is kind of a questionable situation here, buecause it is not accessible here */
        if (null != internalOpenHTTPSConnector
            && internalOpenHTTPSConnector.getPort() == port) {
            logger.info("External is already bound to port: " + port);
            return;
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
    private Connector createConnector(int port, boolean secure)
    {
        Connector ret = emb
            .createConnector((InetAddress)null, port, secure);

        if (secure) {
            Http11BaseProtocol ph = (Http11BaseProtocol)ret.getProtocolHandler();
            ph.setKeystore(this.keystoreFile);
            ph.setKeypass(this.keystorePass);
            ph.setKeyAlias(this.keyAlias);
        }

        emb.addConnector(ret);
        return ret;
    }

    private boolean destroyConnector(Connector connector,
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

    private boolean startConnector(Connector connector, String logName)
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

    private static class TomClassLoader extends ClassLoader {
        TomClassLoader(ClassLoader parent) {
            super(parent);
        }
    }
}
