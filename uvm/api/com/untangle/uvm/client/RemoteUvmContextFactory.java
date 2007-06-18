/*
 * $HeadURL$
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

package com.untangle.uvm.client;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import javax.security.auth.login.FailedLoginException;

import com.untangle.uvm.engine.HttpInvokerStub;
import com.untangle.uvm.security.LoginSession;
import com.untangle.uvm.security.UvmLogin;

/**
 * Factory to get the RemoteUvmContext for an UVM instance.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class RemoteUvmContextFactory
{
    private static final String SYSTEM_USER = "localadmin";
    private static final String SYSTEM_PASSWORD = "nimda11lacol";

    private static final RemoteUvmContextFactory FACTORY
        = new RemoteUvmContextFactory();

    private RemoteUvmContext remoteContext;
    private HttpInvokerStub httpInvokerStub;

    // factory methods --------------------------------------------------------

    public static RemoteUvmContextFactory factory()
    {
        return FACTORY;
    }

    // public methods ---------------------------------------------------------

    /**
     * Get the <code>RemoteUvmContext</code> from this classloader.
     * used by the GUI to get the context remotely.
     *
     * @return the <code>UvmContext</code>.
     */
    public RemoteUvmContext uvmContext()
    {
        return remoteContext;
    }

    /**
     * Describe <code>isActivated</code> method here.
     *
     * @param host the host of the Uvm.
     * @param timeout an <code>int</code> value.
     * @param secure use a SSL connection.
     * @return a <code>boolean</code> true if already activated
     * @exception UvmConnectException when an UvmLogin object cannot
     *    be accessed at given <code>host</code>
     */
    public boolean isActivated(String host, int timeout, boolean secure)
        throws UvmConnectException
    {
        return isActivated(host, 0, timeout, secure );
    }

    /**
     * Describe <code>isActivated</code> method here.
     *
     * @param host the host of the Uvm.
     * @param port an <code>int</code> value, or 0 for undefined.
     * @param timeout an <code>int</code> value.
     * @param secure use a SSL connection.
     * @return a <code>boolean</code> true if already activated
     * @exception UvmConnectException when an UvmLogin object cannot
     *    be accessed at given <code>host</code>
     */
    public boolean isActivated(String host, int port, int timeout,
                               boolean secure)
        throws UvmConnectException
    {
        URL url = makeURL(host, port, secure);

        synchronized (this) {
            UvmLogin ml = uvmLogin(url, timeout, null);
            return ml.isActivated();
        }
    }

    /**
     * Log in and get a RemoteUvmContext. This method is for
     * interactive clients that require an exclusive login.
     *
     * @param host the host of the Uvm.
     * @param username the username.
     * @param password the password.
     * @param timeout an <code>int</code> value.
     * @param classLoader the class loader to be used for deserialization.
     * @param secure use a SSL connection.
     * @param force logout other interactive logins, if
     * @return the <code>UvmContext</code> for this machine.
     * @exception UvmConnectException when an UvmLogin object cannot
     *    be accessed at given <code>host</code> and <code>port</code>.
     * @exception FailedLoginException when the username and password
     * are incorrect.
     * @exception MultipleLoginsException when there is already an
     * interactive login and force is false.
     */
    public RemoteUvmContext interactiveLogin(String host, String username,
                                              String password, int timeout,
                                              ClassLoader classLoader,
                                              boolean secure, boolean force)
        throws UvmConnectException, FailedLoginException,
               MultipleLoginsException
    {
        return interactiveLogin(host, 0, username, password, timeout, classLoader, secure, force);
    }


    /**
     * Log in and get a RemoteUvmContext. This method is for
     * interactive clients that require an exclusive login.
     *
     * @param host the host of the Uvm.
     * @param port the the port the server is on.
     * @param username the username.
     * @param password the password.
     * @param timeout an <code>int</code> value.
     * @param classLoader the class loader to be used for deserialization.
     * @param secure use a SSL connection.
     * @param force logout other interactive logins, if
     * @return the <code>UvmContext</code> for this machine.
     * @exception UvmConnectException when an UvmLogin object cannot
     *    be accessed at given <code>host</code> and <code>port</code>.
     * @exception FailedLoginException when the username and password
     * are incorrect.
     * @exception MultipleLoginsException when there is already an
     * interactive login and force is false.
     */
    public RemoteUvmContext interactiveLogin(String host, int port,
                                              String username, String password,
                                              int timeout,
                                              ClassLoader classLoader,
                                              boolean secure, boolean force)
        throws UvmConnectException, FailedLoginException,
               MultipleLoginsException
    {
        URL url = makeURL(host, port, secure);

        synchronized (this) {
            UvmLogin ml = uvmLogin(url, timeout, classLoader);
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
     * @param host the host of the Uvm.
     * @param key a <code>String</code> giving the key to be activated under
     * @param timeout an <code>int</code> value.
     * @param classLoader the class loader to be used for deserialization.
     * @param secure use a SSL connection.
     * @return a <code>RemoteUvmContext</code> value
     * @exception UvmConnectException when an UvmLogin object cannot
     * be accessed at given <code>host</code> and <code>port</code>.
     * @exception FailedLoginException if the key isn't kosher or the
     * product has already been activated
     */
    public RemoteUvmContext activationLogin(String host, String key,
                                             int timeout,
                                             ClassLoader classLoader,
                                             boolean secure)
        throws UvmConnectException, FailedLoginException
    {
        return activationLogin( host, 0, key, timeout, classLoader, secure );
    }

    /**
     * Activates the Untangle Server using the given key.
     *
     * @param host the host of the Uvm.
     * @param port the the port the server is on.
     * @param key a <code>String</code> giving the key to be activated under
     * @param timeout an <code>int</code> value.
     * @param classLoader the class loader to be used for deserialization.
     * @param secure use a SSL connection.
     * @return a <code>RemoteUvmContext</code> value
     * @exception UvmConnectException when an UvmLogin object cannot
     *    be accessed at given <code>host</code> and <code>port</code>.
     * @exception FailedLoginException if the key isn't kosher or the
     *    product has already been activated
     */
    public RemoteUvmContext activationLogin(String host, int port, String key,
                                             int timeout,
                                             ClassLoader classLoader,
                                             boolean secure)
        throws UvmConnectException, FailedLoginException
    {
        URL url = makeURL(host, port, secure);
        synchronized (this) {
            UvmLogin ml = uvmLogin(url, timeout, classLoader);
            remoteContext = ml.activationLogin(key);
            if (null != remoteContext) {
                InvocationHandler ih = Proxy.getInvocationHandler(remoteContext);
                httpInvokerStub = (HttpInvokerStub)ih;
            }
        }

        return remoteContext;
    }

    /**
     * Log in and get a RemoteUvmContext. This is for system logins
     * that do not require exclusive access to the UVM. These logins
     * are allowed even when a client has an interactive login. Even
     * though some of these applications (mcli) may change the UVM
     * state in a way that an interactive client cannot handle, this
     * functionality is not exposed to end users.
     *
     * @param timeout an <code>int</code> value
     * @return the remote RemoteUvmContext.
     * @exception FailedLoginException if an error occurs
     * @exception UvmConnectException if an error occurs
     */
    public RemoteUvmContext systemLogin(int timeout, ClassLoader cl)
        throws FailedLoginException, UvmConnectException
    {
        URL url;
        try {
            url = new URL("http", "localhost", "/http-invoker");
        } catch (MalformedURLException exn) { /* shouldn't happen */
            throw new UvmConnectException(exn);
        }

        synchronized (this) {
            UvmLogin ml = uvmLogin(url, timeout, cl);
            remoteContext = ml.systemLogin(SYSTEM_USER, SYSTEM_PASSWORD);
            if (null != remoteContext) {
                InvocationHandler ih = Proxy.getInvocationHandler(remoteContext);
                httpInvokerStub = (HttpInvokerStub)ih;
            }
        }

        return remoteContext;
    }

    public RemoteUvmContext systemLogin(int timeout)
        throws FailedLoginException, UvmConnectException
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

    private UvmLogin uvmLogin(URL url, int timeout, ClassLoader classLoader)
    {
        HttpInvokerStub.setTimeout(timeout);

        // Note -- this login session is completely ignored by the server
        // (which it must be since it's made on the client).
        HttpInvokerStub his = new HttpInvokerStub(url, classLoader);

        UvmLogin uvmLogin;
        try {
            // XXX hack to get a login proxy
            return (UvmLogin)his.invoke(null, null, null);
        } catch (Exception exn) {
            throw new RuntimeException(exn); // XXX
        }
    }

    /**
     * Utility to create a URL based on the host, port and whether or
     * not it is secure
     * @param host the host of the Uvm.
     * @param port the the port the server is on.
     * @param secure use a SSL connection.
     * @return the remote URL.
     * @exception UvmConnectException if an error occurs, this should never occur.
     */
    private URL makeURL(String host, int port, boolean secure)
        throws UvmConnectException
    {
        try {
            if (port <= 0) {
                return new URL(secure ? "https" : "http", host, "/http-invoker");
            } else {
                return new URL(secure ? "https" : "http", host, port, "/http-invoker");
            }
        } catch (MalformedURLException exn) { /* shouldn't happen */
            throw new UvmConnectException( exn );
        }
    }
}
