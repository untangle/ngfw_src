/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: HttpInvokerStub.java,v 1.12 2005/03/25 03:51:16 amread Exp $
 */

package com.metavize.mvvm.engine;

import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;

import com.metavize.mvvm.security.LoginSession;

public class HttpInvokerStub implements InvocationHandler, Serializable
{
    private static final long serialVersionUID = -7096144423864315443L;

    private static int timeout;
    private static ClassLoader classLoader;
    private URL url;
    private Object cacheId;
    private LoginSession loginSession;

    public HttpInvokerStub(LoginSession ls, URL url, Object cacheId,
                           int timeout, ClassLoader classLoader)
    {
        this.loginSession = ls;
        this.url = url;
        this.cacheId = cacheId;
        this.timeout = timeout;
        if (classLoader != null) {
            this.classLoader = classLoader;
        }
    }

    public HttpInvokerStub(LoginSession ls, URL url, Object cacheId,
                           ClassLoader classLoader)
    {
        this(ls, url, cacheId, 0, classLoader);
    }

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
            ? new HttpInvocation(loginSession, url, null, null, null)
            : new HttpInvocation(loginSession, url, cacheId, method.toString(),
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
        } else if (o instanceof Exception) {
            throw (Exception)o;
        } else {
            return o;
        }
    }

    public LoginSession getLoginSession()
    {
        return loginSession;
    }

    public void logout()
    {
        // XXX implement
    }

    void setUrl(URL url)
    {
        this.url = url;
    }
}
