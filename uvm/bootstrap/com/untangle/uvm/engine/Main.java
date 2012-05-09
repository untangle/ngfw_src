/*
 * $Id$
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

/**
 * Bootstraps the UVM. Access to the Main object should be protected.
 *
 * Properties defined by this class:
 * <ul>
 * <li>uvm.home - home of uvm, usually
 * <code>/usr/share/uvm</code>.</li>
 * <li>uvm.lib.dir - uvm libraries.</li>
 * <li>uvm.toolbox.dir - node jars.</li>
 * <li>uvm.log.dir - log files.</li>
 * <li>uvm.web.dir - servlet directories.</li>
 * <li>uvm.conf.dir - configuration files.</li>
 * <li>uvm.settings.dir - settings files</li>
 * <li>uvm.tmp.dir - temporary files.</li>
 * <li>uvm.skins.dir - skins files.</li>
 * <li>uvm.lang.dir - languages resources files.</li>
 */
public class Main
{
    private static final String UVM_CONTEXT_CLASSNAME = "com.untangle.uvm.engine.UvmContextImpl";

    private static Main MAIN;

    private final Logger logger = Logger.getLogger(getClass());

    private UvmClassLoader uvmCl;
    private UvmContextBase uvmContext;

    // constructor -------------------------------------------------------------

    private Main()
    {
        /**
         * Configure the basic logging setup
         */
        LogManager.setRepositorySelector(UvmRepositorySelector.instance(), new Object());
        UvmRepositorySelector.instance().setLoggingUvm();
    }

    /**
     * The fun starts here.
     */
    public static final void main(String[] args) throws Exception
    {
        System.out.println("UVM starting...");
        
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
        return uvmCl.refreshToolbox();
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
            if (x != null) {
                logger.error("FATAL ERROR: " + throwingLocation, x);
                System.err.println("FATAL ERROR: " + throwingLocation);
                System.err.println("Throwable: " + x.getMessage());
                x.printStackTrace(System.err);
            } else {
                logger.error("FATAL ERROR: " + throwingLocation);
                System.err.println("FATAL ERROR: " + throwingLocation);
            }
        } catch (Throwable y) {
            System.out.println("Throwable: " + x.getMessage());
            x.printStackTrace();
        } finally {
            System.exit(-1);
        }
    }

    public boolean loadUvmResource(String name)
    {
        return uvmCl.loadUvmResource(name);
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
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {public void run() { destroy(); }}));

        logger.info("Setting up properties...");
        setProperties();
        
        logger.info("Starting uvm...");

        try {
            configureClassLoader();
            startUvm();
        } catch (Throwable exn) {
            fatalError("could not start uvm", exn);
        }

        System.out.println("UVM startup complete: \"Today vegetables...tomorrow the world!\"");
        
        logger.info("Restarting nodes...");

        restartNodes();

        System.out.println("UVM postInit complete");
    }

    private void configureClassLoader() throws Exception
    {
        List<URL> urls = new ArrayList<URL>();

        /* Add everything in lib */
        File uvmLibDir = new File(System.getProperty("uvm.lib.dir"));
        for (File f : uvmLibDir.listFiles()) {
            URL url = f.toURI().toURL();
            urls.add(url);
        }

        urls.add(new URL("file://" + System.getProperty("uvm.lang.dir") + "/"));
        uvmCl = new UvmClassLoader(urls.toArray(new URL[urls.size()]), getClass().getClassLoader(), new File(System.getProperty("uvm.toolbox.dir")));
        Thread.currentThread().setContextClassLoader(uvmCl);
    }
    
    private void destroy()
    {
        uvmContext.doDestroy();
        System.out.println("UVM shutdown complete.");
    }

    private void setProperties() throws Exception
    {
        String uvmHome = System.getProperty("uvm.home");

        String uvmLib = uvmHome + "/lib";
        System.setProperty("uvm.lib.dir", uvmLib);
        String uvmBin = uvmHome + "/bin";
        System.setProperty("uvm.bin.dir", uvmBin);
        String uvmToolbox = uvmHome + "/toolbox";
        System.setProperty("uvm.toolbox.dir", uvmToolbox);
        String uvmLog = "/var/log/uvm";
        System.setProperty("uvm.log.dir", uvmLog);
        String uvmWeb = uvmHome + "/web";
        System.setProperty("uvm.web.dir", uvmWeb);
        String uvmConf = uvmHome + "/conf";
        System.setProperty("uvm.conf.dir", uvmConf);
        String uvmSettings = uvmHome + "/settings";
        System.setProperty("uvm.settings.dir", uvmSettings);
        String uvmTmp = "/tmp";
        System.setProperty("uvm.tmp.dir", uvmTmp);
        String uvmSkins = "/var/www/skins";
        System.setProperty("uvm.skins.dir", uvmSkins);
        String uvmLang = uvmHome + "/lang";
        System.setProperty("uvm.lang.dir", uvmLang);

        logger.info("uvm.home         " + uvmHome);
        logger.info("uvm.lib.dir      " + uvmLib);
        logger.info("uvm.toolbox.dir  " + uvmToolbox);
        logger.info("uvm.log.dir      " + uvmLog);
        logger.info("uvm.web.dir      " + uvmWeb);
        logger.info("uvm.conf.dir     " + uvmConf);
        logger.info("uvm.settings.dir " + uvmSettings);
        logger.info("uvm.tmp.dir      " + uvmTmp);
        logger.info("uvm.skins.dir    " + uvmSkins);
    }

    private void startUvm() throws Exception
    {
        uvmContext = (UvmContextBase)uvmCl.loadClass(UVM_CONTEXT_CLASSNAME).getMethod("context").invoke(null);
        uvmContext.doInit(this);
    }

    private void restartNodes() throws Exception
    {
        uvmContext.doPostInit();
    }
}
