/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.engine;

import java.net.URL;
import java.net.URLClassLoader;

class CasingClassLoader extends URLClassLoader
{
    CasingClassLoader(ClassLoader parent)
    {
        super(new URL[0], parent); // XXX transform-lib ?
    }

    void addResources(URL[] resources)
    {
        for (URL resource : resources) {
            addURL(resource);
        }
    }
}
