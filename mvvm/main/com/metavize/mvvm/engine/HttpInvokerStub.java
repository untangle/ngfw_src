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

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;

import com.metavize.mvvm.client.InvocationConnectionException;
import com.metavize.mvvm.client.InvocationException;
import com.metavize.mvvm.security.LoginSession;

public class HttpInvokerStub implements InvocationHandler, Serializable
{
    private static final long serialVersionUID = 7987422171291937863L;

    private static final int[] SLEEP_TIMES = new int[] { 0, 500, 1000, 2000 };

    private static ClassLoader classLoader;

    private final URL url;
    private final int timeout;
    private final LoginSession loginSession;
    private final Integer targetId;

    // constructors -----------------------------------------------------------

    public HttpInvokerStub(URL url, int timeout, LoginSession loginSession,
                           Integer targetId)
    {
        this.url = url;
        this.timeout = timeout;
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
        try {
            return doInvoke(proxy, method, args);
        } catch (ConnectException exn) {
            throw new InvocationConnectionException("could not connect", exn);
        }
    }

    // private methods --------------------------------------------------------

    private Object doInvoke(Object proxy, Method method, Object[] args)
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
            ? new HttpInvocation(loginSession, null, null, null, url, timeout)
            : new HttpInvocation(loginSession, targetId, method.toString(),
                                 method.getDeclaringClass().getName(),
                                 url, timeout);

        boolean written = false;
        IOException writeExn = null;
        for (int i = 0; i < SLEEP_TIMES.length && !written; i++) {
            ObjectOutputStream oos = new ObjectOutputStream(huc.getOutputStream());
            try {
                oos.writeObject(inv);
                oos.writeObject(args);
                written = true;
            } catch (IOException exn) {
                writeExn = exn;
                try {
                    Thread.currentThread().sleep(SLEEP_TIMES[i]);
                } catch (InterruptedException e) { /* keep going */ }
            } finally {
                try {
                    oos.close();
                } catch (IOException exn) {
                    System.out.println("could not close connection" + exn);
                    exn.printStackTrace();
                }
            }
        }

        if (!written) {
            System.out.println("could not write invocation " + writeExn);
            writeExn.printStackTrace();
            throw new InvocationException(writeExn);
        }

        Object o = null;

        boolean read = false;
        IOException readExn = null;
        for (int i = 0; i < SLEEP_TIMES.length && !read; i++) {
            ProxyInputStream pis = new ProxyInputStream(huc.getInputStream());
            try {
                if (classLoader != null) {
                    o = pis.readObject(classLoader);
                } else {
                    o = pis.readObject();
                }
                read = true;
            } catch (IOException exn) {
                readExn = exn;
                try {
                    Thread.currentThread().sleep(SLEEP_TIMES[i]);
                } catch (InterruptedException e) { /* keep going */ }
            } finally {
                try {
                    pis.close();
                } catch (IOException exn) {
                    System.out.println("could not close connection" + exn);
                    exn.printStackTrace();
                }
            }
        }

        if (!read) {
            System.out.println("could not read return value " + readExn);
            readExn.printStackTrace();
            throw new InvocationException(readExn);
        }

        if (null == o) {
            return null;
        } else if (o instanceof Exception) {
            Exception e = (Exception)o;
            StackTraceElement[] r = e.getStackTrace();
            StackTraceElement[] l = Thread.currentThread().getStackTrace();
            StackTraceElement[] t = new StackTraceElement[r.length + l.length];
            System.arraycopy(r, 0, t, 0, r.length);
            System.arraycopy(l, 0, t, r.length, l.length);
            e.setStackTrace(t);
            throw e;
        } else if (o instanceof Error) {
            Error e = (Error)o;
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
