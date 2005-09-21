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

import java.net.InetAddress;
import javax.security.auth.login.FailedLoginException;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.client.MultipleLoginsException;
import com.metavize.mvvm.client.MvvmRemoteContext;
import com.metavize.mvvm.security.LoginFailureReason;
import com.metavize.mvvm.security.LoginSession;
import com.metavize.mvvm.security.MvvmLogin;
import com.metavize.mvvm.security.MvvmPrincipal;
import com.metavize.mvvm.security.PasswordUtil;
import com.metavize.mvvm.security.User;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.apache.log4j.Logger;

class MvvmLoginImpl implements MvvmLogin
{
    private static final String SYSTEM_USER = "localadmin";
    private static final String SYSTEM_PASSWORD = "nimda11lacol";
    // Add two seconds to each failed login attempt to blunt the force
    // of scripted dictionary attacks.
    private static final long LOGIN_FAIL_SLEEP_TIME = 2000;

    private static final MvvmLoginImpl MVVM_LOGIN = new MvvmLoginImpl();

    private final Logger logger = Logger.getLogger(MvvmLoginImpl.class);
    private final Logger eventLogger = MvvmContextFactory.context()
        .eventLogger();

    private int loginId = 0;

    // Constructors -----------------------------------------------------------

    private MvvmLoginImpl() { }

    // factories --------------------------------------------------------------

    static MvvmLoginImpl mvvmLogin()
    {
        return MVVM_LOGIN;
    }

    // MvvmLogin methods ------------------------------------------------------

    public MvvmRemoteContext interactiveLogin(String login, String password,
                                              boolean force)
        throws FailedLoginException, MultipleLoginsException
    {
        HttpInvoker invoker = HttpInvoker.invoker();
        InetAddress clientAddr = invoker.getClientAddr();

        User user = null;
        Session s = MvvmContextFactory.context().openSession();
        try {
            Transaction tx = s.beginTransaction();

            Query q = s.createQuery("from User u where u.login = :login");
            q.setString("login", login);
            user = (User)q.uniqueResult();

            tx.commit();
        } catch (HibernateException exn) {
            logger.warn("could not get User: " + login, exn);
        } finally {
            if (null != s) {
                try {
                    s.close();
                } catch (HibernateException exn) {
                    logger.warn("could not close session", exn);
                }
            }
        }

        if (null == user) {
            logger.debug("no user found with login: " + login);
            eventLogger.info(new LoginEvent(clientAddr, login, false, false,
                                            LoginFailureReason.UNKNOWN_USER));
            try {
                Thread.sleep(LOGIN_FAIL_SLEEP_TIME);
            } catch (InterruptedException exn) { /* whateva */ }

            throw new FailedLoginException("no such user: " + login);
        } else if (!PasswordUtil.check(password, user.getPassword())) {
            logger.debug("password check failed");
            eventLogger.info(new LoginEvent(clientAddr, login, false, false,
                                            LoginFailureReason.BAD_PASSWORD));

            try {
                Thread.sleep(LOGIN_FAIL_SLEEP_TIME);
            } catch (InterruptedException exn) { /* whateva */ }

            throw new FailedLoginException("incorrect password");
        } else {
            logger.debug("password check succeeded");
            eventLogger.info(new LoginEvent(clientAddr, login, false, true));

            return login(login, clientAddr, LoginSession.LoginType.INTERACTIVE,
                         force);
        }

    }

    public MvvmRemoteContext systemLogin(String username, String password)
        throws FailedLoginException
    {
        HttpInvoker invoker = HttpInvoker.invoker();
        InetAddress clientAddr = invoker.getClientAddr();

        // Do something better here. XXX
        logger.debug("attempting local login");
        if (!clientAddr.isLoopbackAddress()) {
            logger.debug("systemLogin failed, not localhost");

            eventLogger.info(new LoginEvent(clientAddr, username, true,
                                            false));

            try {
                Thread.sleep(LOGIN_FAIL_SLEEP_TIME);
            } catch (InterruptedException exn) { /* whateva */ }

            throw new FailedLoginException("system login not from localhost");
        } else if (!isSystemLogin(username, password)) {
            eventLogger.info(new LoginEvent(clientAddr, username, true,
                                            false));
            try {
                Thread.sleep(LOGIN_FAIL_SLEEP_TIME);
            } catch (InterruptedException exn) { /* whateva */ }

            throw new FailedLoginException("bad system login");
        } else {
            logger.debug("local login succeeded");
            eventLogger.info(new LoginEvent(clientAddr, username, true, true));

            return login(username, clientAddr, LoginSession.LoginType.SYSTEM,
                         false);
        }
    }

    private MvvmRemoteContext login(String username, InetAddress clientAddr,
                                    LoginSession.LoginType loginType,
                                    boolean force)
    {
        HttpInvoker invoker = HttpInvoker.invoker();

        MvvmPrincipal mp = new MvvmPrincipal(username);

        LoginSession loginSession = new LoginSession(mp, nextLoginId(),
                                                     clientAddr, loginType);

        invoker.login(loginSession, force);

        return MvvmContextImpl.getInstance().remoteContext();
    }

    // private methods --------------------------------------------------------

    private boolean isSystemLogin(String username, String passwd)
    {
        return username.equals(SYSTEM_USER) && passwd.equals(SYSTEM_PASSWORD);
    }

    private int nextLoginId()
    {
        synchronized (this) {
            return ++loginId;
        }
    }
}
