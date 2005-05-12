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
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import com.metavize.mvvm.client.InvocationTargetExpiredException;
import com.metavize.mvvm.client.LoginExpiredException;
import com.metavize.mvvm.client.MvvmRemoteContext;
import com.metavize.mvvm.security.LoginSession;
import org.apache.log4j.Logger;

class HttpInvoker extends InvokerBase
{
    private static final HttpInvoker INVOKER = new HttpInvoker();
    private static final int LOGIN_TIMEOUT_MINUTES = 30;
    private static final int LOGIN_REAPER_PERIOD = 60000;

    private static final Logger logger = Logger.getLogger(HttpInvoker.class);

    private final Map<LoginSession, LoginDesc> logins;
    private final ThreadLocal<LoginSession> activeLogin;
    private final ThreadLocal<LoginSession> newLogin;
    private final ThreadLocal<InetAddress> clientAddr;
    private final Timer loginReaper = new Timer(true);
    private final TargetReaper targetReaper = new TargetReaper();

    // constructors -----------------------------------------------------------

    private HttpInvoker()
    {
        logins = new ConcurrentHashMap<LoginSession, LoginDesc>();
        activeLogin = new ThreadLocal<LoginSession>();
        newLogin = new ThreadLocal<LoginSession>();
        clientAddr = new ThreadLocal<InetAddress>();
    }

    // static factories -------------------------------------------------------

    static HttpInvoker invoker()
    {
        return INVOKER;
    }

    // protected methods ------------------------------------------------------

    protected void handleStream(InputStream is, OutputStream os,
                                boolean local, InetAddress remoteAddr)
    {
        newLogin.remove();

        ObjectOutputStream oos = null;
        ProxyInputStream pis = null;

        try {
            pis = new ProxyInputStream(is);
            oos = new ObjectOutputStream(os);
            HttpInvocation hi = (HttpInvocation)pis.readObject();

            LoginSession loginSession = hi.loginSession;

            if (!remoteAddr.equals(remoteAddr)) {
                logger.warn("client address mismatch: " + remoteAddr);

                LoginExpiredException exn = new LoginExpiredException
                    ("client address mismatch: " + remoteAddr);
                oos.writeObject(exn);
                return;
            }

            Integer targetId = hi.targetId;
            String methodName = hi.methodSignature;
            String definingClass = hi.definingClass;

            if (null == targetId) {
                if (null == loginSession) {  /* login request */
                    NullLoginDesc loginDesc = NullLoginDesc.getLoginDesc(local);
                    TargetDesc targetDesc = loginDesc.getTargetDesc();
                    Object proxy = targetDesc.getProxy();
                    oos.writeObject(proxy);
                    return;
                } else {                     /* logout */
                    logins.remove(loginSession);
                    oos.writeObject(null);
                    return;
                }
            }

            LoginDesc loginDesc = null == loginSession
                ? NullLoginDesc.getLoginDesc(local) /* access MvvmLogin only */
                : logins.get(loginSession);         /* logged in */

            if (null == loginDesc) {
                oos.writeObject(new LoginExpiredException("login expired"));
                return;
            } else {
                loginDesc.touch();
            }

            TargetDesc targetDesc = loginDesc.getTargetDesc(targetId);
            Object target = null == targetDesc ? null : targetDesc.getTarget();

            if (null == target) {
                oos.writeObject(new InvocationTargetExpiredException
                                ("target expired: " + methodName));
                return;
            }

            Method method = targetDesc.getMethod(methodName);

            // XXX setting context-cl to current object's cl may not
            // always be correct, think about this
            ClassLoader targetCl = target.getClass().getClassLoader();
            ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
            // Entering TransformClassLoader ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
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
            } finally {
                activeLogin.remove();
                clientAddr.remove();
                Thread.currentThread().setContextClassLoader(oldCl);
                // Left TransformClassLoader ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            }


            if (retVal instanceof MvvmRemoteContext) { // XXX if is to login()
                LoginSession ls = newLogin.get();
                newLogin.remove();

                if (null != ls) {
                    loginSession = ls;
                    loginDesc = new LoginDesc(loginSession);
                    logins.put(loginSession, loginDesc);
                }
            }

            if (null != retVal && !(retVal instanceof Serializable)) {
                targetDesc = loginDesc.getTargetDesc(retVal, targetReaper);
                retVal = targetDesc.getProxy();
            }

            oos.writeObject(retVal);
        } catch (IOException exn) {
            logger.warn("IOException in HttpInvoker", exn);
        } catch (ClassNotFoundException exn) {
            logger.warn("ClassNotFoundException in HttpInvoker", exn);
        } catch (Exception exn) {
            logger.warn("Exception in HttpInvoker", exn);
        } finally {
            if (null != oos) {
                try {
                    oos.close();
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
                                logins.remove(loginSession);
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

    void login(LoginSession loginSession)
    {
        newLogin.set(loginSession);
    }

    InetAddress getClientAddr()
    {
        return clientAddr.get();
    }

    LoginSession[] getLoginSessions()
    {
        return logins.keySet().toArray(new LoginSession[0]);
    }
}
