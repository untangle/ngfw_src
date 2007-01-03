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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Iterator;
import java.util.WeakHashMap;

import org.apache.log4j.Hierarchy;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.LoggerRepository;
import org.apache.log4j.spi.RepositorySelector;
import org.apache.log4j.xml.DOMConfigurator;
import org.apache.tools.ant.filters.ReplaceTokens;
import org.apache.log4j.spi.RootLogger;

/**
 * An implementation of the Log4j RepositorySelector that looks for local
 * log4j.xml files
 * @author Scott.Stark@jboss.org
 * @version $Revision: 1.1 $
 */
public class MvvmRepositorySelector implements RepositorySelector
{
    public static final String TRANLOGFILENAME_TOKEN = "TranLogFileName";
    public static final String TRANUSERLOGFILENAME_TOKEN = "TranUserLogFileName";

    public static final long WATCH_INTERVAL = 30000; // 30 seconds

    // key: current thread's ContextClassLoader,
    // value: Hierarchy instance
    private WeakHashMap ht;

    private static MvvmRepositorySelector theSelector;

    private static Object guard = new Object();

    private static MVHierarchy nullHierarchy = null;

    private MvvmRepositorySelector() {
        System.out.println("Creating MvvmRepositorySelector");
        ht = new WeakHashMap();
    }

    public static synchronized MvvmRepositorySelector get() {
        if (theSelector == null) {
            theSelector = new MvvmRepositorySelector();
            LogManager.setRepositorySelector(theSelector, guard);
        }
        return theSelector;
    }

    // This one is used for generic, and for mvvm. (not transforms)
    public void init(String name) {
        init(name, null, null);
    }

    // This one is used for transforms (not mvvm or others).
    public void init(String name, String tranLogFileName, String tranUserLogFileName)
    {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        System.out.println("Initializing MvvmRepositorySelector for name " + name
                           + ", cl " + cl);
        MVHierarchy hierarchy = (MVHierarchy) ht.get(cl);
        if (hierarchy == null || !hierarchy.hasBeenInitialized) {
            hierarchy = new MVHierarchy(new RootLogger((Level)Level.DEBUG), name,
                                        tranLogFileName, tranUserLogFileName);
            ht.put(cl, hierarchy);
            if (name.equals("")) {
                nullHierarchy = hierarchy;
            }

            // Locate the log4j.xml or log4j.properties config (but only for
            // non-toplevel loggers)
            InputStream is = findConfig(name, hierarchy);
            if( is == null ) {
                if (name != null && !name.equals("")) {
                    LogLog.warn("Failed to find any log4j.xml config for name "
                                + name);
                    throw new IllegalStateException("Failed to find log4j.xml");
                }
                // Otherwise we wait for the next init().
            } else {
                hierarchy.hasBeenInitialized = true;
                doConfiguration(is, hierarchy);
            }
        } else {
            LogLog.warn("Attempted to reinitialize logrepository for cl " + cl);
        }
    }

    public LoggerRepository getLoggerRepository() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        // XXX what to do if contextcl == null
        if (null == cl) {
            cl = getClass().getClassLoader();
        }

        Hierarchy hierarchy = (Hierarchy) ht.get(cl);
        while (hierarchy == null) {
            cl = cl.getParent();
            if (cl == null)
                break;
            hierarchy = (Hierarchy) ht.get(cl);
        }
        if (hierarchy == null) {
            LogLog.error("No logger hierarchy found for cl " + cl
                         + " or parents");
            System.err.println("stderr: No logger hierarchy found for cl " + cl
                               + " or parents, returning root");
            return nullHierarchy;

        }

