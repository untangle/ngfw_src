/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle Networks, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.engine;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Set;

class CasingClassLoader extends URLClassLoader
{
    private final Set<String> resources = new HashSet<String>();

    CasingClassLoader(ClassLoader parent)
    {
        super(new URL[0], parent); // XXX transform-lib ?
    }

    /**
     * Adds resource to classloader. Should be externally synchronized.
     *
     * @param resource to add.
     */
    void addResource(URL resource)
    {
        String resStr = resource.toString();

        if (resources.contains(resStr)) {
            return;
        } else {
            resources.add(resStr);
            addURL(resource);
        }
    }
}
