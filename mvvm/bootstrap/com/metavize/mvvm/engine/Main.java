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
import java.util.Properties;

import org.apache.catalina.Connector;
import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.logger.FileLogger;
import org.apache.catalina.session.StandardManager;
import org.apache.catalina.startup.Embedded;
import org.apache.coyote.tomcat5.CoyoteConnector;
import org.apache.log4j.Logger;
import org.logicalcobwebs.proxool.ProxoolException;
import org.logicalcobwebs.proxool.ProxoolFacade;

public class Main
{
    private static final String INIT_SCHEMA_CMD
        = System.getProperty("bunnicula.home") + "/../../bin/init-schema ";

    public static int HTTP_PORT = 80;
    public static int HTTPS_PORT = 443;

    public static long TOMCAT_SLEEP_TIME = 20 * 1000; // 20 seconds
    public static int NUM_TOMCAT_RETRIES = 15; //  5 minutes total

    private static String MVVM_LOCAL_CONTEXT_CLASSNAME
        = "com.metavize.mvvm.engine.MvvmLocalContextImpl";

    static {
        MvvmRepositorySelector.get().init("");
    }

    private Embedded emb = null;

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

    private String jdbcUrl;

    private Main() { }

    // XXX get rid of all these throws
    public static final void main(String[] args) throws Exception
    {
        SchemaUtil.initSchema("mvvm");

        new Main().init();
    }

    // XXX get rid of all these throws
    private void init() throws Exception
    {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                public void run() { destroy(); }
            }));

        initJdbcPool();

        ClassLoader cl = getClass().getClassLoader();
        Thread.currentThread().setContextClassLoader(cl);

        logger.info("setting up properties");
        setProperties();
        logger.info("starting mvvm");
        try {
            startMvvm();
        } catch (Exception exn) {
            logger.warn("could not start mvvm", exn);
            System.exit(1);
        }
        System.out.println("MVVM startup complete: \"Today vegetables...tomorrow the world!\"");
        logger.info("restarting transforms and socket invoker");
        restartTransfoms();
        logger.info("starting tomcat");
        startTomcat();
        System.out.println("MVVM postInit complete");
    }

    private void destroy()
    {
        try {
            if (emb != null) {
                emb.stop();
            }
        } catch (LifecycleException exn) {
            logger.debug(exn);
        }

        mvvmContext.doDestroy();
        System.out.println("MVVM shutdown complete.");
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

        System.setProperty("derby.system.home", bunniculaHome + "/db");

        logger.info("bunnicula.home        " + bunniculaHome);
        logger.info("bunnicula.lib.dir     " + bunniculaLib);
        logger.info("bunnicula.toolbox.dir " + bunniculaToolbox);
        logger.info("bunnicula.log.dir     " + bunniculaLog);
        logger.info("bunnicula.data.dir    " + bunniculaData);
        logger.info("bunnicula.web.dir     " + bunniculaWeb);
        logger.info("bunnicula.conf.dir    " + bunniculaConf);

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
                .getMethod("localContext").invoke(null);

            mvvmContext.doInit(this);

        } finally {
            Thread.currentThread().setContextClassLoader(oldCl);
            // restored classloader ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        }
    }

    private void initJdbcPool()
    {
        logger.info("Initializing Proxool");
        try {
            Class.forName("org.logicalcobwebs.proxool.ProxoolDriver");
        } catch (ClassNotFoundException exn) {
            throw new RuntimeException("could not load Proxool", exn);
        }
        Properties info = new Properties();
        info.setProperty("proxool.maximum-connection-count", "50");
        info.setProperty("proxool.house-keeping-test-sql", "select CURRENT_DATE");
        /* XXX not for production: */
        info.setProperty("proxool.statistics", "1m,15m,1d");
        info.setProperty("user", "metavize");
        info.setProperty("password", "foo");
        String alias = "mvvm";
        String driverClass = "org.postgresql.Driver";
        String driverUrl = "jdbc:postgresql://localhost/mvvm";
        jdbcUrl = "proxool." + alias + ":" + driverClass + ":" + driverUrl;
        try {
            ProxoolFacade.registerConnectionPool(jdbcUrl, info);
        } catch (ProxoolException exn) {
            logger.debug("could not set up Proxool", exn);
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

    // XXX exception handling
    private void startTomcat() throws Exception
    {
        // jdi 8/30/04 -- canonical host name depends on ordering of /etc/hosts
        String hostname = "localhost";

        // set default logger and realm
        FileLogger fileLog = new FileLogger();
        fileLog.setDirectory(bunniculaLog);
        fileLog.setPrefix("tomcat");
        fileLog.setSuffix(".log");
        fileLog.setTimestamp(true);

        emb = new Embedded(fileLog, new MvvmRealm());
        emb.setCatalinaHome(bunniculaHome);

        // create an Engine
        Engine baseEngine = emb.createEngine();

        // set Engine properties
        baseEngine.setName("tomcat");
        baseEngine.setDefaultHost(hostname);

        // create Host
        StandardHost baseHost = (StandardHost)emb
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

        ctx = emb.createContext("/session-dumper",
                                bunniculaWeb + "/session-dumper");
        mgr = new StandardManager();
        mgr.setPathname(null); /* disable session persistence */
        ctx.setManager(mgr);
        baseHost.addChild(ctx);

        ctx = emb.createContext("/webstart", bunniculaWeb + "/webstart");
        mgr = new StandardManager();
        mgr.setPathname(null); /* disable session persistence */
        ctx.setManager(mgr);
        baseHost.addChild(ctx);

        ctx = emb.createContext("/reports", bunniculaWeb + "/reports");
        mgr = new StandardManager();
        mgr.setPathname(null); /* disable session persistence */
        ctx.setManager(mgr);
        baseHost.addChild(ctx);

        // XXX for internal use only
        ctx = emb.createContext("/proxool", bunniculaWeb + "/proxool");
        mgr = new StandardManager();
        mgr.setPathname(null); /* disable session persistence */
        ctx.setManager(mgr);
        baseHost.addChild(ctx);

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