        return hierarchy;
    }

    /**
     * The Container should remove the entry when the web-application
     * is removed or restarted.
     * */
    public static void destroy() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        get().ht.remove(cl);
    }

    public void reconfigureAll() {
        Thread ct = Thread.currentThread();
        ClassLoader oldCl = ct.getContextClassLoader();
        for (Iterator iter = ht.keySet().iterator(); iter.hasNext();) {
            ClassLoader cl = (ClassLoader) iter.next();
            MVHierarchy h = (MVHierarchy) ht.get(cl);
            if (h == null) {
                // Classloader has been collected.
                continue;
            }
            System.out.println("Resetting log configuration for class loader " + cl);
            ct.setContextClassLoader(cl);
            try {
                InputStream is = findConfig(h.name, h);
                doConfiguration(is, h);
            } finally {
                ct.setContextClassLoader(oldCl);
            }
        }
    }


    private void doConfiguration(InputStream is, MVHierarchy hierarchy) {
        DOMConfigurator configurator = new DOMConfigurator();
        if (hierarchy.tranLogFileName != null) {
            // A Transform.
            ReplaceTokens rts = new ReplaceTokens(new InputStreamReader(is));
            ReplaceTokens.Token tranLogFileNameToken = new ReplaceTokens.Token();
            tranLogFileNameToken.setKey(TRANLOGFILENAME_TOKEN);
            tranLogFileNameToken.setValue(hierarchy.tranLogFileName);
            rts.addConfiguredToken(tranLogFileNameToken);
            ReplaceTokens.Token tranUserLogFileNameToken = new ReplaceTokens.Token();
            tranUserLogFileNameToken.setKey(TRANUSERLOGFILENAME_TOKEN);
            tranUserLogFileNameToken.setValue(hierarchy.tranUserLogFileName);
            rts.addConfiguredToken(tranUserLogFileNameToken);
            configurator.doConfigure(rts, hierarchy);
        } else {
            configurator.doConfigure(is, hierarchy);
        }
    }

    private static InputStream findConfig(String name, Hierarchy hierarchy)
    {
        ClassLoader tcl = Thread.currentThread().getContextClassLoader();
        InputStream is = null;

        // First look for a resource: "name / log4j-suffix(name).xml"
        String prefix = "";
        String suffix = name;
        int dot = name.lastIndexOf('.');
        if( dot >= 0 ) {
            prefix = name.substring(0, dot);
            suffix = name.substring(dot+1);
        }
        prefix = prefix.replace('.', '/');

        String log4jxml = prefix + "/log4j-" + suffix + ".xml";
        URL resURL = tcl.getResource(log4jxml);
        if( resURL != null ) {
            try {
                is = resURL.openStream();
            } catch(IOException e) {
            }
            System.out.println("For " + name + " found resURL: " + resURL);
            return is;
        }

        // Next look for log4j-resource name.xml
        if (prefix.equals("")) {
            log4jxml = "log4j-" + suffix + ".xml";
            resURL = tcl.getResource(log4jxml);
            if( resURL != null ) {
                try {
                    is = resURL.openStream();
                } catch(IOException e) {
                }
                System.out.println("For " + name + " found resURL: " + resURL);
                return is;
            }
        }

        // Next look for resource name / + log4j.xml
        log4jxml = prefix + "/log4j.xml";
        resURL = tcl.getResource(log4jxml);
        if( resURL != null ) {
            try {
                is = resURL.openStream();
            } catch(IOException e) {
            }
            System.out.println("For " + name + " found resURL: " + resURL);
            return is;
        }

        // Next look for just the log4j.xml res
        log4jxml = "log4j.xml";
        resURL = tcl.getResource(log4jxml);
        if( resURL != null ) {
            try {
                is = resURL.openStream();
            } catch(IOException e) {
            }
            System.out.println("For " + name + " found resURL: " + resURL);
            return is;
        }

        // Shit.
        return null;
    }

    class MVHierarchy extends Hierarchy {

        String name;
        String tranLogFileName;
        String tranUserLogFileName;
        boolean hasBeenInitialized = false;

        MVHierarchy(Logger root, String name, String tranLogFileName, String tranUserLogFileName) {
            super(root);
            this.name = name;
            this.tranLogFileName = tranLogFileName;
            this.tranUserLogFileName = tranUserLogFileName;
       }
    }
}
