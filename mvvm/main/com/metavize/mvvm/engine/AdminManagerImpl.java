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

import com.metavize.mvvm.MailSender;
import com.metavize.mvvm.MailSettings;
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

    private static final AdminManagerImpl ADMIN_MANAGER = new AdminManagerImpl();

    private final MvvmLogin mvvmLogin;

    private final Logger logger = Logger.getLogger(AdminManagerImpl.class);

    private AdminSettings adminSettings;


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

        mvvmLogin = MvvmLoginImpl.mvvmLogin();

        logger.info("Initialized AdminManager");
    }

    static AdminManagerImpl adminManager()
    {
        return ADMIN_MANAGER;
    }

    MvvmLogin mvvmLogin() {
        return mvvmLogin;
    }

    public MailSettings getMailSettings()
    {
        MailSender ms = MvvmContextFactory.context().mailSender();
        return ms.getMailSettings();
    }

    public void setMailSettings(MailSettings settings)
    {
        MailSender ms = MvvmContextFactory.context().mailSender();
        ms.setMailSettings(settings);
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
