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
import com.metavize.mvvm.security.User;
import com.metavize.mvvm.util.TransactionWork;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import java.util.TimeZone;
import java.io.IOException;
import javax.transaction.TransactionRolledbackException;

class AdminManagerImpl implements AdminManager
{
    private static final String INITIAL_USER_NAME = "System Administrator";
    private static final String INITIAL_USER_LOGIN = "admin";
    private static final String INITIAL_USER_PASSWORD = "passwd";

    private static final AdminManagerImpl ADMIN_MANAGER = new AdminManagerImpl();

    private static final String SET_TIMEZONE_SCRIPT;

    private final MvvmLoginImpl mvvmLogin;

    private final Logger logger = Logger.getLogger(AdminManagerImpl.class);

    private AdminSettings adminSettings;

    private AdminManagerImpl()
    {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    Query q = s.createQuery("from AdminSettings");
                    adminSettings = (AdminSettings)q.uniqueResult();

                    if (null == adminSettings) {
                        adminSettings = new AdminSettings();
                        adminSettings.addUser(new User(INITIAL_USER_LOGIN,
                                                       INITIAL_USER_PASSWORD,
                                                       INITIAL_USER_NAME));
                        s.save(adminSettings);

                    }
                    return true;
                }

                public Object getResult() { return null; }
            };
        MvvmContextFactory.context().runTransaction(tw);

        mvvmLogin = MvvmLoginImpl.mvvmLogin();

        logger.info("Initialized AdminManager");
    }

    static AdminManagerImpl adminManager()
    {
        return ADMIN_MANAGER;
    }

    MvvmLoginImpl mvvmLogin() {
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

    public void setAdminSettings(final AdminSettings as)
    {
        // Do something with summaryPeriod? XXX
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    s.saveOrUpdate(as);
                    return true;
                }

                public Object getResult() { return null; }
            };
        MvvmContextFactory.context().runTransaction(tw);

        this.adminSettings = as;
    }

    public LoginSession[] loggedInUsers()
    {
        return HttpInvoker.invoker().getLoginSessions();
    }

    public TimeZone getTimeZone()
    {
        return TimeZone.getDefault();
    }

    public void setTimeZone(TimeZone timezone)
        throws TransactionRolledbackException
    {
        TimeZone.setDefault(timezone);
        String id = timezone.getID();

        try {
            Process p = Runtime.getRuntime().exec(new String[] { SET_TIMEZONE_SCRIPT, id });
            for (byte[] buf = new byte[1024]; 0 <= p.getInputStream().read(buf); );
            int exitValue = p.waitFor();
            if (0 != exitValue) {
                String message = "Unable to set time zone (" + exitValue + ") to: " + id;
                logger.error(message);
                throw new TransactionRolledbackException(message);
            } else {
                logger.info("Time zone set to : " + id);
            }
        } catch (InterruptedException exn) {
            String message = "Interrupted during set time zone";
            logger.error(message);
            throw new TransactionRolledbackException(message);
        } catch (IOException exn) {
            String message = "Exception during set time zone to: " + id;
            logger.error(message, exn);
            throw new TransactionRolledbackException(message);
        }
    }

    static {
        SET_TIMEZONE_SCRIPT = System.getProperty("bunnicula.home")
            + "/../../bin/mvtimezone";
    }
}
