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

package com.untangle.mvvm.client;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import javax.security.auth.login.FailedLoginException;

import com.untangle.mvvm.engine.HttpInvokerStub;
import com.untangle.mvvm.security.LoginSession;
import com.untangle.mvvm.security.MvvmLogin;

/**
 * Factory to get the MvvmRemoteContext for an MVVM instance.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class MvvmRemoteContextFactory
{
    private static final String SYSTEM_USER = "localadmin";
    private static final String SYSTEM_PASSWORD = "nimda11lacol";

    private static final MvvmRemoteContextFactory FACTORY
        = new MvvmRemoteContextFactory();

    private MvvmRemoteContext remoteContext;
    private HttpInvokerStub httpInvokerStub;

    // factory methods --------------------------------------------------------

    public static MvvmRemoteContextFactory factory()
    {
        return FACTORY;
    }

    // public methods ---------------------------------------------------------

    /**
     * Get the <code>MvvmRemoteContext</code> from this classloader.
     * used by the GUI to get the context remotely.
     *
     * @return the <code>MvvmContext</code>.
     */
    public MvvmRemoteContext mvvmContext()
    {
        return remoteContext;
    }

    /**
     * Describe <code>isActivated</code> method here.
     *
     * @param host the host of the Mvvm.
     * @param timeout an <code>int</code> value.
     * @param secure use a SSL connection.
     * @return a <code>boolean</code> true if already activated
     * @exception MvvmConnectException when an MvvmLogin object cannot
     *    be accessed at given <code>host</code>
     */
    public boolean isActivated(String host, int timeout, boolean secure)
        throws MvvmConnectException
    {
        return isActivated(host, 0, timeout, secure );
    }

    /**
     * Describe <code>isActivated</code> method here.
     *
     * @param host the host of the Mvvm.
     * @param port an <code>int</code> value, or 0 for undefined.
     * @param timeout an <code>int</code> value.
     * @param secure use a SSL connection.
     * @return a <code>boolean</code> true if already activated
     * @exception MvvmConnectException when an MvvmLogin object cannot
     *    be accessed at given <code>host</code>
     */
    public boolean isActivated(String host, int port, int timeout,
                               boolean secure)
        throws MvvmConnectException
    {
        URL url = makeURL(host, port, secure);

        synchronized (this) {
            MvvmLogin ml = mvvmLogin(url, timeout, null);
            return ml.isActivated();
        }
    }

    /**
     * Log in and get a MvvmRemoteContext. This method is for
     * interactive clients that require an exclusive login.
     *
     * @param host the host of the Mvvm.
     * @param username the username.
     * @param password the password.
     * @param timeout an <code>int</code> value.
     * @param classLoader the class loader to be used for deserialization.
     * @param secure use a SSL connection.
     * @param force logout other interactive logins, if
     * @return the <code>MvvmContext</code> for this machine.
     * @exception MvvmConnectException when an MvvmLogin object cannot
     *    be accessed at given <code>host</code> and <code>port</code>.
     * @exception FailedLoginException when the username and password
     * are incorrect.
     * @exception MultipleLoginsException when there is already an
     * interactive login and force is false.
     */
    public MvvmRemoteContext interactiveLogin(String host, String username,
                                              String password, int timeout,
                                              ClassLoader classLoader,
                                              boolean secure, boolean force)
        throws MvvmConnectException, FailedLoginException,
               MultipleLoginsException
    {
        return interactiveLogin(host, 0, username, password, timeout, classLoader, secure, force);
    }


    /**
     * Log in and get a MvvmRemoteContext. This method is for
     * interactive clients that require an exclusive login.
     *
     * @param host the host of the Mvvm.
     * @param port the the port the server is on.
     * @param username the username.
     * @param password the password.
     * @param timeout an <code>int</code> value.
     * @param classLoader the class loader to be used for deserialization.
     * @param secure use a SSL connection.
     * @param force logout other interactive logins, if
     * @return the <code>MvvmContext</code> for this machine.
     * @exception MvvmConnectException when an MvvmLogin object cannot
     *    be accessed at given <code>host</code> and <code>port</code>.
     * @exception FailedLoginException when the username and password
     * are incorrect.
     * @exception MultipleLoginsException when there is already an
     * interactive login and force is false.
     */
    public MvvmRemoteContext interactiveLogin(String host, int port,
                                              String username, String password,
                                              int timeout,
                                              ClassLoader classLoader,
                                              boolean secure, boolean force)
        throws MvvmConnectException, FailedLoginException,
               MultipleLoginsException
    {
        URL url = makeURL(host, port, secure);

        synchronized (this) {
            MvvmLogin ml = mvvmLogin(url, timeout, classLoader);
            remoteContext = ml.interactiveLogin(username, password, force);
            if (null != remoteContext) {
                InvocationHandler ih = Proxy.getInvocationHandler(remoteContext);
                httpInvokerStub = (HttpInvokerStub)ih;
            }
        }

        return remoteContext;
    }

    /**
     * Activates the Untangle Server using the given key.
     *
     * @param host the host of the Mvvm.
     * @param key a <code>String</code> giving the key to be activated under
     * @param timeout an <code>int</code> value.
     * @param classLoader the class loader to be used for deserialization.
     * @param secure use a SSL connection.
     * @return a <code>MvvmRemoteContext</code> value
     * @exception MvvmConnectException when an MvvmLogin object cannot
     * be accessed at given <code>host</code> and <code>port</code>.
     * @exception FailedLoginException if the key isn't kosher or the
     * product has already been activated
     */
    public MvvmRemoteContext activationLogin(String host, String key,
                                             int timeout,
                                             ClassLoader classLoader,
                                             boolean secure)
        throws MvvmConnectException, FailedLoginException
    {
        return activationLogin( host, 0, key, timeout, classLoader, secure );
    }

    /**
     * Activates the Untangle Server using the given key.
     *
     * @param host the host of the Mvvm.
     * @param port the the port the server is on.
     * @param key a <code>String</code> giving the key to be activated under
     * @param timeout an <code>int</code> value.
     * @param classLoader the class loader to be used for deserialization.
     * @param secure use a SSL connection.
     * @return a <code>MvvmRemoteContext</code> value
     * @exception MvvmConnectException when an MvvmLogin object cannot
     *    be accessed at given <code>host</code> and <code>port</code>.
     * @exception FailedLoginException if the key isn't kosher or the
     *    product has already been activated
     */
    public MvvmRemoteContext activationLogin(String host, int port, String key,
                                             int timeout,
                                             ClassLoader classLoader,
                                             boolean secure)
        throws MvvmConnectException, FailedLoginException
    {
        URL url = makeURL(host, port, secure);
        synchronized (this) {
            MvvmLogin ml = mvvmLogin(url, timeout, classLoader);
            remoteContext = ml.activationLogin(key);
            if (null != remoteContext) {
                InvocationHandler ih = Proxy.getInvocationHandler(remoteContext);
                httpInvokerStub = (HttpInvokerStub)ih;
            }
        }

        return remoteContext;
    }

    /**
     * Log in and get a MvvmRemoteContext. This is for system logins
     * that do not require exclusive access to the MVVM. These logins
     * are allowed even when a client has an interactive login. Even
     * though some of these applications (mcli) may change the MVVM
     * state in a way that an interactive client cannot handle, this
     * functionality is not exposed to end users.
     *
     * @param timeout an <code>int</code> value
     * @return the remote MvvmRemoteContext.
     * @exception FailedLoginException if an error occurs
     * @exception MvvmConnectException if an error occurs
     */
    public MvvmRemoteContext systemLogin(int timeout, ClassLoader cl)
        throws FailedLoginException, MvvmConnectException
    {
        URL url;
        try {
            url = new URL("http", "localhost", "/http-invoker");
        } catch (MalformedURLException exn) { /* shouldn't happen */
            throw new MvvmConnectException(exn);
        }

        synchronized (this) {
            MvvmLogin ml = mvvmLogin(url, timeout, cl);
            remoteContext = ml.systemLogin(SYSTEM_USER, SYSTEM_PASSWORD);
            if (null != remoteContext) {
                InvocationHandler ih = Proxy.getInvocationHandler(remoteContext);
                httpInvokerStub = (HttpInvokerStub)ih;
            }
        }

        return remoteContext;
    }

    public MvvmRemoteContext systemLogin(int timeout)
        throws FailedLoginException, MvvmConnectException
    {
        return systemLogin(timeout, null);
    }

    public void setTimeout(int timeout)
    {
        HttpInvokerStub.setTimeout(timeout);
    }

    public int getTimeout()
    {
        return HttpInvokerStub.getTimeout();
    }

    public void logout()
    {
        synchronized (this) {
            try {
                httpInvokerStub.invoke(null, null, null);
            } catch (Exception exn) {
                throw new RuntimeException(exn); // XXX
            }
            httpInvokerStub = null;
        }
    }

    public LoginSession loginSession()
    {
        synchronized (this) {
            return httpInvokerStub.getLoginSession();
        }
    }

    // private methods --------------------------------------------------------

    private MvvmLogin mvvmLogin(URL url, int timeout, ClassLoader classLoader)
    {
        HttpInvokerStub.setTimeout(timeout);

        // Note -- this login session is completely ignored by the server
        // (which it must be since it's made on the client).
        HttpInvokerStub his = new HttpInvokerStub(url, classLoader);

        MvvmLogin mvvmLogin;
        try {
            // XXX hack to get a login proxy
            return (MvvmLogin)his.invoke(null, null, null);
        } catch (Exception exn) {
            throw new RuntimeException(exn); // XXX
        }
    }

    /**
     * Utility to create a URL based on the host, port and whether or
     * not it is secure
     * @param host the host of the Mvvm.
     * @param port the the port the server is on.
     * @param secure use a SSL connection.
     * @return the remote URL.
     * @exception MvvmConnectException if an error occurs, this should never occur.
     */
    private URL makeURL(String host, int port, boolean secure)
        throws MvvmConnectException
    {
        try {
            if (port <= 0) {
                return new URL(secure ? "https" : "http", host, "/http-invoker");
            } else {
                return new URL(secure ? "https" : "http", host, port, "/http-invoker");
            }
        } catch (MalformedURLException exn) { /* shouldn't happen */
            throw new MvvmConnectException( exn );
        }
    }
}
