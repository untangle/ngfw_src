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

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.metavize.mvvm.security.LoginSession;


class TargetDesc
{
    private final WeakReference targetRef;
    private final Map<String, Method> methods;
    private final Object proxy;

    // constructors -----------------------------------------------------------

    TargetDesc(LoginSession ls, int targetId, WeakReference targetRef)
    {
        this.targetRef = targetRef;

        Object target = targetRef.get();
        Class[] ifaces = interfaces(target.getClass());
        ClassLoader cl = target.getClass().getClassLoader();

        Map<String, Method> ms = new HashMap<String, Method>();
        for (Class iface : ifaces) {
            for (Method m : iface.getMethods()) {
                ms.put(m.toString(), m);
            }
        }
        this.methods = Collections.unmodifiableMap(ms);

        HttpInvokerStub his = new HttpInvokerStub(ls, targetId);
        this.proxy = Proxy.newProxyInstance(cl, ifaces, his);
    }

    // package protected methods ----------------------------------------------

    Object getTarget()
    {
        return targetRef.get();
    }

    Method getMethod(String methodName)
    {
        return methods.get(methodName);
    }

    Object getProxy()
    {
        return proxy;
    }

    // private methods --------------------------------------------------------

    private Class[] interfaces(Class c)
    {
        Set<Class> s = new HashSet<Class>();

        interfaces(c, s);

        return s.toArray(new Class[s.size()]);
    }

    private Set<Class> interfaces(Class c, Set s)
    {
        // XXX Is there are more structural way of accomplishing this
        if (c.isInterface()) {
            s.add(c);
        }

        for (Class iface : c.getInterfaces()) {
            interfaces(iface, s);
        }

        Class superclass = c.getSuperclass();
        if (null != superclass) {
            interfaces(superclass, s);
        }
        return s;
    }
}
