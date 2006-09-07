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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Configuration;

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

    private static final Map<ClassLoader, WeakReference<SessionFactory>> sessionFactories
        = new WeakHashMap<ClassLoader, WeakReference<SessionFactory>>();

    static SessionFactory makeSessionFactory(ClassLoader cl)
    {
        SessionFactory sessionFactory = null;
        synchronized (sessionFactories) {
            WeakReference<SessionFactory> wr = sessionFactories.get(cl);
            if (null != wr) {
                sessionFactory = wr.get();
            }

            if (null == sessionFactory) {
                sessionFactory = createSessionFactory(cl);
                if (null != sessionFactory) {
                    sessionFactories.put(cl, new WeakReference(sessionFactory));
                }
            }
        }
        return sessionFactory;
    }

    static SessionFactory makeStandaloneSessionFactory(List<JarFile> jfs)
    {
        SessionFactory sessionFactory = null;

        try {
            ShortBus cfg = new ShortBus();
            addAnnotatedClasses(jfs, cfg);

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

    // private methods --------------------------------------------------------

    private static SessionFactory createSessionFactory(ClassLoader cl)
    {
        SessionFactory sessionFactory = null;

        try {
            ShortBus cfg = new ShortBus();
            addAnnotatedClasses(cl, cfg);

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

    private static void addAnnotatedClasses(ClassLoader cl, ShortBus cfg)
    {
        Enumeration<URL> e = null;

        try {
            e = cl.getResources("META-INF/annotated-classes");
        } catch (IOException exn) {
            logger.warn("could not load annotated-classes", exn);
        }

        while (null != e && e.hasMoreElements()) {
            try {
                URL url = e.nextElement();
                InputStream is = url.openStream();
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String line;
                while (null != (line = br.readLine())) {
                    try {
                        Class c = Class.forName(line);
                        cfg.addAnnotatedClass(c);
                    } catch (ClassNotFoundException exn) {
                        logger.warn("skipping unknown class: " + line, exn);
                    }
                }
            } catch (IOException exn) {
                logger.warn("could not read annotated-classes", exn);
            }
        }
    }

    private static void addAnnotatedClasses(List<JarFile> jfs, ShortBus cfg)
    {
        List<URL> urls = new ArrayList<URL>(jfs.size());

        for (JarFile jf : jfs) {
            String urlStr = "file://" + jf.getName();
            try {
                urls.add(new URL(urlStr));
            } catch (MalformedURLException exn) {
                logger.warn("skipping bad url: " + urlStr, exn);
            }
        }

        ClassLoader cl = new URLClassLoader(urls.toArray(new URL[urls.size()]));

        addAnnotatedClasses(cl, cfg);
    }

    private static void addClassLoader(URLClassLoader ucl, ShortBus cfg,
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

    private static void addJar(JarFile jf, ShortBus cfg, Set seen)
    {
        for (Enumeration e = jf.entries(); e.hasMoreElements(); ) {
            ZipEntry je = (ZipEntry)e.nextElement();
            String name = je.getName();
            if (name.endsWith("hbm.xml") && !seen.contains(name)) {
                ZipEntry cache = (ZipEntry)jf.getEntry(name + ".bin");
                if (null == cache) {
                    logger.info("no cached dom for: " + name);
                    addXml(cfg, jf, je);
                } else {
                    logger.info("using cached dom for: " + name);
                    ObjectInputStream oos = null;
                    try {
                        oos = new ObjectInputStream(jf.getInputStream(cache));
                        Document doc = (Document)oos.readObject();
                        cfg.addDom4j(doc);
                    } catch (Exception exn) {
                        logger.warn("could not used cached dom for: " + name);
                        addXml(cfg, jf, je);
                    } finally {
                        if (null != oos) {
                            try {
                                oos.close();
                            } catch (IOException exn) {
                                logger.warn("could not close output", exn);
                            }
                        }
                    }
                }
                seen.add(name);
            }
        }
    }

    private static void addXml(ShortBus cfg, JarFile jf, ZipEntry je)
    {
        InputStream is = null;

        try {
            logger.info("adding mappings for: " + je.getName());
            is = jf.getInputStream(je);
            cfg.addInputStream(is);
        } catch (MappingException exn) {
            logger.warn("bad mappings for: " + je, exn);
        } catch (IOException exn) {
            logger.warn("could not read ZipEntry: " + je, exn);
        } finally {
            if (null != is) {
                try {
                    is.close();
                } catch (IOException exn) {
                    logger.warn("could not close output", exn);
                }
            }
        }
    }

    // private classes --------------------------------------------------------

    private static class ShortBus extends AnnotationConfiguration
    {
        public void addDom4j(Document doc)
        {
            add(doc);
        }
    }
}
