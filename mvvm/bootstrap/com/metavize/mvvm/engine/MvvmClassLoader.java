/*
 * Copyright (c) 2003, 2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: MvvmClassLoader.java,v 1.1 2005/01/14 07:59:45 amread Exp $
 */

package com.metavize.mvvm.engine;

import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.cert.Certificate;


/**
 * XXX This ClassLoader is not really needed anymore, but remains for
 * compatibility, it will probably be removed soon.
 *
 * @author <a href="mailto:amread@nyx.net">Aaron Read</a>
 * @version 1.0
 */
public class MvvmClassLoader extends URLClassLoader
{
    public static final CodeSource MVVM_CODE_SOURCE;
    static {
        java.net.URL url;
        try {
            // XXX a more apropriate URL (the sar url)?
            url = new java.net.URL("http://localhost/mvvm");
        } catch (java.net.MalformedURLException exn) {
            url = null;
            System.out.println("Warning unexpected exception: " + exn);
        }
        // XXX certificates?
        MVVM_CODE_SOURCE = new CodeSource(url, new Certificate[0]);
    }

    public MvvmClassLoader(URL[] urls, ClassLoader parent)
    {
        super(urls, parent);
    }
}
