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

package com.untangle.mvvm.tapi;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class ProxyGenerator
{
    public static Object generateProxy(Class iface, Object impl)
    {
        Handler h = new Handler(impl);
        ClassLoader cl = impl.getClass().getClassLoader();
        return Proxy.newProxyInstance(cl, new Class[] { iface }, h);
    }

    // private classes -------------------------------------------------------

    private static class Handler implements InvocationHandler
    {
        private final Object impl;
        private final ClassLoader implCl;

        Handler(Object impl)
        {
            this.impl = impl;
            this.implCl = impl.getClass().getClassLoader();
        }

        public Object invoke(Object proxy, Method method, Object[] args)
        {
            Thread t = Thread.currentThread();
            ClassLoader oldCl = t.getContextClassLoader();

            Object retVal;
            try {
                // Entering impl ClassLoader ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                t.setContextClassLoader(implCl);
                retVal = method.invoke(impl, args);
            } catch (IllegalAccessException exn) {
                throw new RuntimeException(exn);
            } catch (InvocationTargetException exn) {
                throw new RuntimeException(exn);
            } finally {
                t.setContextClassLoader(oldCl);
                // Left impl ClassLoader ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            }

            return retVal;
        }
    }
}
