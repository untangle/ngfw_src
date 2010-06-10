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
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

/**
 * ClassLoader that allows us to add new resources as new applications
 * are installed. The bulk of the UVM and all nodes are loaded
 * from this ClassLoader. Tomcat and its ClassLoaders are a child of
 * this ClassLoader.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
class UvmClassLoader extends URLClassLoader
{
    private final Set<URL> resources = new HashSet<URL>();
    private final File toolboxDir;
    private final Logger logger = Logger.getLogger(getClass());

    /**
     * Creates new UvmClassLoader.
     *
     * @param urls non-toolbox urls that are resources for this ClassLoader.
     * @param parent the parent ClassLoader.
     * @param toolboxDir location of the toolbox packages, this
     * directory will be scanned for additional resources to add to
     * the classpath.
     *
     * @see #refreshToolbox()
     */
    UvmClassLoader(URL[] urls, ClassLoader parent, File toolboxDir)
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
            changed |= addFile(f);
        }

        return changed;
    }

    boolean loadRup()
    {
        String uvmLib = System.getProperty("uvm.lib.dir");

        boolean r = false;

        r |= addFile(uvmLib + "/untangle-professional-core-impl/");
        r |= addFile(uvmLib + "/untangle-professional-core-api/");
        r |= addFile(uvmLib + "/untangle-professional-core-localapi/");

        return r;
    }

    boolean loadUvmResource(String name)
    {
        String uvmLib = System.getProperty("uvm.lib.dir");

        return addFile(uvmLib + "/" + name + "/");
    }

    // private methods --------------------------------------------------------

    private boolean addFile(File f)
    {
        try {
            URL url = f.toURI().toURL();
            if (!resources.contains(url)) {
                addURL(url);
                resources.add(url);
                return true;
            } else {
                return false;
            }
        } catch (MalformedURLException exn) {
            logger.warn("could not load: " + f, exn);
            return false;
        }
    }

    private boolean addFile(String fn)
    {
        File f = new File(fn);
        if (f.exists()) {
            logger.info("adding file to classloader: " + fn);
            return addFile(f);
        } else {
            return false;
        }
    }
}
