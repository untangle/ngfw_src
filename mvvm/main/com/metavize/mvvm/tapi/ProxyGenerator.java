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

package com.metavize.mvvm.tapi;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class ProxyGenerator
{
    private final ClassLoader proxyCl;

    public ProxyGenerator(ClassLoader proxyCl)
    {
        this.proxyCl = proxyCl;
    }

    public Object generateProxy(Class iface, Object impl, ClassLoader cl)
    {
        Handler h = new Handler(impl, cl);
        return Proxy.newProxyInstance(proxyCl, new Class[] { iface }, h);
    }

    public Object generateProxy(Class iface, Object impl)
    {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        return generateProxy(iface, impl, cl);
    }

    // private classes -------------------------------------------------------

    private static class Handler implements InvocationHandler
    {
        private final Object impl;
        private final ClassLoader implCl;

        Handler(Object impl, ClassLoader implCl)
        {
            this.impl = impl;
            this.implCl = implCl;
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
