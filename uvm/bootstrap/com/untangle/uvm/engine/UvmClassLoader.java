/**
 * $Id: UvmClassLoader.java 34425 2013-03-31 18:35:45Z dmorris $
 */
package com.untangle.uvm.engine;

import java.io.File;
import java.net.MalformedURLException;
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
 */
class UvmClassLoader extends URLClassLoader
{
    private final Set<URL> resources = new HashSet<URL>();
    private final File libsDir;
    private final Logger logger = Logger.getLogger(getClass());

    /**
     * Creates new UvmClassLoader.
     *
     * @param urls non-libs urls that are resources for this ClassLoader.
     * @param parent the parent ClassLoader.
     * @param libsDir location of the libs packages, this
     * directory will be scanned for additional resources to add to
     * the classpath.
     *
     * @see #refreshLibs()
     */
    UvmClassLoader(URL[] urls, ClassLoader parent, File libsDir)
    {
        super(urls, parent);

        this.libsDir = libsDir;
        refreshLibs();
    }

    /**
     * Check for new applications. Scans <code>libsDir</code>
     * adding all directories and jar files not already in the
     * classpath.
     *
     * @return true if the classpath changed.
     */
    boolean refreshLibs()
    {
        boolean changed = false;

        for (File f : libsDir.listFiles()) {
            changed |= addFile(f);
        }

        return changed;
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
