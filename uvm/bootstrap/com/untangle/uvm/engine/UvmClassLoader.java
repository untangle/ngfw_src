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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Set;

/**
 * ClassLoader that allows us to add new resources as new applications
 * are installed. The bulk of the MVVM and all transforms are loaded
 * from this ClassLoader. Tomcat and its ClassLoaders are a child of
 * this ClassLoader.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
class MvvmClassLoader extends URLClassLoader
{
    private final Set<URL> resources = new HashSet<URL>();
    private final File toolboxDir;

    /**
     * Creates new MvvmClassLoader.
     *
     * @param urls non-toolbox urls that are resources for this ClassLoader.
     * @param parent the parent ClassLoader.
     * @param toolboxDir location of the toolbox packages, this
     * directory will be scanned for additional resources to add to
     * the classpath.
     *
     * @see #refreshToolbox()
     */
    MvvmClassLoader(URL[] urls, ClassLoader parent, File toolboxDir)
    {
        super(urls, parent);

        this.toolboxDir = toolboxDir;
        refreshToolbox();
    }

    /**
     * Check for new applications. Scans <code>toolboxDir</code>
     * adding all directories and jar files not already in the
     * classpath.
     *
     * @return true if the classpath changed.
     */
    boolean refreshToolbox()
    {
        boolean changed = false;

        for (File f : toolboxDir.listFiles()) {
            URL url;
            try {
                url = f.toURL();
            } catch (MalformedURLException exn) {
                throw new RuntimeException(exn);
            }
            if (!resources.contains(url)) {
                addURL(url);
                resources.add(url);
                changed = true;
            }
        }

        return changed;
    }
}
