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

class MvvmClassLoader extends URLClassLoader
{
    private final Set<URL> resources = new HashSet<URL>();

    private final File toolboxDir;

    MvvmClassLoader(URL[] urls, ClassLoader parent, File toolboxDir)
    {
        super(urls, parent);

        this.toolboxDir = toolboxDir;
        refreshToolbox();
    }

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
