/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: AdminManagerImpl.java,v 1.10 2005/02/07 21:25:27 jdi Exp $
 */

package com.metavize.mvvm.engine;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.security.AdminManager;
import com.metavize.mvvm.security.AdminSettings;
import com.metavize.mvvm.security.LoginSession;
import com.metavize.mvvm.security.MvvmLogin;
import com.metavize.mvvm.security.User;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Query;
import net.sf.hibernate.Session;
import net.sf.hibernate.Transaction;
import org.apache.log4j.Logger;

class AdminManagerImpl implements AdminManager
{
    private static final String INITIAL_USER_NAME = "System Administrator";
    private static final String INITIAL_USER_LOGIN = "admin";
    private static final String INITIAL_USER_PASSWORD = "passwd";

    private static final Object LOCK = new Object();
    private static AdminManagerImpl ADMIN_MANAGER;

    private MvvmLogin mvvmLoginRemote;
    private MvvmLogin mvvmLoginLocal;

    private AdminSettings adminSettings;

    private static Logger logger
        = Logger.getLogger(AdminManager.class.getName());

    private AdminManagerImpl()
    {
        Session s = MvvmContextFactory.context().openSession();
        try {
            Transaction tx = s.beginTransaction();

            Query q = s.createQuery("from AdminSettings as");
            adminSettings = (AdminSettings)q.uniqueResult();

            if (null == adminSettings) {
                adminSettings = new AdminSettings();
                adminSettings.addUser(new User(INITIAL_USER_LOGIN,
                                               INITIAL_USER_PASSWORD,
                                               INITIAL_USER_NAME));
                s.save(adminSettings);
            }

            tx.commit();
        } catch (HibernateException exn) {
            logger.warn("could not get AdminSettings", exn);
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                logger.warn("could not close session", exn);
            }
        }

        mvvmLoginRemote = new MvvmLoginImpl(false);
        mvvmLoginLocal  = new MvvmLoginImpl(true);

        logger.info("Initialized AdminManager");
    }

    static AdminManagerImpl adminManager()
    {
        synchronized (LOCK) {
            if (null == ADMIN_MANAGER) {
                ADMIN_MANAGER = new AdminManagerImpl();
            }
        }

        return ADMIN_MANAGER;
    }

    MvvmLogin mvvmLogin(boolean isLocal) {
        return isLocal ? mvvmLoginLocal : mvvmLoginRemote;
    }

    public AdminSettings getAdminSettings()
    {
        return adminSettings;
    }

    public void setAdminSettings(AdminSettings as)
    {
        // Do something with summaryPeriod? XXX
        Session s = MvvmContextFactory.context().openSession();
        try {
            Transaction tx = s.beginTransaction();

            s.saveOrUpdateCopy(as);

            tx.commit();
        } catch (HibernateException exn) {
            logger.warn("could not save AdminSettings", exn);
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                logger.warn("could not close session", exn); // XXX TransExn
            }
        }

        this.adminSettings = as;
    }

    public LoginSession[] loggedInUsers()
    {
        return HttpInvoker.invoker().getLoginSessions();
    }
}
