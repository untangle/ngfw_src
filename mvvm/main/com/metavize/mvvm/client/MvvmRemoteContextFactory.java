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

package com.metavize.mvvm.client;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import javax.security.auth.login.FailedLoginException;

import com.metavize.mvvm.MvvmContext;
import com.metavize.mvvm.engine.HttpInvokerStub;
import com.metavize.mvvm.security.LoginSession;
import com.metavize.mvvm.security.MvvmLogin;
import com.metavize.mvvm.security.MvvmLoginException;

/**
 * Factory to get the MvvmContext for an MVVM instance.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 */
public class MvvmRemoteContextFactory
{
    private static final Object LOCK = new Object();

    private static MvvmContext MVVM_CONTEXT;
    private static HttpInvokerStub HTTP_INVOKER_STUB;

    /**
     * Get the <code>MvvmContext</code> from this classloader.
     * used by the GUI to get the context remotely.
     *
     * @return the <code>MvvmContext</code>.
     */
    public static MvvmContext mvvmContext()
    {
        return MVVM_CONTEXT;
    }

    /**
     * Get the MVVM context from a remote MVVM instance by logging in
     * from the login service on the specified <code>host</code>.
     *
     * @param host the host of the Mvvm.
     * @param username the username.
     * @param password the password.
     * @param timeout an <code>int</code> value.
     * @param classLoader the class loader to be used for deserialization.
     * @param secure use a SSL connection.
     * @return the <code>MvvmContext</code> for this machine.
     * @exception MvvmConnectException when an MvvmLogin object cannot
     *    be accessed at given <code>host</code> and <code>port</code>.
     * @exception FailedLoginException if an error occurs
     */
    public static MvvmContext login(String host, String username,
                                    String password, int timeout,
                                    ClassLoader classLoader, boolean secure)
        throws MvvmConnectException, FailedLoginException
    {
        URL url;

        try {
            url = new URL(secure ? "https" : "http", host, "/http-invoker");
        } catch (MalformedURLException exn) { /* shouldn't happen */
            throw new MvvmConnectException(exn);
        }

        synchronized (LOCK) {
            MvvmLogin ml = mvvmLogin(url, timeout, classLoader);
            MVVM_CONTEXT = ml.login(username, password);
            if (null != MVVM_CONTEXT) {
                InvocationHandler ih = Proxy.getInvocationHandler(MVVM_CONTEXT);
                HTTP_INVOKER_STUB = (HttpInvokerStub)ih;
            }
        }

        return MVVM_CONTEXT;
    }

    /**
     * Login with a secure connection.
     *
     * @param host the host of the Mvvm.
     * @param port the port of the <code>HttpNamingContextFactory</code>.
     * @param username the username.
     * @param password the password.
     * @param classLoader the class loader to be used for deserialization.
     * @return the <code>MvvmContext</code> for this machine.
     * @exception MvvmConnectException when an MvvmLogin object cannot
     *    be accessed at given <code>host</code> and <code>port</code>.
     */
    public static MvvmContext login(String host, String username,
                                    String password, int timeout,
                                    ClassLoader classLoader)
        throws MvvmConnectException, FailedLoginException
    {
        return login(host, username, password, timeout, classLoader, true);
    }

    /**
     * Get the MVVM context for the localhost "remote" MVVM instance
     * by logging in (with our magic localhost passwd) from the login
     * service at http://localhost/http-invoker.
     *
     * @return the remote MvvmContext.
     * @exception MvvmLoginException on login failure.
     */
    public static MvvmContext localLogin()
        throws FailedLoginException, MvvmConnectException
    {
        return localLogin(0);
    }

    /**
     * Get the MVVM context for the localhost "remote" MVVM instance
     * by logging in (with our magic localhost passwd) from the login
     * service at http://localhost/http-invoker.
     *
     * @return the remote MvvmContext.
     * @exception MvvmLoginException on login failure.
     */
    public static MvvmContext localLogin(int timeout)
        throws FailedLoginException, MvvmConnectException
    {
        String localUser = "localadmin";
        String localPasswd = "nimda11lacol";
        return login("localhost", localUser, localPasswd, timeout,
                     null, false);
    }

    public static void logout()
    {
        synchronized (LOCK) {
            try {
                HTTP_INVOKER_STUB.invoke(null, null, null);
            } catch (Exception exn) {
                throw new RuntimeException(exn); // XXX
            }
            HTTP_INVOKER_STUB = null;
        }
    }

    public static LoginSession loginSession()
    {
        synchronized (LOCK) {
            return HTTP_INVOKER_STUB.getLoginSession();
        }
    }

    private static MvvmLogin mvvmLogin(URL url, int timeout,
                                       ClassLoader classLoader)
    {
        // Note -- this login session is completely ignored by the server
        // (which it must be since it's made on the client).
        HttpInvokerStub his = new HttpInvokerStub(url, timeout, classLoader);

        MvvmLogin mvvmLogin;
        try {
            // XXX hack to get a login proxy
            return (MvvmLogin)his.invoke(null, null, null);
        } catch (Exception exn) {
            throw new RuntimeException(exn); // XXX
        }
    }
}
