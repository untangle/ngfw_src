/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: TransformClassLoader.java,v 1.5 2005/01/27 09:53:35 amread Exp $
 */

package com.metavize.mvvm.engine;

import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.Principal;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;

import com.metavize.mvvm.security.Tid;

/**
 * XXX This ClassLoader is not really needed anymore, but remains for
 * compatibility, it will probably be removed soon.
 *
 * @author <a href="mailto:amread@nyx.net">Aaron Read</a>
 * @version 1.0
 */
public class TransformClassLoader extends URLClassLoader
{
    ProtectionDomain protectionDomain;

    TransformClassLoader(Tid tid, URL[] urls, ClassLoader parent)
    {
        super(urls, parent);

        // XXX append TID to url?
        // XXX append real certificates
        CodeSource codeSource = new CodeSource(urls[0], new Certificate[0]);
        Principal[] principals = new Principal[] { tid };
        protectionDomain = new ProtectionDomain(codeSource, null, this,
                                                principals);
    }

    public String toString() {
        StringBuilder result = new StringBuilder("TransformClassLoader<");
        Principal[] ps = protectionDomain.getPrincipals();
        Tid tid = (Tid) ps[0];
        result.append(tid.getName());
        result.append(">");
        return result.toString();
    }
}
