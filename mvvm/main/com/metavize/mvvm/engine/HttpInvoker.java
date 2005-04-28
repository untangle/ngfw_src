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
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.InetAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.client.InvocationTargetExpiredException;
import com.metavize.mvvm.security.LoginSession;
import org.apache.log4j.Logger;

class HttpInvoker extends InvokerBase
{
    private static final HttpInvoker INVOKER = new HttpInvoker();
    private static final Logger logger = Logger.getLogger(HttpInvoker.class);

    // canoncial LoginSession:
    private Map loginSessions = new WeakHashMap();
    // keeps cacheIds weakly by loginSession:
    private Map cacheIds = new WeakHashMap();
    // keeps targets weakly by cacheId:
    private Map targets = new HashMap();
    private Map proxyCache = Collections.synchronizedMap(new WeakHashMap());
    private Map classMethodsMap = new WeakHashMap();
    private Map interfaceMap = new WeakHashMap();

    private HttpInvoker() { }

    static HttpInvoker invoker()
    {
        return INVOKER;
    }

    public LoginSession[] getLoginSessions()
    {
        return new LoginSession[0]; // XXX implement!!!
    }

    protected void handleStream(InputStream is, OutputStream os,
                                boolean isLocal, InetAddress remoteAddr)
    {
        ObjectOutputStream oos = null;
        ProxyInputStream pis = null;

        try {
            pis = new ProxyInputStream(is);
            oos = new ObjectOutputStream(os);
            HttpInvocation hi = (HttpInvocation)pis.readObject();

            LoginSession loginSession = hi.loginSession;
            Object cacheId = hi.cacheId;
            String methodName = hi.methodSignature;
            String definingClass = hi.definingClass;
            URL url = hi.url;

            if (null == cacheId) {
                oos.writeObject(mvvmLoginProxy(url, isLocal, remoteAddr));
                return;
            }

            Object target = getTarget(cacheId);
            if (null == target) {
                oos.writeObject(new InvocationTargetExpiredException
                                ("target expired: " + methodName));
                return;
            }

            Method method = methodByName(target, methodName);

            // XXX setting context-cl to current object's cl may not
            // always be correct, think about this
            ClassLoader targetCl = target.getClass().getClassLoader();
            ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
            // Entering TransformClassLoader ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            Thread.currentThread().setContextClassLoader(targetCl);
            loginSession.setActive();

            Object returnValue = null;
            try {
                Object[] args = (Object[])pis.readObject(targetCl);
                // XXX use Subject.doAs() :
                returnValue = method.invoke(target, args);
            } catch (InvocationTargetException exn) {
                returnValue = exn.getTargetException();
            } catch (Exception exn) {
                logger.warn("exception in RPC call", exn);
                returnValue = exn;
            } finally {
                Thread.currentThread().setContextClassLoader(oldCl);
                // Left TransformClassLoader ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            }

            if (null != returnValue
                && Proxy.isProxyClass(returnValue.getClass())) {
                InvocationHandler ih = Proxy.getInvocationHandler(returnValue);
                if (ih instanceof HttpInvokerStub) {
                    HttpInvokerStub his = (HttpInvokerStub)ih;
                    his.setUrl(url);
                    loginSession = his.getLoginSession();
                }
            } else if (null != returnValue
                       && !(returnValue instanceof Serializable)) {
                returnValue = makeProxy(loginSession, returnValue, url);
            }

            oos.writeObject(returnValue);
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
        // XXX implement
    }

    void destroy()
    {
        // XXX implement
    }

    Object makeProxy(LoginSession ls, Object target, URL url)
        throws Exception
    {
        Map tgtMap = (Map)proxyCache.get(ls);
        if (null == tgtMap) {
            tgtMap = Collections.synchronizedMap(new WeakHashMap());
            proxyCache.put(ls, tgtMap);
        }
        WeakReference wr = (WeakReference)tgtMap.get(target);
        Object pxy = null == wr ? null : wr.get();

        if (null == pxy) {
            ClassLoader cl = target.getClass().getClassLoader();

            Class[] ifaces = (Class[])interfaces(target.getClass())
                .toArray(new Class[0]);

            Object cacheId = (Integer)addTarget(ls, target);
            HttpInvokerStub his = new HttpInvokerStub(ls, url, cacheId, null);
            pxy = Proxy.newProxyInstance(cl, ifaces, his);
            tgtMap.put(target, new WeakReference(pxy));
        }

        return pxy;
    }

    // private methods --------------------------------------------------------

    private Method methodByName(Object target, String name)
    {
        boolean newMethodMap = false;
        WeakReference cmref;
        Map cm;
        synchronized (classMethodsMap) {
            cmref = (WeakReference)classMethodsMap.get(target);
            cm = (cmref == null) ? null : (Map) cmref.get();
            if (null == cm) {
                newMethodMap = true;
                cm = new HashMap();
                classMethodsMap.put(target, new WeakReference(cm));
            }
        }

        Method m;
        synchronized (cm) {
            if (newMethodMap) {
                populateMethodMap(cm, target);
            }
            m = (Method)cm.get(name);
        }

        return m;
    }

    private void populateMethodMap(Map cm, Object t)
    {
        populateMethodMap(cm, t.getClass());
    }

    private void populateMethodMap(Map cm, Class c)
    {
        if (null == c) { return; }

        Method[] m = c.getMethods();
        for (int i = 0; i < m.length; i++) {
            String ms = m[i].toString();
            cm.put(ms, m[i]);
        }

        populateMethodMap(cm, c.getSuperclass());

        Class[] ifcs = c.getInterfaces();
        for (int i = 0; i < ifcs.length; i++) {
            populateMethodMap(cm, ifcs[i]);
        }
    }

    private Set interfaces(Class c)
    {
        Set i;
        synchronized (interfaceMap) {
            i = (Set)interfaceMap.get(c);

            if (null == i) {
                i = interfaces(c, new HashSet());
                interfaceMap.put(c, i);
            }
        }

        return i;
    }

    private Set interfaces(Class[] c, Set l)
    {
        for (int i = 0; i < c.length; i++) {
            interfaces(c[i], l);
        }
        return l;
    }

    private Set interfaces(Class c, Set l)
    {
        // XXX Is there are more structural way of accomplishing this
        if (c.isInterface()
            && !c.getName().endsWith("MvvmLocalContext")) {
            l.add(c);
        }
        interfaces(c.getInterfaces(), l);

        Class superclass = c.getSuperclass();
        if (null != superclass) {
            interfaces(superclass, l);
        }
        return l;
    }

    private Object mvvmLoginProxy(URL url, boolean isLocal, InetAddress remoteAddr) throws Exception
    {
        // We make a temporary login session here that gets replaced when we complete the login.
        return makeProxy(new LoginSession(null, 0, remoteAddr),
                         MvvmContextFactory.context().mvvmLogin(isLocal),
                         url);
    }

    private Object getTarget(Object key)
    {
        WeakReference wr;
        synchronized (targets) {
            wr = (WeakReference)targets.get(key);
        }

        Object target = null == wr ? null : wr.get();

        return target;
    }

    private Object addTarget(LoginSession ls, Object o)
    {
        Set s;
        synchronized (cacheIds) {
            s = (Set)cacheIds.get(ls);
            if (null == s) {
                s = Collections.synchronizedSet(new HashSet());
                cacheIds.put(ls, s);
            }
        }

        Object cacheId;
        synchronized (targets) {
            cacheId = new Integer(targets.size() + 1);
            s.add(cacheId);
            targets.put(cacheId, new WeakReference(o));
        }

        return cacheId;
    }
}
