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

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import net.sf.hibernate.HibernateException;
import net.sf.hibernate.MappingException;
import net.sf.hibernate.SessionFactory;
import net.sf.hibernate.cfg.Configuration;
import org.apache.log4j.Logger;

/**
 * These are internal utility methods for internal use by the MVVM or
 * other top-level tools (reporting).
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 */
class Util
{
    private static final Logger logger = Logger.getLogger(Util.class);

    static SessionFactory makeSessionFactory(ClassLoader cl)
    {
        SessionFactory sessionFactory = null;

        try {
            Configuration cfg = new Configuration();

            Set seen = new HashSet();
            while (null != cl) {
                if (cl instanceof URLClassLoader) {
                    addClassLoader((URLClassLoader)cl, cfg, seen);
                }
                cl = cl.getParent();
            }

            long t0 = System.currentTimeMillis();
            sessionFactory = cfg.buildSessionFactory();
            long t1 = System.currentTimeMillis();
            logger.info("session factory in " + (t1 - t0) + " millis");
        } catch (HibernateException exn) {
            logger.warn("could not create SessionFactory", exn);
        }

        return sessionFactory;
    }

    static SessionFactory makeStandaloneSessionFactory(List<JarFile> jfs)
    {
        SessionFactory sessionFactory = null;

        try {
            Configuration cfg = new Configuration();

            Set seen = new HashSet();
            for (JarFile jf : jfs) {
                addJar(jf, cfg, seen);
            }

            long t0 = System.currentTimeMillis();
            sessionFactory = cfg.buildSessionFactory();
            long t1 = System.currentTimeMillis();
            logger.info("session factory in " + (t1 - t0) + " millis");
        } catch (HibernateException exn) {
            logger.warn("could not create SessionFactory", exn);
        }

        return sessionFactory;
    }

    private static void addClassLoader(URLClassLoader ucl, Configuration cfg,
                                       Set seen)
    {
        Thread ct = Thread.currentThread();
        ClassLoader oldCl = ct.getContextClassLoader();

        // entering URLClassLoader ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        ct.setContextClassLoader(ucl);
        try {
            URL[] urls = ucl.getURLs();

            for (int i = 0; i < urls.length; i++) {
                String f = urls[i].getFile();
                if (!urls[i].getProtocol().equals("file")
                    || !f.endsWith("jar") && !f.endsWith("mar")) {
                    continue;
                }

                try {
                    addJar(new JarFile(urls[i].getPath()), cfg, seen);
                } catch (IOException exn) {
                    logger.warn("could not add mappings for: " + urls[i], exn);
                }
            }
        } finally {
            ct.setContextClassLoader(oldCl);
            // left URLClassLoader ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        }
    }

    private static void addJar(JarFile jf, Configuration cfg, Set seen)
    {
        for (Enumeration e = jf.entries(); e.hasMoreElements(); ) {
            JarEntry je = (JarEntry)e.nextElement();
            String name = je.getName();
            if (name.endsWith("hbm.xml") && !seen.contains(name)) {
                try {
                    logger.info("adding mappings for: " + name);
                    cfg.addInputStream(jf.getInputStream(je));
                } catch (MappingException exn) {
                    logger.warn("bad mappings for: " + je, exn);
                } catch (IOException exn) {
                    logger.warn("could not read JarEntry: " + je, exn);
                }
                seen.add(name);
            }
        }
    }

}
