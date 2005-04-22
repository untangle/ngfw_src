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

import javax.security.auth.login.FailedLoginException;

import com.metavize.mvvm.MvvmContext;
import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.security.LoginFailureReason;
import com.metavize.mvvm.security.LoginSession;
import com.metavize.mvvm.security.MvvmLogin;
import com.metavize.mvvm.security.MvvmLoginException;
import com.metavize.mvvm.security.MvvmPrincipal;
import com.metavize.mvvm.security.PasswordUtil;
import com.metavize.mvvm.security.User;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Query;
import net.sf.hibernate.Session;
import net.sf.hibernate.Transaction;
import org.apache.log4j.Logger;

class MvvmLoginImpl implements MvvmLogin
{
    private static final long LOGIN_FAIL_SLEEP_TIME = 5000;

    private static final Object LOCK = new Object();
    private static final Logger logger = Logger
        .getLogger(MvvmLoginImpl.class.getName());

    private Logger eventLogger;

    private static int loginId = 0; /* have LOCK */

    private boolean isLocal;

    MvvmLoginImpl(boolean isLocal)
    {
        super();
        this.isLocal = isLocal;
        this.eventLogger = MvvmContextFactory.context().eventLogger();
    }

    public MvvmContext login(String login, String password)
        throws FailedLoginException
    {
        boolean success = false;
        if (isLocal) {
            // Do something better here. XXX
            logger.debug("Attempting local login");
            String localUser = "localadmin";
            String localPasswd = "nimda11lacol";
            if (login.equals(localUser) &&
                password.equals(localPasswd)) {
                logger.debug("Local login succeeded");
                eventLogger.info(new LoginEvent(localUser, true, true));
                success = true;
            } else {
                eventLogger.info(new LoginEvent(localUser, true, false));
                logger.debug("Failed local, trying normal");
            }
        }

        if (!success) {
            Session s = MvvmContextFactory.context().openSession();
            FailedLoginException x = null;
            try {
                Transaction tx = s.beginTransaction();

                Query q = s.createQuery
                    ("from User u where u.login = :login");
                q.setString("login", login);
                User u = (User)q.uniqueResult();

                if (null != u) {
                    logger.debug("Attempting login of user: " + u.getLogin());
                    if (PasswordUtil.check(password, u.getPassword())) {
                        logger.debug("Password check succeeded");
                        // Just use login, not id, so it can be congruent with "localadmin" from above.
                        eventLogger.info(new LoginEvent(login, false, true));
                        success = true;
                    } else {
                        logger.debug("Password check failed");
                        eventLogger.info(new LoginEvent(login, false, false, LoginFailureReason.BAD_PASSWORD));
                        Thread.sleep(LOGIN_FAIL_SLEEP_TIME);
                        x = new FailedLoginException("Incorrect password");
                    }
                } else {
                    logger.debug("No user found with login: " + login);
                    eventLogger.info(new LoginEvent(login, false, false, LoginFailureReason.UNKNOWN_USER));
                    Thread.sleep(LOGIN_FAIL_SLEEP_TIME);
                    x = new FailedLoginException("No such user: " + login);
                }

                tx.commit();
            } catch (HibernateException exn) {
                logger.warn("could not get User: " + login, exn);
            } catch (InterruptedException exn) {
                // Can't really happen.
                logger.warn("interrupted in mvvmlogin backend");
            } finally {
                try {
                    s.close();
                } catch (HibernateException exn) {
                    logger.warn("could not close session", exn);
                }
            }

            if (!success) {
                throw x;
            }
        }

        MvvmPrincipal mp = new MvvmPrincipal(login);

        // XXX Throw MvvmLoginException on failure!
        try {
            return (MvvmContext)HttpInvoker.invoker()
                .makeProxy(new LoginSession(mp, nextLoginId()),
                           MvvmContextFactory.context(), null);
        } catch (Exception exn) {
            throw new RuntimeException(exn); // XXX
        }
    }

    private int nextLoginId()
    {
        synchronized (LOCK) {
            return ++loginId;
        }
    }
}
