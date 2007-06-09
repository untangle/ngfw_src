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

package com.untangle.uvm.engine;

import java.io.IOException;
import java.io.InputStream;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPOutputStream;

import com.untangle.uvm.client.InvocationTargetExpiredException;
import com.untangle.uvm.client.LoginExpiredException;
import com.untangle.uvm.client.LoginStolenException;
import com.untangle.uvm.client.MultipleLoginsException;
import com.untangle.uvm.client.UvmRemoteContext;
import com.untangle.uvm.security.LoginSession;
import org.apache.log4j.Logger;

class HttpInvokerImpl implements HttpInvoker
{
    private static final boolean ALLOW_MULTIPLE_LOGINS
        = Boolean.parseBoolean(System.getProperty("uvm.login.multiuser"));

    private static final HttpInvokerImpl INVOKER = new HttpInvokerImpl();
    private static final int LOGIN_TIMEOUT_MINUTES = 30;
    private static final int LOGIN_REAPER_PERIOD = 60000;

    private final Logger logger = Logger.getLogger(getClass());

    private final Map<LoginSession, LoginDesc> logins;
    private final ThreadLocal<LoginSession> activeLogin;
    private final ThreadLocal<NewLoginDesc> newLogin;
    private final ThreadLocal<InetAddress> clientAddr;
    private final ThreadLocal<LoginDesc> loginDescs;
    private final Timer loginReaper = new Timer(true);
    private final TargetReaper targetReaper = new TargetReaper();

    // constructors -----------------------------------------------------------

    private HttpInvokerImpl()
    {
        logins = new ConcurrentHashMap<LoginSession, LoginDesc>();
        activeLogin = new ThreadLocal<LoginSession>();
        newLogin = new ThreadLocal<NewLoginDesc>();
        clientAddr = new ThreadLocal<InetAddress>();
        loginDescs = new ThreadLocal<LoginDesc>();
    }

    // static factories -------------------------------------------------------

    static HttpInvokerImpl invoker()
    {
        return INVOKER;
    }

    // public methods ---------------------------------------------------------

