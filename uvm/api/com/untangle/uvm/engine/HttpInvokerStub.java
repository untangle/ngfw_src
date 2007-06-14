/*
 * $HeadURL:$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.uvm.engine;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.GZIPInputStream;

import com.untangle.uvm.client.InvocationConnectionException;
import com.untangle.uvm.client.InvocationException;
import com.untangle.uvm.security.LoginSession;

public class HttpInvokerStub implements InvocationHandler, Serializable
{
    private static final long serialVersionUID = 7987422171291937863L;

    private static final int[] SLEEP_TIMES = new int[] { 0, 500, 1000, 2000 };

    private static ClassLoader classLoader;

    private static int timeout = 0;

    private final URL url;
    private final LoginSession loginSession;
    private final Integer targetId;

    // constructors -----------------------------------------------------------

    public HttpInvokerStub(URL url, LoginSession loginSession,
                           Integer targetId)
    {
        this.url = url;
        this.loginSession = loginSession;
        this.targetId = targetId;
    }


    public HttpInvokerStub(URL url, ClassLoader classLoader)
    {
        this.loginSession = null;
        this.targetId = null;
        this.url = url;
        if (classLoader != null) {
            this.classLoader = classLoader;
        }
    }

    // static methods ---------------------------------------------------------

    public static void setTimeout(int timeout)
    {
        HttpInvokerStub.timeout = timeout;
    }

    public static int getTimeout()
    {
        return timeout;
    }

    public static String encodeMethod(Method m) {
        StringBuilder sb = new StringBuilder();

        sb.append(m.getReturnType().getName());
        sb.append(" ");
        sb.append(m.getDeclaringClass().getName());
        sb.append(".");
        sb.append(m.getName());
        sb.append("(");

        Class[] params = m.getParameterTypes();
        for (int i = 0; i < params.length; i++) {
            if (0 != i) {
                sb.append(",");
            }
            sb.append(params[i].getName());
        }
        sb.append(")");

        return sb.toString();
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
        huc.setRequestMethod("POST");
        huc.setRequestProperty("Accept-Encoding", "gzip");
        huc.setRequestProperty("Content-Type", "application/octet-stream");
        huc.setDoInput(true);
        huc.setDoOutput(true);
        huc.setConnectTimeout(timeout);

        // XXX hack: null for login proxy
        HttpInvocation inv = (null == proxy)
            ? new HttpInvocation(loginSession, null, null, null, url, timeout)
            : new HttpInvocation(loginSession, targetId, encodeMethod(method),
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
            ProxyInputStream pis;
            String ce = huc.getContentEncoding();
            if (ce != null && ce.indexOf("gzip") != -1)
                pis = new ProxyInputStream(new GZIPInputStream(huc.getInputStream()));
            else
                pis = new ProxyInputStream(huc.getInputStream());

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
