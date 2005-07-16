/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.engine;

import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.HttpURLConnection;
import java.net.URL;

import com.metavize.mvvm.security.LoginSession;

public class HttpInvokerStub implements InvocationHandler, Serializable
{
    private static final long serialVersionUID = 7987422171291937863L;

    private static ClassLoader classLoader;

    private final LoginSession loginSession;
    private final Integer targetId;

    // values passed to HttpInvokerStubs returned from MVVM:
    private URL url;
    private int timeout;

    // constructors -----------------------------------------------------------

    public HttpInvokerStub(LoginSession loginSession, Integer targetId)
    {
        this.loginSession = loginSession;
        this.targetId = targetId;
    }

    public HttpInvokerStub(URL url, int timeout, ClassLoader classLoader)
    {
        this.loginSession = null;
        this.targetId = null;
        this.url = url;
        this.timeout = timeout;
        if (classLoader != null) {
            this.classLoader = classLoader;
        }
    }

    // public methods ---------------------------------------------------------

    public LoginSession getLoginSession()
    {
        return loginSession;
    }

    // InvocationHandler methods ----------------------------------------------

    public Object invoke(Object proxy, Method method, Object[] args)
        throws Exception
    {
        HttpURLConnection huc = (HttpURLConnection)url.openConnection();
        huc.setRequestProperty("ContentType", "application/octet-stream");
        huc.setDoInput(true);
        huc.setDoOutput(true);
        huc.setRequestMethod("POST");
        huc.setConnectTimeout(timeout);

        // XXX hack: null for login proxy
        HttpInvocation inv = (null == proxy)
            ? new HttpInvocation(loginSession, null, null, null)
            : new HttpInvocation(loginSession, targetId, method.toString(),
                                  method.getDeclaringClass().getName());

        ObjectOutputStream oos = new ObjectOutputStream(huc.getOutputStream());
        try {
            oos.writeObject(inv);
            oos.writeObject(args);
        } finally {
            oos.close();
        }

        Object o = null;

        ProxyInputStream pis = new ProxyInputStream(huc.getInputStream());
        try {
            if (classLoader != null) {
                o = pis.readObject(classLoader);
            } else {
                o = pis.readObject();
            }
        } finally {
            pis.close();
        }

        if (null == o) {
            return null;
        } else if (Proxy.isProxyClass(o.getClass())) {
            InvocationHandler ih = Proxy.getInvocationHandler(o);
            if (ih instanceof HttpInvokerStub) {
                HttpInvokerStub his = (HttpInvokerStub)ih;
                his.url = url;
                his.timeout = timeout;
            }
            return o;
        } else if (o instanceof Exception) {
            Exception e = (Exception)o;
            StackTraceElement[] r = e.getStackTrace();
            StackTraceElement[] l = Thread.currentThread().getStackTrace();
            StackTraceElement[] t = new StackTraceElement[r.length + l.length];
            System.arraycopy(r, 0, t, 0, r.length);
            System.arraycopy(l, 0, t, r.length, l.length);
            e.setStackTrace(t);
            throw e;
        } else {
            return o;
        }
    }
}
