/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.engine;

import java.io.File;
import java.io.FileInputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.catalina.Connector;
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

public class Main
{
    public static int HTTP_PORT = 80;
    public static int HTTPS_PORT = 443;

    public static long TOMCAT_SLEEP_TIME = 20 * 1000; // 20 seconds
    public static int NUM_TOMCAT_RETRIES = 15;        //  5 minutes total

    private static String MVVM_LOCAL_CONTEXT_CLASSNAME
        = "com.metavize.mvvm.engine.MvvmContextImpl";

    static {
        MvvmRepositorySelector.get().init("");
    }

    private static final Logger logger = Logger.getLogger(Main.class);

    private URLClassLoader ucl;
    private Class mvvmPrivClass;
    private MvvmContextBase mvvmContext;

    private String bunniculaConf;
    private String bunniculaData;
    private String bunniculaHome;
    private String bunniculaLib;
    private String bunniculaLog;
    private String bunniculaToolbox;
    private String bunniculaWeb;
    private String bunniculaTmp;

    private TomcatManager m_tomcatManager;

    // constructor ------------------------------------------------------------

    private Main() { }

    // public static methods --------------------------------------------------

    // XXX get rid of all these throws
    public static final void main(String[] args) throws Exception
    {
        SchemaUtil.initSchema("settings", "mvvm");

        new Main().init();
    }

    /**
     * <code>fatalError</code> can be called by any part of the mvvm
     * (even by transforms for now, change later XXX) to indicate that
     * a fatal error has occured and that the MVVM *must* restart (or
     * otherwise recover) itself.  One example is an OutOfMemory
     * error.
     *
     * @param x a <code>Throwable</code> giving the related/causing
     * exception, if any, otherwise null
     */
    public static void fatalError(String throwingLocation, Throwable x)
    {
        try {
            System.err.println("Fatal Error in MVVM in " + throwingLocation);
            if (x != null) {
                System.err.println("Throwable: " + x.getMessage());
                x.printStackTrace(System.err);
            }
        } catch (Throwable y) {
            // We want to always call System.exit(), so we do
            // absolutely nothing here.
        } finally {
            System.exit(-1);
        }
    }

    // private methods --------------------------------------------------------