    public void handle(InputStream is, OutputStream os, boolean local,
                       InetAddress remoteAddr)
    {
        newLogin.remove();
        loginDescs.remove();

        ProxyOutputStream pos = null;
        ProxyInputStream pis = null;

        try {
            pis = new ProxyInputStream(is);
            HttpInvocation hi = (HttpInvocation)pis.readObject();
            if (GZIP_RESPONSE)
                pos = new ProxyOutputStream(new GZIPOutputStream(os), hi.url, hi.timeout);
            else
                pos = new ProxyOutputStream(os, hi.url, hi.timeout);

            LoginSession loginSession = hi.loginSession;

            if (!remoteAddr.equals(remoteAddr)) {
                logger.warn("client address mismatch: " + remoteAddr);

                LoginExpiredException exn = new LoginExpiredException
                    ("client address mismatch: " + remoteAddr);
                pos.writeObject(exn);
                return;
            }

            Integer targetId = hi.targetId;
            String methodName = hi.methodSignature;
            String definingClass = hi.definingClass;

            if (null == targetId) {
                if (null == loginSession) {  /* login request */
                    NullLoginDesc loginDesc = new NullLoginDesc(hi.url, hi.timeout);
                    loginDescs.set(loginDesc);
                    TargetDesc targetDesc = loginDesc.getTargetDesc();
                    Object proxy = targetDesc.getProxy();
                    pos.writeObject(proxy);
                    return;
                } else {                     /* logout */
                    LoginDesc ld = logins.remove(loginSession);
                    if (null == ld) {
                        logger.warn("null LoginDesc for: " + loginSession);
                    } else {
                        ld.destroy(targetReaper);
                        pos.writeObject(null);
                    }
                    return;
                }
            }

            LoginDesc loginDesc;
            if (null == loginSession) {
                loginDesc = new NullLoginDesc(hi.url, hi.timeout);
            } else {
                loginDesc = logins.get(loginSession);
                if (null != loginDesc) {
                    loginSession = loginDesc.getLoginSession();
                }
            }
            loginDescs.set(loginDesc);

            if (null == loginDesc) {
                pos.writeObject(new LoginExpiredException("login expired"));
                return;
            } else if (loginDesc.isStolen()) {
                pos.writeObject(new LoginStolenException(loginDesc.getLoginThief()));
                return;
            } else {
                loginDesc.touch();
            }

            TargetDesc targetDesc = loginDesc.getTargetDesc(targetId);
            Object target = null == targetDesc ? null : targetDesc.getTarget();

            if (null == target) {
                pos.writeObject(new InvocationTargetExpiredException
                                ("target expired: " + methodName));
                return;
            }

            if (logger.isDebugEnabled())
                logger.debug("Invoking " + target.getClass() + "." + methodName);

            Method method = targetDesc.getMethod(methodName);

            // XXX setting context-cl to current object's cl may not
            // always be correct, think about this
            ClassLoader targetCl = target.getClass().getClassLoader();
            ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
            // Entering NodeClassLoader ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            Thread.currentThread().setContextClassLoader(targetCl);

            Object retVal = null;
            try {
                Object[] args = (Object[])pis.readObject(targetCl);
                activeLogin.set(loginSession);
                clientAddr.set(remoteAddr);
                // XXX use Subject.doAs() :
                retVal = method.invoke(target, args);
            } catch (InvocationTargetException exn) {
                retVal = exn.getTargetException();
            } catch (Exception exn) {
                logger.warn("exception in RPC call", exn);
                retVal = exn;
            } catch (Error err) {
                logger.warn("error in RPC call", err);
            } finally {
                activeLogin.remove();
                clientAddr.remove();
                Thread.currentThread().setContextClassLoader(oldCl);
                // Left NodeClassLoader ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            }

            if (retVal instanceof UvmRemoteContext) { // XXX if is to login()
                NewLoginDesc nld = newLogin.get();
                newLogin.remove();

                if (null != nld) {
                    LoginSession ls = nld.loginSession;

                    if (null != ls) {
                        LoginSession ols = checkSessions(ls, nld.force);
                        if (null != ols) {
                            retVal = new MultipleLoginsException(ols);
                        } else {
                            loginSession = ls;
                            loginDesc = new LoginDesc(hi.url, hi.timeout,
                                                      loginSession);
                            loginDescs.set(loginDesc);
                            logins.put(loginSession, loginDesc);
                        }
                    }
                }
            }

            if (null != retVal && !(retVal instanceof Serializable)) {
                targetDesc = loginDesc.getTargetDesc(retVal, targetReaper);
                retVal = targetDesc.getProxy();
            }

            pos.writeObject(retVal);
        } catch (IOException exn) {
            logger.warn("IOException in HttpInvoker", exn);
        } catch (ClassNotFoundException exn) {
            logger.warn("ClassNotFoundException in HttpInvoker", exn);
        } catch (Exception exn) {
            logger.warn("Exception in HttpInvoker", exn);
        } finally {
            if (null != pos) {
                try {
                    pos.close();
                } catch (IOException exn) {
                    logger.warn("could not close output stream", exn);
                }
            }

            if (null != pis) {
                try {
                    pis.close();
                } catch (IOException exn) {
                    logger.warn("could not close input stream", exn);
                }
            }

            loginDescs.remove();
        }
    }

    // package private methods ------------------------------------------------

    void init()
    {
        loginReaper.schedule(new TimerTask() {
                public void run()
                {
                    Calendar cal = Calendar.getInstance();
                    cal.add(Calendar.MINUTE, -LOGIN_TIMEOUT_MINUTES);
                    Date cutoff = cal.getTime();

                    for (LoginSession loginSession : logins.keySet()) {
                        LoginDesc loginDesc = logins.get(loginSession);
                        if (null != loginDesc) {
                            Date lastAccess = loginDesc.getLastAccess();
                            if (cutoff.after(lastAccess)) {
                                LoginDesc ld = logins.remove(loginSession);
                                ld.destroy(targetReaper);
                            }
                        }
                    }
                }
            }, LOGIN_REAPER_PERIOD, LOGIN_REAPER_PERIOD);
        targetReaper.init();
    }

    void destroy()
    {
        loginReaper.cancel();
        targetReaper.destroy();
    }

    void login(LoginSession loginSession, boolean force)
    {
        newLogin.set(new NewLoginDesc(loginSession, force));
    }

    InetAddress getClientAddr()
    {
        return clientAddr.get();
    }

    LoginSession[] getLoginSessions()
    {
        return logins.keySet().toArray(new LoginSession[0]);
    }

    LoginSession getActiveLogin()
    {
        return activeLogin.get();
    }

    void logoutActiveLogin()
    {
        activeLogin.remove();
    }

    // private classes --------------------------------------------------------

    private static class NewLoginDesc
    {
        final LoginSession loginSession;
        final boolean force;

        NewLoginDesc(LoginSession loginSession, boolean force)
        {
            this.loginSession = loginSession;
            this.force = force;
        }
    }

    private class ProxyOutputStream extends ObjectOutputStream
    {
        private final URL url;
        private final int timeout;

        ProxyOutputStream(OutputStream os, URL url, int timeout)
            throws IOException
        {
            super(os);

            this.url = url;
            this.timeout = timeout;

            enableReplaceObject(true);
        }

        protected Object replaceObject(Object o) throws IOException
        {
            if (null != o && !(o instanceof Serializable)) {
                LoginDesc loginDesc = loginDescs.get();
                if (null != loginDesc) {
                    TargetDesc targetDesc = loginDesc.getTargetDesc(o, targetReaper);
                    return targetDesc.getProxy();
                } else {
                    throw new NotSerializableException("loginDesc not set, cannot make a proxy");
                }
            } else {
                return o;
            }
        }
    }

    // private methods --------------------------------------------------------

    private LoginSession checkSessions(LoginSession ls, boolean force)
    {
        if (!ALLOW_MULTIPLE_LOGINS && ls.isInteractive()) {
            for (Iterator<LoginSession> i = logins.keySet().iterator();
                 i.hasNext(); ) {
                LoginSession ols = i.next();
                if (ols.isInteractive()) {
                    LoginDesc ld = logins.get(ols);
                    if (null != ld && !ld.isStolen()) {
                        if (force) {
                            ld.steal(ls);
                        } else {
                            return ols;
                        }
                    }
                }
            }
        }

        return null;
    }
}
