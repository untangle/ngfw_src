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

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.untangle.uvm.logging.UvmRepositorySelector;

/**
 * Bootstraps the UVM. Access to the Main object should be protected.
 *
 * Properties defined by this class:
 * <ul>
 * <li>bunnicula.home - home of uvm, usually
 * <code>/usr/share/uvm</code>.</li>
 * <li>bunnicula.lib.dir - uvm libraries.</li>
 * <li>bunnicula.toolbox.dir - node jars.</li>
 * <li>bunnicula.log.dir - log files.</li>
 * <li>bunnicula.data.dir - data files.</li>
 * <li>bunnicula.db.dir - database files.</li>
 * <li>bunnicula.web.dir - servlet directories.</li>
 * <li>bunnicula.conf.dir - configuration files, added to classpath.</li>
 * <li>bunnicula.tmp.dir - temporary files.</li>
 * <li>bunnicula.skins.dir - skins files.</li>
 * <li>bunnicula.lang.dir - languages resources files.</li>
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class Main
{
    private static final String UVM_LOCAL_CONTEXT_CLASSNAME
        = "com.untangle.uvm.engine.UvmContextImpl";

    private static Main MAIN;

    private final SchemaUtil schemaUtil = new SchemaUtil();

    private final Logger logger = Logger.getLogger(getClass());

    private UvmClassLoader mcl;
    private UvmContextBase uvmContext;

    // constructor -------------------------------------------------------------

    private Main()
    {
        schemaUtil.initSchema("settings", "uvm");
        LogManager.setRepositorySelector(UvmRepositorySelector.selector(),
                                         new Object());
    }

    // public static methods ---------------------------------------------------

    /**
     * The fun starts here.
     */
    public static final void main(String[] args) throws Exception
    {
        synchronized (Main.class) {
            if (null == MAIN) {
                MAIN = new Main();
                MAIN.init();
            }
        }
    }

    public static Main getMain()
    {
        return MAIN;
    }

    // public methods ----------------------------------------------------------

    /**
     * @see UvmClassLoader.refreshToolbox()
     */
    public boolean refreshToolbox()
    {
        return mcl.refreshToolbox();
    }

    /**
     * <code>fatalError</code> can be called to indicate that a fatal
     * error has occured and that the UVM *must* restart (or
     * otherwise recover) itself.  One example is an OutOfMemory
     * error.
     *
     * @param x a <code>Throwable</code> giving the related/causing
     * exception, if any, otherwise null.
     */
    public void fatalError(String throwingLocation, Throwable x)
    {
        try {
            System.err.println("FATAL ERROR: " + throwingLocation);
            if (x != null) {
                System.err.println("Throwable: " + x.getMessage());
                x.printStackTrace(System.err);
            }
        } catch (Throwable y) {
            System.out.println("Throwable: " + x.getMessage());
            x.printStackTrace();
        } finally {
            System.exit(-1);
        }
    }

    /**
     * Provides UvmContext access to {@link SchemaUtil}.
     */
    public SchemaUtil schemaUtil()
    {
        return schemaUtil;
    }

    /**
     * Attempt to load premium libraries.
     */
    public boolean loadRup()
    {
        return mcl.loadRup();
    }

    public boolean loadUvmResource(String name)
    {
        return mcl.loadUvmResource(name);
    }

    public Map<String, String> getTranslations(String module)
    {
        return uvmContext.getTranslations(module);
    }

    public String getCompanyName()
    {
        return uvmContext.getCompanyName();
    }

    // private methods ---------------------------------------------------------

    private void init() throws Exception
    {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                public void run() { destroy(); }
            }));

        ClassLoader cl = getClass().getClassLoader();
        Thread.currentThread().setContextClassLoader(cl);

        logger.info("setting up properties");
        setProperties();

        logger.info("starting uvm");
        try {
            startUvm();
        } catch (Throwable exn) {
            fatalError("could not start uvm", exn);
        }
        System.out.println("UVM startup complete: \"Today vegetables...tomorrow the world!\"");
        logger.info("restarting nodes and socket invoker");
        restartNodes();
        System.out.println("UVM postInit complete");
    }

    private void destroy()
    {
        uvmContext.doDestroy();
        try {
            DataSourceFactory.factory().destroy();
        } catch (SQLException exn) {
            logger.warn("could not destory DataSourceFactory", exn);
        }
        System.out.println("UVM shutdown complete.");
    }

    private void setProperties() throws Exception
    {
        String bunniculaHome = System.getProperty("bunnicula.home");

        String bunniculaLib = bunniculaHome + "/lib";
        System.setProperty("bunnicula.lib.dir", bunniculaLib);
        String bunniculaBin = bunniculaHome + "/bin";
        System.setProperty("bunnicula.bin.dir", bunniculaBin);
        String bunniculaToolbox = bunniculaHome + "/toolbox";
        System.setProperty("bunnicula.toolbox.dir", bunniculaToolbox);
        String bunniculaLog = bunniculaHome + "/log";
        System.setProperty("bunnicula.log.dir", bunniculaLog);
        String bunniculaData = bunniculaHome + "/data";
        System.setProperty("bunnicula.data.dir", bunniculaData);
        String bunniculaDb = bunniculaHome + "/db";
        System.setProperty("bunnicula.db.dir", bunniculaDb);
        String bunniculaWeb = bunniculaHome + "/web";
        System.setProperty("bunnicula.web.dir", bunniculaWeb);
        String bunniculaConf = bunniculaHome + "/conf";
        System.setProperty("bunnicula.conf.dir", bunniculaConf);
        String bunniculaTmp = bunniculaHome + "/tmp";
        System.setProperty("bunnicula.tmp.dir", bunniculaTmp);
        String bunniculaSkins = "/var/www/skins";
        System.setProperty("bunnicula.skins.dir", bunniculaSkins);
        String bunniculaLang = bunniculaHome + "/lang";
        System.setProperty("bunnicula.lang.dir", bunniculaLang);

        logger.info("bunnicula.home        " + bunniculaHome);
        logger.info("bunnicula.lib.dir     " + bunniculaLib);
        logger.info("bunnicula.toolbox.dir " + bunniculaToolbox);
        logger.info("bunnicula.log.dir     " + bunniculaLog);
        logger.info("bunnicula.data.dir    " + bunniculaData);
        logger.info("bunnicula.db.dir    " + bunniculaData);
        logger.info("bunnicula.web.dir     " + bunniculaWeb);
        logger.info("bunnicula.conf.dir    " + bunniculaConf);
        logger.info("bunnicula.tmp.dir     " + bunniculaTmp);
        logger.info("bunnicula.skins.dir     " + bunniculaSkins);

        File f = new File(bunniculaConf + "/uvm.properties");
        if (f.exists()) {
            logger.info("Loading " + f);
            System.getProperties().load(new FileInputStream(f));
        } else {
            logger.warn("Could not find " + f);
        }
    }

    private void startUvm() throws Exception
    {
        List<URL> urls = new ArrayList();
        String bunniculaLib = System.getProperty("bunnicula.lib.dir");
        urls.add(new URL("file://" + bunniculaLib + "/untangle-libuvm-impl/"));
        urls.add(new URL("file://" + bunniculaLib + "/untangle-libuvm-api/"));
        urls.add(new URL("file://" + bunniculaLib + "/untangle-libuvm-localapi/"));
        urls.add(new URL("file://" + bunniculaLib + "/untangle-libuvm-reporting/"));
        urls.add(new URL("file://" + bunniculaLib + "/jvector-impl/"));
        urls.add(new URL("file://" + bunniculaLib + "/jnetcap-impl/"));
        String bunniculaLang = System.getProperty("bunnicula.lang.dir");
        urls.add(new URL("file://" + bunniculaLang + "/"));

        String bunniculaToolbox = System.getProperty("bunnicula.toolbox.dir");
        mcl = new UvmClassLoader(urls.toArray(new URL[urls.size()]),
                                  getClass().getClassLoader(),
                                  new File(bunniculaToolbox));

        ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
        try {
            // Entering UVM ClassLoader ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            Thread.currentThread().setContextClassLoader(mcl);

            uvmContext = (UvmContextBase)mcl
                .loadClass(UVM_LOCAL_CONTEXT_CLASSNAME)
                .getMethod("context").invoke(null);

            uvmContext.doInit(this);
        } finally {
            Thread.currentThread().setContextClassLoader(oldCl);
            // restored classloader ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        }
    }

    private void restartNodes() throws Exception
    {
        ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
        try {
            // Entering UVM ClassLoader ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            Thread.currentThread().setContextClassLoader(mcl);

            uvmContext.doPostInit();

        } finally {
            Thread.currentThread().setContextClassLoader(oldCl);
            // restored classloader ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        }
    }
}
