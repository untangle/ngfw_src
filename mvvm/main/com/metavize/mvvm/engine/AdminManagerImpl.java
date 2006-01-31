/*
 * Copyright (c) 2004, 2005, 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.engine;

import java.io.*;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.TimeZone;
import javax.transaction.TransactionRolledbackException;

import com.metavize.mvvm.MailSender;
import com.metavize.mvvm.MailSettings;
import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.security.*;
import com.metavize.mvvm.snmp.SnmpManager;
import com.metavize.mvvm.snmp.SnmpManagerImpl;
import com.metavize.mvvm.util.TransactionWork;
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

    private static final AdminManagerImpl ADMIN_MANAGER = new AdminManagerImpl();

    private final MvvmLoginImpl mvvmLogin;

    private final Logger logger = Logger.getLogger(AdminManagerImpl.class);

    private AdminSettings adminSettings;
    private SnmpManager snmpManager;

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
                                                       INITIAL_USER_NAME,
                                                       false));
                        s.save(adminSettings);

                    }
                    return true;
                }
            };
        MvvmContextFactory.context().runTransaction(tw);

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
            };
        MvvmContextFactory.context().runTransaction(tw);

        this.adminSettings = as;
    }

    public LoginSession[] loggedInUsers()
    {
        return HttpInvoker.invoker().getLoginSessions();
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
            Process p = Runtime.getRuntime().exec(new String[] { SET_TIMEZONE_SCRIPT, id });
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
            writer.write(((MvvmContextImpl)MvvmContextFactory.context()).getActivationKey());
            writer.write("&version=");
            writer.write(MvvmContextFactory.context().version());
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
        TomcatManager tm = ((MvvmContextImpl)MvvmContextFactory.context()).getMain().getTomcatManager();
        logger.warn("Generating auth nonce for " + ls.getClientAddr() + " " + ls.getMvvmPrincipal());
        return tm.generateAuthNonce(ls.getClientAddr(), ls.getMvvmPrincipal());
    }


    // Helper class for dealing with form data
    static class FormUtil {

        static Hashtable emptyHashtable = new Hashtable();

        private FormUtil() {}

        static Hashtable parseQueryString(String s) {

            String valArray[] = null;

            if (s == null) {
                throw new IllegalArgumentException();
            }
            Hashtable ht = new Hashtable();
            StringBuffer sb = new StringBuffer();
            StringTokenizer st = new StringTokenizer(s, "&");
            while (st.hasMoreTokens()) {
                String pair = (String)st.nextToken();
                int pos = pair.indexOf('=');
                if (pos == -1) {
                    // XXX
                    // should give more detail about the illegal argument
                    throw new IllegalArgumentException();
                }
                String key = parseName(pair.substring(0, pos), sb);
                String val = parseName(pair.substring(pos+1, pair.length()), sb);
                if (ht.containsKey(key)) {
                    String oldVals[] = (String []) ht.get(key);
                    valArray = new String[oldVals.length + 1];
                    for (int i = 0; i < oldVals.length; i++)
                        valArray[i] = oldVals[i];
                    valArray[oldVals.length] = val;
                } else {
                    valArray = new String[1];
                    valArray[0] = val;
                }
                ht.put(key, valArray);
            }
            return ht;
        }

        static Hashtable parsePostData(File file)
        {
            try {
                InputStream in = new FileInputStream(file);
                int len = (int) file.length();

                int inputLen, offset;
                byte[] postedBytes = null;
                String postedBody;

                // XXX
                // should a length of 0 be an IllegalArgumentException

                if (len <=0)
                    return new Hashtable(); // cheap hack to return an empty hash

                if (in == null) {
                    throw new IllegalArgumentException();
                }

                //
                // Make sure we read the entire POSTed body.
                //
                postedBytes = new byte [len];
                offset = 0;
                do {
                    inputLen = in.read (postedBytes, offset, len - offset);
                    if (inputLen <= 0) {
                        throw new IOException ("short read from form data file");
                    }
                    offset += inputLen;
                } while ((len - offset) > 0);
                in.close();

                // XXX we shouldn't assume that the only kind of POST body
                // is FORM data encoded using ASCII or ISO Latin/1 ... or
                // that the body should always be treated as FORM data.

                postedBody = new String(postedBytes, 0, len);

                return parseQueryString(postedBody);
            } catch (IOException e) {
                return emptyHashtable;
            }

        }

        static private String parseName(String s, StringBuffer sb) {
            sb.setLength(0);
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                switch (c) {
                case '+':
                    sb.append(' ');
                    break;
                case '%':
                    try {
                        sb.append((char) Integer.parseInt(s.substring(i+1, i+3),
                                                          16));
                        i += 2;
                    } catch (NumberFormatException e) {
                        // XXX
                        // need to be more specific about illegal arg
                        throw new IllegalArgumentException();
                    } catch (StringIndexOutOfBoundsException e) {
                        String rest  = s.substring(i);
                        sb.append(rest);
                        if (rest.length()==2)
                            i++;
                    }

                    break;
                default:
                    sb.append(c);
                    break;
                }
            }
            return sb.toString();
        }
    }
}
