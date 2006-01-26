/*
 * Copyright (c) 2004, 2005, 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: Main.java 3717 2005-12-01 06:04:17Z rbscott $
 */

package com.metavize.mvvm.engine;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.core.StandardDefaultContext;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.logger.FileLogger;
import org.apache.catalina.session.StandardManager;
import org.apache.catalina.startup.Embedded;
import org.apache.coyote.tomcat5.CoyoteConnector;
import org.apache.log4j.Logger;

/**
  * Wrapper around the Tomcat server embedded within the MVVM.
  * <br>
  * Note that the rest of the MVVM should <b>not</b> access this
  * code directly.  Instead, please go through the
  * {@link com.metavize.mvvm.AppServerManager AppServerManager} interface.
  */
public class TomcatManager {

    public static int NUM_TOMCAT_RETRIES = 15;        //  5 minutes total
    public static long TOMCAT_SLEEP_TIME = 20 * 1000; // 20 seconds
    private static final long REBIND_SLEEP_TIME = 1 * 1000; // 1 second
    public static int NUM_REBIND_RETRIES = 5;        //  10 seconds

    private Embedded emb = null;
    private StandardHost baseHost;
    private List<WebAppDescriptor> descriptors;
    private Object modifyExternalSynch = new Object();
    private static final Logger logger = Logger.getLogger(TomcatManager.class);

    private final String webAppRoot;
    private final String catalinaHome;
    private final String logDir;

    private String keystoreFile = "conf/keystore";
    private String keystorePass = "changeit";
    private String keyAlias = "tomcat";

    private CoyoteConnector defaultHTTPConnector;
    private CoyoteConnector defaultHTTPSConnector;
    private CoyoteConnector externalHTTPSConnector;


    /**
     * Little class used to describe a web app to be deployed.
     */
    class WebAppDescriptor {
        final String urlBase;
        final String relativeRoot;

        WebAppDescriptor(String base, String rr) {
            this.urlBase = base;
            this.relativeRoot = rr;
        }
    }

    TomcatManager(String catalinaHome,
        String webAppRoot,
        String logDir) {

        this.catalinaHome = catalinaHome;
        this.webAppRoot = webAppRoot;
        this.logDir = logDir;

        //Create the list of web-apps we know we're going to deploy
        //*before* we actualy create out Tomcat
        descriptors = new ArrayList<WebAppDescriptor>();
        descriptors.add(new WebAppDescriptor("/session-dumper", "session-dumper"));
        descriptors.add(new WebAppDescriptor("/webstart", "webstart"));
        descriptors.add(new WebAppDescriptor("/reports", "reports"));
    }

    public String getKeystoreFileName() {
      return this.keystoreFile;
    }
    public String getKeystorePassword() {
      return this.keystorePass;
    }
    public String getKeyAlias() {
      return this.keyAlias;
    }

    /**
     * Method sets the security info (cert) for this Tomcat.  If the
     * server is already started, this triggers a reset of the
     * HTTPS sockets
     *
     * @param ksFile the KeyStore file
     * @param ksPass the password for the keystore file
     * @param ksAlias the alias within the KeyStore file
     */
    public void setSecurityInfo(String ksFile, String ksPass, String ksAlias)
      throws Exception {

      this.keystoreFile = ksFile;
      this.keystorePass = ksPass;
      this.keyAlias = ksAlias;

      if(emb != null) {
        //TODO Some validation of the new Keystore data, so we don't
        //     hose ourselves
        int port = 0;
        if(externalHTTPSConnector != null) {
          port = externalHTTPSConnector.getPort();
          destroyConnector(externalHTTPSConnector, "External HTTPS");
          externalHTTPSConnector = createConnector(port, true);
          startConnector(externalHTTPSConnector, "External HTTPS");
        }
        if(defaultHTTPSConnector != null) {
          port = defaultHTTPSConnector.getPort();
          destroyConnector(defaultHTTPSConnector, "Default HTTPS");
          defaultHTTPSConnector = createConnector(port, true);
          startConnector(defaultHTTPSConnector, "Default HTTPS");
        }
      }
    }

    public synchronized boolean loadWebApp(String urlBase,
                                           String rootDir) {
        if(emb == null) {
            //Haven't started yet
            descriptors.add(new WebAppDescriptor(urlBase, rootDir));
            return true;
        }
        else {
            return loadWebAppImpl(urlBase, rootDir);
        }
    }


    private boolean loadWebAppImpl(String urlBase,
                                    String rootDir) {


        String fqRoot = webAppRoot + "/" + rootDir;

        try {
            Context ctx = emb.createContext(urlBase, fqRoot);
            StandardManager mgr = new StandardManager();
            mgr.setPathname(null); /* disable session persistence */
            ctx.setManager(mgr);
            baseHost.addChild(ctx);
            return true;
        }
        catch(Exception ex) {
            logger.error("Unable to deploy webapp \"" +
                          urlBase + "\" from directory \"" +
                          fqRoot + "\"", ex);
            return false;
        }
    }


    public boolean unloadWebApp(String contextRoot) {
        try {
            if (baseHost != null) {
                Container c = baseHost.findChild(contextRoot);
                if(c != null) {
                    baseHost.removeChild(c);
                    return true;
                }
            }
        }
        catch(Exception ex) {
            logger.error("Unable to unload web app \"" +
                          contextRoot + "\"", ex);
        }
        return false;
    }

