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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarFile;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;

/**
 * These are internal utility methods for internal use by the UVM or
 * other top-level tools (reporting).
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
class Util
{
    private static final Logger logger = Logger.getLogger(Util.class);

    static SessionFactory makeSessionFactory(ClassLoader cl)
    {
        SessionFactory sessionFactory = null;

        try {
            AnnotationConfiguration cfg = new AnnotationConfiguration();
            addAnnotatedClasses(cl, cfg);

            long t0 = System.currentTimeMillis();
            sessionFactory = cfg.buildSessionFactory();
            long t1 = System.currentTimeMillis();
            logger.info("session factory in " + (t1 - t0) + " millis");
        } catch (HibernateException exn) {
            logger.warn("could not create SessionFactory", exn);
        }

        return sessionFactory;
    }

    static SessionFactory makeStandaloneSessionFactory(List<File> classDirs, List<JarFile> jfs)
    {
        SessionFactory sessionFactory = null;

        try {
            AnnotationConfiguration cfg = new AnnotationConfiguration();
            addAnnotatedClasses(classDirs, jfs, cfg);

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

    @SuppressWarnings("unchecked")
	private static void addAnnotatedClasses(ClassLoader cl,
                                            AnnotationConfiguration cfg)
    {
        Enumeration<URL> e = null;

        try {
            e = cl.getResources("META-INF/annotated-classes");
        } catch (IOException exn) {
            logger.warn("could not load annotated-classes", exn);
        }

        while (null != e && e.hasMoreElements()) {
            Thread t = Thread.currentThread();
            ClassLoader oldCl = t.getContextClassLoader();
            try {
                t.setContextClassLoader(cl);
                URL url = e.nextElement();
                InputStream is = url.openStream();
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String line;
                while (null != (line = br.readLine())) {
                    try {
                        /* skip blank lines */
                        line = line.trim();
                        if (line.length() == 0) {
                            continue;
                        }
                        Class c = cl.loadClass(line);
                        cfg.addAnnotatedClass(c);
                    } catch (ClassNotFoundException exn) {
                        logger.warn("skipping unknown class: '" + line + "'", exn);
                    }
                }
            } catch (IOException exn) {
                logger.warn("could not read annotated-classes", exn);
            } finally {
                t.setContextClassLoader(oldCl);
            }
        }
    }

    private static void addAnnotatedClasses(List<File> classDirs, List<JarFile> jfs,
                                            AnnotationConfiguration cfg)
    {
        List<URL> urls = new ArrayList<URL>();

        if (classDirs != null) {
            for (File cd : classDirs) {
                String urlStr = "file://" + cd.getPath();
                try {
                    urls.add(new URL(urlStr));
                } catch (MalformedURLException exn) {
                    logger.warn("skipping bad url: " + urlStr, exn);
                }
            }
        }

        if (jfs != null) {
            for (JarFile jf : jfs) {
                String urlStr = "file://" + jf.getName();
                try {
                    urls.add(new URL(urlStr));
                } catch (MalformedURLException exn) {
                    logger.warn("skipping bad url: " + urlStr, exn);
                }
            }
        }

        ClassLoader cl = new URLClassLoader(urls.toArray(new URL[urls.size()]));

        addAnnotatedClasses(cl, cfg);
    }
}
