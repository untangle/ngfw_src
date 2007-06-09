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

package com.untangle.uvm.engine;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.Proxy;

class ProxyInputStream extends ObjectInputStream
{
    private ClassLoader targetCl;

    ProxyInputStream(InputStream inputStream) throws IOException
    {
        super(inputStream);
    }

    public Object readObject(ClassLoader targetCl)
        throws IOException, ClassNotFoundException
    {
        Object o;
        try {
            this.targetCl = targetCl;
            o = readObject();
        } finally {
            targetCl = null;
        }

        return o;
    }

    final protected Class resolveClass(ObjectStreamClass desc)
        throws IOException, ClassNotFoundException
    {
        return null == targetCl
            ? super.resolveClass(desc)
            : Class.forName(desc.getName(), false, targetCl);
    }

    final protected Class resolveProxyClass(String[] interfaces)
        throws IOException, ClassNotFoundException {

        ClassLoader latestLoader = null == targetCl
            ? Thread.currentThread().getContextClassLoader()
            : targetCl;

        ClassLoader nonPublicLoader = null;
        boolean hasNonPublicInterface = false;

        // define proxy in class loader of non-public interface(s), if any
        Class[] classObjs = new Class[interfaces.length];
        for (int i = 0; i < interfaces.length; i++) {
            Class cl = Class.forName(interfaces[i], false, latestLoader);
            if ((cl.getModifiers() & java.lang.reflect.Modifier.PUBLIC) == 0) {
                if (hasNonPublicInterface) {
                    if (nonPublicLoader != cl.getClassLoader()) {
                        throw new IllegalAccessError
                            ("conflicting non-public interface class loaders");
                    }
                } else {
                    nonPublicLoader = cl.getClassLoader();
                    hasNonPublicInterface = true;
                }
            }
            classObjs[i] = cl;
        }

        try {
            return Proxy.getProxyClass(hasNonPublicInterface
                                       ? nonPublicLoader : latestLoader,
                                       classObjs);
        } catch (IllegalArgumentException e) {
            throw new ClassNotFoundException(null, e);
        }
    }
}