    public void rebindExternalHttpsPort(int port) throws Exception
    {
        /* Synchronize on the external thread */
        synchronized(this.modifyExternalSynch) {
            doRebindExternalHttpsPort( port );
        }
    }

    private void doRebindExternalHttpsPort(int port) throws Exception
    {
        logger.debug( "Rebinding the HTTPS port" );

        if ( port == 80 || port == 0 || port > 0xFFFF ) {
            throw new Exception( "Cannot bind external to port 80" );
        }

        /* If there was a failed attempt, retry, startExternal will only be null */
        if ((externalHTTPSConnector != null) &&
            (externalHTTPSConnector.getPort() == port)) {
            logger.info( "External is already bound to port: " + port );
            return;
        }
        destroyConnector(externalHTTPSConnector, "External HTTPS");
        externalHTTPSConnector = null;

        /* If it is not the default port, then rebind it */
        if ( defaultHTTPSConnector == null || port != defaultHTTPSConnector.getPort() ) {
            logger.info( "Rebinding external server to " + port );
            externalHTTPSConnector = createConnector(port, true);
            if(!startConnector(externalHTTPSConnector, "External HTTPS")) {
              throw new Exception("Unable to bind to: " + port);
            }
        }
    }

    /**
      * Gives no exceptions, even if Tomcat was never started.
      */
    void stopTomcat() {
        try {
            if (emb != null) {
                emb.stop();
            }
        } catch (LifecycleException exn) {
            logger.debug(exn);
        }
    }

    /**
     * Helper method - no synchronization
     */
    private CoyoteConnector createConnector(int port, boolean secure) {
        CoyoteConnector ret = (CoyoteConnector)emb.createConnector((InetAddress)null, port, secure);
        if(secure) {
            ret.setKeystoreFile(this.keystoreFile);
            ret.setKeystorePass(this.keystorePass);
            ret.setKeyAlias(this.keyAlias);
        }
        emb.addConnector(ret);
        return ret;
    }

    private boolean destroyConnector(CoyoteConnector connector, String nameForLog) {
        /* Need to change the port */
        if ( connector != null ) {
            logger.info( "Removing connector on port " + connector.getPort());
            emb.removeConnector( connector );
            try {
                connector.stop();
            } catch ( Exception e ) {
                logger.error( "Unable to stop externalConnector", e );
                return false;
            }
        }
        return true;
    }

    private boolean startConnector(CoyoteConnector connector, String nameForLog) {
      for (int i = 0; i < NUM_REBIND_RETRIES; i++) {
        try {
          connector.start();
          return true;
        }
        catch (LifecycleException ex) {
          if (i == NUM_REBIND_RETRIES) {
            logger.error("Exception starting connector \"" + nameForLog + "\" on port " +
              connector.getPort(), ex);
            return false;
          }
        }
        catch (Exception ex) {
          logger.error("Exception starting connector \"" + nameForLog + "\" on port " +
            connector.getPort(), ex);
          return false;
        }

        try {
            Thread.sleep(REBIND_SLEEP_TIME);
        } catch (InterruptedException exn) {
            /* ??? */
            logger.warn("Interrupted, breaking");
            return false;
        }
      }
      logger.error("Unable to start connector \"" + nameForLog + "\" on port " +
        connector.getPort() + " after " + NUM_REBIND_RETRIES + " attempts sleeping " +
        REBIND_SLEEP_TIME + " between start attempts");
      return false;
    }

    // XXX exception handling
    public synchronized void startTomcat(MvvmContextBase contextForInvoker,
      int internalHTTPPort,
      int internalHTTPSPort,
      int externalHTTPSPort) throws Exception
    {
        // jdi 8/30/04 -- canonical host name depends on ordering of /etc/hosts
        String hostname = "localhost";

        // set default logger and realm
        FileLogger fileLog = new FileLogger();
        fileLog.setDirectory(logDir);
        fileLog.setPrefix("tomcat");
        fileLog.setSuffix(".log");
        fileLog.setTimestamp(true);
        // fileLog.setVerbosityLevel("DEBUG");

        emb = new Embedded(fileLog, new MvvmRealm());
        emb.setCatalinaHome(catalinaHome);

        // create an Engine
        Engine baseEngine = emb.createEngine();

        // set Engine properties
        baseEngine.setName("tomcat");
        baseEngine.setDefaultHost(hostname);

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
        ctx.getServletContext()
            .setAttribute("invoker", contextForInvoker.getInvokerBase());
        mgr = new StandardManager();
        mgr.setPathname(null); /* disable session persistence */
        ctx.setManager(mgr);

        //Load the webapps which were requested before the
        //system started-up.
        for(WebAppDescriptor desc : descriptors) {
            loadWebAppImpl(desc.urlBase, desc.relativeRoot);
        }

        // add new Engine to set of
        // Engine for embedded server
        emb.addEngine(baseEngine);

        // create Connectors
        defaultHTTPConnector = createConnector(internalHTTPPort, false);
        defaultHTTPSConnector = createConnector(internalHTTPSPort, true);

        /* Start the outside https server */
        if ( externalHTTPSPort != internalHTTPSPort ) {
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

    private boolean isAIUExn(LifecycleException exn) {
        Throwable wrapped = exn.getThrowable();
        String msg = exn.getMessage();
        if (wrapped != null && wrapped instanceof java.net.BindException)
            // Never happens right now. XXX
            return true;
        if (msg.contains("ddress already in use"))
            return true;
        return false;
    }
}

