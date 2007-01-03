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

package com.untangle.mvvm.engine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Hashtable;
import java.util.TimeZone;
import javax.transaction.TransactionRolledbackException;

import com.untangle.mvvm.MailSender;
import com.untangle.mvvm.MailSettings;
import com.untangle.mvvm.security.AdminManager;
import com.untangle.mvvm.security.AdminSettings;
import com.untangle.mvvm.security.LoginSession;
import com.untangle.mvvm.security.RegistrationInfo;
import com.untangle.mvvm.security.User;
import com.untangle.mvvm.snmp.SnmpManager;
import com.untangle.mvvm.snmp.SnmpManagerImpl;
import com.untangle.mvvm.util.FormUtil;
import com.untangle.mvvm.util.TransactionWork;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

class AdminManagerImpl implements AdminManager
{
    private static final String INITIAL_USER_NAME = "System Administrator";
    private static final String INITIAL_USER_LOGIN = "admin";
    private static final String INITIAL_USER_PASSWORD = "passwd";

    private static final String SET_TIMEZONE_SCRIPT = System.getProperty("bunnicula.home")
        + "/../../bin/mvtimezone";
    private static final String TIMEZONE_FILE = System.getProperty("bunnicula.conf.dir")
        + "/timezone";
    private static final String REGISTRATION_INFO_FILE = System.getProperty("bunnicula.home")
        + "/registration.info";

    private final MvvmContextImpl mvvmContext;
    private final MvvmLoginImpl mvvmLogin;

    private final Logger logger = Logger.getLogger(AdminManagerImpl.class);

    private AdminSettings adminSettings;
    private SnmpManager snmpManager;

    AdminManagerImpl(MvvmContextImpl mvvmContext)
    {
        this.mvvmContext = mvvmContext;

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
                                                       INITIAL_USER_NAME,
                                                       false));
                        s.save(adminSettings);

                    }
                    return true;
                }
            };
        mvvmContext.runTransaction(tw);

        mvvmLogin = MvvmLoginImpl.mvvmLogin();

        snmpManager = SnmpManagerImpl.snmpManager();

        // If timezone on box is different (example: kernel upgrade), reset it:
        TimeZone currentZone = getTimeZone();
        if (!currentZone.equals(TimeZone.getDefault()))
            try {
                setTimeZone(currentZone);
            } catch (Exception x) {
                // Already logged.
            }

        logger.info("Initialized AdminManager");
    }

    MvvmLoginImpl mvvmLogin() {
        return mvvmLogin;
    }

    public MailSettings getMailSettings()
    {
        MailSender ms = mvvmContext.mailSender();
        return ms.getMailSettings();
    }

    public void setMailSettings(MailSettings settings)
    {
        MailSender ms = mvvmContext.mailSender();
        ms.setMailSettings(settings);
    }

    public boolean sendTestMessage(String recipient)
    {
        MailSender ms = mvvmContext.mailSender();
        return ms.sendTestMessage(recipient);
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
                    s.merge(as);
                    return true;
                }
            };
        mvvmContext.runTransaction(tw);

        this.adminSettings = as;
    }

    public LoginSession[] loggedInUsers()
    {
        return HttpInvoker.invoker().getLoginSessions();
    }

    public void logout()
    {
        HttpInvoker.invoker().logoutActiveLogin();
    }

    public LoginSession whoAmI()
    {
        return HttpInvoker.invoker().getActiveLogin();
    }

    public TimeZone getTimeZone()
    {
        try {
            BufferedReader in = new BufferedReader(new FileReader(TIMEZONE_FILE));
            String str = in.readLine();
            str = str.trim();
            in.close();
            TimeZone current = TimeZone.getTimeZone(str);
            return current;
        } catch (Exception x) {
            logger.warn("Unable to get timezone, using java default:" , x);
            return TimeZone.getDefault();
        }
    }

    public void setTimeZone(TimeZone timezone)
        throws TransactionRolledbackException
    {
        String id = timezone.getID();

        try {
            Process p = mvvmContext.exec(new String[] { SET_TIMEZONE_SCRIPT, id });
            for (byte[] buf = new byte[1024]; 0 <= p.getInputStream().read(buf); );
            int exitValue = p.waitFor();
            if (0 != exitValue) {
                String message = "Unable to set time zone (" + exitValue + ") to: " + id;
                logger.error(message);
                throw new TransactionRolledbackException(message);
            } else {
                logger.info("Time zone set to : " + id);
                TimeZone.setDefault(timezone); // Note: Only works for threads who haven't yet cached the zone!  XX
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

    public Date getDate()
    {
        return new Date(System.currentTimeMillis());
    }

    public void setRegistrationInfo(RegistrationInfo info)
        throws TransactionRolledbackException
    {
        File regFile = new File(REGISTRATION_INFO_FILE);
        if (regFile.exists()) {
            if (!regFile.delete()) {
                String message = "Unable to remove old registration info";
                logger.error(message);
                throw new TransactionRolledbackException(message);
            }
        }

        try {
            FileWriter writer = new FileWriter(regFile);
            writer.write("regKey=");
            writer.write(mvvmContext.getActivationKey());
            writer.write("&version=");
            writer.write(mvvmContext.version());
            writer.write("&");
            writer.write(info.toForm());
            writer.close();
        } catch (IOException exn) {
            String message = "Exception during writing registration info: " + info;
            logger.error(message, exn);
            throw new TransactionRolledbackException(message);
        }
    }

    public RegistrationInfo getRegistrationInfo() {
        File regFile = new File(REGISTRATION_INFO_FILE);
        if (!regFile.exists())
            return null;

        try {
            Hashtable entries = FormUtil.parsePostData(regFile);
            return new RegistrationInfo(entries);
        } catch (Exception exn) {
            logger.error("Exception during parsing registration info: ", exn);
            return null;
        }
    }

    public SnmpManager getSnmpManager() {
      return snmpManager;
    }

    public String generateAuthNonce() {
        HttpInvoker invoker = HttpInvoker.invoker();
        LoginSession ls = invoker.getActiveLogin();
        if (ls == null)
            throw new IllegalStateException("generateAuthNonce called from backend");
        TomcatManager tm = mvvmContext.tomcatManager();
        logger.info("Generating auth nonce for " + ls.getClientAddr() + " " + ls.getMvvmPrincipal());
        return tm.generateAuthNonce(ls.getClientAddr(), ls.getMvvmPrincipal());
    }
}
