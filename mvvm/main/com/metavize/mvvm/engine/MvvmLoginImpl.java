/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: MvvmLoginImpl.java,v 1.8 2005/02/07 21:25:27 jdi Exp $
 */

package com.metavize.mvvm.engine;

import javax.security.auth.login.FailedLoginException;

import com.metavize.mvvm.MvvmContext;
import com.metavize.mvvm.MvvmContextFactory;
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
    private static final Object LOCK = new Object();
    private static final Logger logger = Logger
        .getLogger(MvvmLoginImpl.class.getName());

    private static int loginId = 0; /* have LOCK */

    private boolean isLocal;

    MvvmLoginImpl(boolean isLocal)
    {
        super();
        this.isLocal = isLocal;
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
                success = true;
            } else {
                logger.debug("Failed local, trying normal");
            }
        }

        if (!success) {
            Session s = MvvmContextFactory.context().openSession();
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
                        success = true;
                    } else {
                        logger.debug("Password check failed");
                        throw new FailedLoginException("Incorrect password");
                    }
                }

                tx.commit();
            } catch (HibernateException exn) {
                logger.warn("could not get User: " + login, exn);
            } finally {
                try {
                    s.close();
                } catch (HibernateException exn) {
                    logger.warn("could not close session", exn);
                }
            }

            if (!success) {
                logger.debug("No user found with login: " + login);
                throw new FailedLoginException("No such user: " + login);
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