    // XXX get rid of all these throws
    private void init() throws Exception
    {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                public void run() { destroy(); }
            }));

        ClassLoader cl = getClass().getClassLoader();
        Thread.currentThread().setContextClassLoader(cl);

        logger.info("setting up properties");
        setProperties();
        logger.info("starting mvvm");
        //Create the tomcat manager *before* the MVVM, so we can "register"
        //webapps to be started before Tomcat exists.
        m_tomcatManager = new TomcatManager();
        try {
            startMvvm();
        } catch (Throwable exn) {
            fatalError("could not start mvvm", exn);
        }
        System.out.println("MVVM startup complete: \"Today vegetables...tomorrow the world!\"");
        logger.info("restarting transforms and socket invoker");
        restartTransfoms();
        logger.info("starting tomcat");
        m_tomcatManager.startTomcat();
        System.out.println("MVVM postInit complete");
    }

    private void destroy()
    {
        m_tomcatManager.stopTomcat();

        mvvmContext.doDestroy();
        try {
            DataSourceFactory.factory().destroy();
        } catch (SQLException exn) {
            logger.warn("could not destory DataSourceFactory", exn);
        }
        System.out.println("MVVM shutdown complete.");
    }

    private void setProperties() throws Exception
    {
        bunniculaHome = System.getProperty("bunnicula.home");

        bunniculaLib = bunniculaHome + "/lib";
        System.setProperty("bunnicula.lib.dir", bunniculaLib);
        bunniculaToolbox = bunniculaHome + "/toolbox";
        System.setProperty("bunnicula.toolbox.dir", bunniculaToolbox);
        bunniculaLog = bunniculaHome + "/log";
        System.setProperty("bunnicula.log.dir", bunniculaLog);
        bunniculaData = bunniculaHome + "/data";
        System.setProperty("bunnicula.data.dir", bunniculaData);
        bunniculaWeb = bunniculaHome + "/web";
        System.setProperty("bunnicula.web.dir", bunniculaWeb);
        bunniculaConf = bunniculaHome + "/conf";
        System.setProperty("bunnicula.conf.dir", bunniculaConf);
        bunniculaTmp = bunniculaHome + "/tmp";
        System.setProperty("bunnicula.tmp.dir", bunniculaTmp);

        System.setProperty("derby.system.home", bunniculaHome + "/db");

        logger.info("bunnicula.home        " + bunniculaHome);
        logger.info("bunnicula.lib.dir     " + bunniculaLib);
        logger.info("bunnicula.toolbox.dir " + bunniculaToolbox);
        logger.info("bunnicula.log.dir     " + bunniculaLog);
        logger.info("bunnicula.data.dir    " + bunniculaData);
        logger.info("bunnicula.web.dir     " + bunniculaWeb);
        logger.info("bunnicula.conf.dir    " + bunniculaConf);
        logger.info("bunnicula.tmp.dir    " + bunniculaTmp);

        File f = new File(bunniculaConf + "/mvvm.properties");
        if (f.exists()) {
            logger.info("Loading " + f);
            System.getProperties().load(new FileInputStream(f));
        } else {
            logger.warn("Could not find " + f);
        }
    }


    // private methods --------------------------------------------------------

    // XXX get rid of all these throws
    private void startMvvm() throws Exception
    {
        URL mvvmJar = new URL("file://" + bunniculaLib + "/mvvm.jar");
        URL jVectorJar = new URL("file://" + bunniculaLib + "/jvector.jar");
        URL jNetcapJar = new URL("file://" + bunniculaLib + "/jnetcap.jar");
        URL[] urls = new URL[] { mvvmJar, jVectorJar, jNetcapJar };
        ucl = new URLClassLoader(urls, getClass().getClassLoader());

        ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
        try {
            // Entering MVVM ClassLoader ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            Thread.currentThread().setContextClassLoader(ucl);

            MvvmRepositorySelector.get().init("mvvm");

            mvvmContext = (MvvmContextBase)ucl
                .loadClass(MVVM_LOCAL_CONTEXT_CLASSNAME)
                .getMethod("context").invoke(null);

            mvvmContext.doInit(this);
        } finally {
            Thread.currentThread().setContextClassLoader(oldCl);
            // restored classloader ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        }
    }

    private void restartTransfoms() throws Exception
    {
        ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
        try {
            // Entering MVVM ClassLoader ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            Thread.currentThread().setContextClassLoader(ucl);

            mvvmContext.doPostInit();

        } finally {
            Thread.currentThread().setContextClassLoader(oldCl);
            // restored classloader ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        }
    }

    //Callback from the MvvmContext to load
    //a web app
    public boolean loadWebApp(String urlBase,
      String rootDir) {
      return m_tomcatManager.loadWebApp(urlBase, rootDir);
    }

    //Callback from the MvvmContext to unload
    //a web app
    public boolean unloadWebApp(String contextRoot) {
      return m_tomcatManager.unloadWebApp(contextRoot);
    }

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

    /**
     * Tomcat stuff broken into its own class, to simplify some synchronization
     * issues.
     */
    class TomcatManager {

        private Embedded emb = null;
        private StandardHost baseHost;
        private List<WebAppDescriptor> m_descriptors;

        TomcatManager() {
          //Create the list of web-apps we know we're going to deploy
          //*before* we actualy create out Tomcat
          m_descriptors = new ArrayList<WebAppDescriptor>();
          m_descriptors.add(new WebAppDescriptor("/session-dumper", "session-dumper"));
          m_descriptors.add(new WebAppDescriptor("/webstart", "webstart"));
          m_descriptors.add(new WebAppDescriptor("/reports", "reports"));
        }

        synchronized boolean loadWebApp(String urlBase,
          String rootDir) {
          if(emb == null) {
            //Haven't started yet
            m_descriptors.add(new WebAppDescriptor(urlBase, rootDir));
            return true;
          }
          else {
            return loadWebAppImpl(urlBase, rootDir);
          }
        }


        private boolean loadWebAppImpl(String urlBase,
          String rootDir) {


          String fqRoot = bunniculaWeb + "/" + rootDir;

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


        boolean unloadWebApp(String contextRoot) {
          try {
            Container c = baseHost.findChild(contextRoot);
            if(c != null) {
              baseHost.removeChild(c);
              return true;
            }
          }
          catch(Exception ex) {
            logger.error("Unable to unload web app \"" +
              contextRoot + "\"", ex);
          }
          return false;
        }

        /**
         * Gives no exceptions, even if Tomcat was never started.
         */
        private void stopTomcat() {
            try {
                if (emb != null) {
                    emb.stop();
                }
            } catch (LifecycleException exn) {
                logger.debug(exn);
            }
        }

        // XXX exception handling
        private synchronized void startTomcat() throws Exception
        {
            // jdi 8/30/04 -- canonical host name depends on ordering of /etc/hosts
            String hostname = "localhost";

            // set default logger and realm
            FileLogger fileLog = new FileLogger();
            fileLog.setDirectory(bunniculaLog);
            fileLog.setPrefix("tomcat");
            fileLog.setSuffix(".log");
            fileLog.setTimestamp(true);
            // fileLog.setVerbosityLevel("DEBUG");

            emb = new Embedded(fileLog, new MvvmRealm());
            emb.setCatalinaHome(bunniculaHome);

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
                .createHost(hostname, bunniculaWeb);
            baseHost.setUnpackWARs(true);
            baseHost.setDeployOnStartup(true);
            baseHost.setAutoDeploy(true);

            // add host to Engine
            baseEngine.addChild(baseHost);

            // create root Context
            Context ctx = emb.createContext("", bunniculaWeb + "/ROOT");
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
                .setAttribute("invoker", mvvmContext.getInvokerBase());
            mgr = new StandardManager();
            mgr.setPathname(null); /* disable session persistence */
            ctx.setManager(mgr);

            //Load the webapps which were requested before the
            //system started-up.
            for(WebAppDescriptor desc : m_descriptors) {
              loadWebAppImpl(desc.urlBase, desc.relativeRoot);
            }

            // add new Engine to set of
            // Engine for embedded server
            emb.addEngine(baseEngine);

            // create Connector
            CoyoteConnector con = (CoyoteConnector)emb.createConnector((InetAddress)null, HTTP_PORT, false);
            emb.addConnector(con);
            con = (CoyoteConnector)emb.createConnector((InetAddress)null, HTTPS_PORT, true);
            con.setKeystoreFile("conf/keystore");
            emb.addConnector(con);

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
                                            fatalError("Starting Tomcat", x);
                                            return;
                                        }
                                    }
                                }
                                if (i == NUM_TOMCAT_RETRIES)
                                    fatalError("Unable to start Tomcat after " +
                                              NUM_TOMCAT_RETRIES
                                              + " tries, giving up",
                                              null);
                            }
                        };
                    (new Thread(tryAgain, "Tomcat starter")).start();
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
}
