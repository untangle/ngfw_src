/*
 * $HeadURL:$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.uvm.engine;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.untangle.uvm.security.LoginSession;

class TargetDesc
{
    private final WeakReference targetRef;
    private final Map<String, Method> methods;
    private final Object proxy;

    // constructors -----------------------------------------------------------

    TargetDesc(URL url, int timeout, LoginSession ls, int targetId,
               WeakReference targetRef)
    {
        this.targetRef = targetRef;

        Object target = targetRef.get();
        Class[] ifaces = interfaces(target.getClass());
        ClassLoader cl = target.getClass().getClassLoader();

        Map<String, Method> ms = new HashMap<String, Method>();
        for (Class iface : ifaces) {
            for (Method m : iface.getMethods()) {
                ms.put(HttpInvokerStub.encodeMethod(m), m);
            }
        }
        for (Method m : Object.class.getMethods()) {
            ms.put(HttpInvokerStub.encodeMethod(m), m);
        }
        this.methods = Collections.unmodifiableMap(ms);

        HttpInvokerStub his = new HttpInvokerStub(url, ls, targetId);
        this.proxy = Proxy.newProxyInstance(cl, ifaces, his);
    }

    // package protected methods ----------------------------------------------

    Object getTarget()
    {
        return targetRef.get();
    }

    WeakReference getTargetRef()
    {
        return targetRef;
    }

    Method getMethod(String methodName)
    {
        if (null == methods.get(methodName)) {
            System.out.println("GETTING METHOD: " + methodName + " FROM : " + methods);
        }

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
        if (c.isInterface() && !c.getName().endsWith("Priv")) {
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
