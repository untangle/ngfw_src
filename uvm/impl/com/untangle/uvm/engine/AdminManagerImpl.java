/*
 * $HeadURL:$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.uvm.engine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Hashtable;
import java.util.TimeZone;
import javax.transaction.TransactionRolledbackException;

import com.untangle.uvm.MailSender;
import com.untangle.uvm.MailSettings;
import com.untangle.uvm.security.AdminManager;
import com.untangle.uvm.security.AdminSettings;
import com.untangle.uvm.security.LoginSession;
import com.untangle.uvm.security.RegistrationInfo;
import com.untangle.uvm.security.User;
import com.untangle.uvm.snmp.SnmpManager;
import com.untangle.uvm.snmp.SnmpManagerImpl;
import com.untangle.uvm.util.FormUtil;
import com.untangle.uvm.util.TransactionWork;
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

    private final UvmContextImpl uvmContext;
    private final UvmLoginImpl uvmLogin;

    private final Logger logger = Logger.getLogger(AdminManagerImpl.class);

    private AdminSettings adminSettings;
    private SnmpManager snmpManager;

    AdminManagerImpl(UvmContextImpl uvmContext)
    {
        this.uvmContext = uvmContext;

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
        uvmContext.runTransaction(tw);

        uvmLogin = UvmLoginImpl.uvmLogin();

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

    UvmLoginImpl uvmLogin() {
        return uvmLogin;
    }

    public MailSettings getMailSettings()
    {
        MailSender ms = uvmContext.mailSender();
        return ms.getMailSettings();
    }

    public void setMailSettings(MailSettings settings)
    {
        MailSender ms = uvmContext.mailSender();
        ms.setMailSettings(settings);
    }

    public boolean sendTestMessage(String recipient)
    {
        MailSender ms = uvmContext.mailSender();
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
                    s.saveOrUpdate(as);
                    return true;
                }
            };
        uvmContext.runTransaction(tw);

        this.adminSettings = as;
    }

    public LoginSession[] loggedInUsers()
    {
        return HttpInvokerImpl.invoker().getLoginSessions();
    }

    public void logout()
    {
        HttpInvokerImpl.invoker().logoutActiveLogin();
    }

    public LoginSession whoAmI()
    {
        return HttpInvokerImpl.invoker().getActiveLogin();
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
            Process p = uvmContext.exec(new String[] { SET_TIMEZONE_SCRIPT, id });
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
            writer.write(uvmContext.getActivationKey());
            writer.write("&version=");
            writer.write(uvmContext.version());
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
        HttpInvokerImpl invoker = HttpInvokerImpl.invoker();
        LoginSession ls = invoker.getActiveLogin();
        if (ls == null)
            throw new IllegalStateException("generateAuthNonce called from backend");
        TomcatManager tm = uvmContext.tomcatManager();
        logger.info("Generating auth nonce for " + ls.getClientAddr() + " " + ls.getUvmPrincipal());
        return tm.generateAuthNonce(ls.getClientAddr(), ls.getUvmPrincipal());
    }
}
