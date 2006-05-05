/*
 * Copyright (c) 2006 Metavize Inc.
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.naming.ServiceUnavailableException;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.MvvmLocalContext;
import com.metavize.mvvm.addrbook.*;
import com.metavize.mvvm.logging.EventLogger;
import com.metavize.mvvm.portal.*;
import com.metavize.mvvm.security.LoginFailureReason;
import com.metavize.mvvm.security.LogoutReason;
import com.metavize.mvvm.util.TransactionWork;
import jcifs.smb.NtlmPasswordAuthentication;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

/**
 * Implementation of the PortalManager
 */
class PortalManagerImpl implements LocalPortalManager
{
    private static final Application[] protoArr = new Application[] { };

    public static final long REAPING_FREQ = 5000;

    // Add two seconds to each failed login attempt to blunt the force
    // of scripted dictionary attacks.
    private static final long LOGIN_FAIL_SLEEP_TIME = 2000;

    private static final PortalManagerImpl PORTAL_MANAGER = new PortalManagerImpl();

    private final Logger logger = Logger.getLogger(PortalManagerImpl.class);

    private EventLogger eventLogger;

    private AddressBook addressBook;

    private PortalApplicationManagerImpl appManager;
    private RemotePortalApplicationManagerImpl remoteAppManager;

    private PortalSettings portalSettings;

    private Map<PortalLoginKey,PortalLogin> activeLogins;

    private TimeoutReaper reaper = null;

    private PortalManagerImpl() {
        MvvmLocalContext ctx = MvvmContextFactory.context();

        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    Query q = s.createQuery("from PortalSettings");
                    portalSettings = (PortalSettings)q.uniqueResult();

                    if (null == portalSettings) {
                        portalSettings = new PortalSettings();
                        PortalGlobal pg = new PortalGlobal();
                        pg.setPortalHomeSettings(new PortalHomeSettings());
                        portalSettings.setGlobal(pg);
                        s.save(portalSettings);

                    }
                    return true;
                }
            };
        ctx.runTransaction(tw);

        appManager = PortalApplicationManagerImpl.applicationManager();
        remoteAppManager = new RemotePortalApplicationManagerImpl(appManager);

        addressBook = ctx.appAddressBook();

        activeLogins = new ConcurrentHashMap<PortalLoginKey,PortalLogin>();

        eventLogger = ctx.eventLogger();

        reaper = new TimeoutReaper(REAPING_FREQ);
        new Thread(reaper).start();

        logger.info("Initialized PortalManager");
    }

    /**
     * Do not call this directly, instead go through <code>MvvmLocalContext</code>
     */
    static PortalManagerImpl getInstance() {
        return PORTAL_MANAGER;
    }

    public PortalApplicationManagerImpl applicationManager() {
        return appManager;
    }

    public RemoteApplicationManager remoteApplicationManager() {
        return remoteAppManager;
    }

    public PortalSettings getPortalSettings()
    {
        return portalSettings;
    }

    public void setPortalSettings(final PortalSettings settings)
    {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    s.saveOrUpdate(settings);
                    return true;
                }
            };
        MvvmContextFactory.context().runTransaction(tw);

        this.portalSettings = settings;
    }

    void destroy() {
    }

    public List<Bookmark> getAllBookmarks(PortalUser user)
    {
        List<Bookmark> result = new ArrayList<Bookmark>();

        List userBookmarks = user.getBookmarks();
        if (userBookmarks != null)
            result.addAll(userBookmarks);

        PortalGroup userGroup = user.getPortalGroup();
        if (userGroup != null) {
            List groupBookmarks = userGroup.getBookmarks();
            if (groupBookmarks != null)
                result.addAll(groupBookmarks);
        }

        List globalBookmarks = portalSettings.getGlobal().getBookmarks();
        if (globalBookmarks != null)
            result.addAll(globalBookmarks);

        return result;
    }

    public PortalHomeSettings getPortalHomeSettings(PortalUser user)
    {
        PortalHomeSettings result = user.getPortalHomeSettings();
        if (result == null) {
            PortalGroup userGroup = user.getPortalGroup();
            if (userGroup != null) {
                result = userGroup.getPortalHomeSettings();
            }
        }
        if (result == null) {
            result = portalSettings.getGlobal().getPortalHomeSettings();
        }
        return result;
    }

    public Bookmark addUserBookmark(final PortalUser user, String name, Application application, String target)
    {
        Bookmark result = user.addBookmark(name, application, target);

        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    s.saveOrUpdate(user);
                    return true;
                }
            };
        MvvmContextFactory.context().runTransaction(tw);

        return result;
    }

    public void removeUserBookmark(final PortalUser user, Bookmark bookmark)
    {
        user.removeBookmark(bookmark);

        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    s.saveOrUpdate(user);
                    return true;
                }
            };
        MvvmContextFactory.context().runTransaction(tw);
    }


    public PortalUser getUser(String uid)
    {
        List allUsers = portalSettings.getUsers();
        for (Iterator iter = allUsers.iterator(); iter.hasNext();) {
            PortalUser user = (PortalUser) iter.next();
            if (uid.equals(user.getUid()))
                return user;
        }
        return null;
    }

    public List<PortalLogin> getActiveLogins()
    {
        return new ArrayList<PortalLogin>(activeLogins.values());
    }

    public void forceLogout(PortalLogin login)
    {
        if (login == null)
            throw new NullPointerException("login cannot be null");
        for (Iterator<PortalLoginKey> iter = activeLogins.keySet().iterator(); iter.hasNext();) {
            PortalLoginKey key = iter.next();
            PortalLogin l = activeLogins.get(key);
            if (l.equals(login)) {
                doLogout(key, LogoutReason.ADMINISTRATOR);
                return;
            }
        }
        logger.warn("Tried to logout missing login with uid " + login.getUser());
    }

    public void logout(PortalLoginKey loginKey)
    {
        doLogout(loginKey, LogoutReason.USER);
    }

    public PortalLogin getLogin(PortalLoginKey key)
    {
        if (key == null)
            throw new NullPointerException("login key cannot be null");
        return activeLogins.get(key);
    }

    public PortalLoginKey login(String uid, String password, InetAddress clientAddr)
    {
        try {
            boolean authenticated = addressBook.authenticate(uid, password);

            if (!authenticated) {
                UserEntry ue = addressBook.getEntry(uid);
                if (ue == null) {
                    logger.debug("no user found with login: " + uid);
                    PortalLoginEvent event = new PortalLoginEvent(clientAddr, uid, false, LoginFailureReason.UNKNOWN_USER);
                    eventLogger.log(event);
                } else {
                    logger.debug("password check failed");
                    PortalLoginEvent event = new PortalLoginEvent(clientAddr, uid, false, LoginFailureReason.BAD_PASSWORD);
                    eventLogger.log(event);
                }
                try {
                    Thread.sleep(LOGIN_FAIL_SLEEP_TIME);
                } catch (InterruptedException exn) { }
                return null;
            }
        } catch (ServiceUnavailableException x) {
            logger.error("Unable to authenticate user", x);
            return null;
        }

        PortalUser user = getUser(uid);

        if (user == null && portalSettings.getGlobal().isAutoCreateUsers()) {
            logger.debug("lookupUser: " + uid + " didn't exist, auto creating");
            PortalUser newUser = portalSettings.addUser(uid);
            setPortalSettings(portalSettings);
            user = newUser;
        }

        if (!user.isLive()) {
            logger.debug("user is disabled");
            PortalLoginEvent event = new PortalLoginEvent(clientAddr, uid, false, LoginFailureReason.DISABLED);
            eventLogger.log(event);
            return null;
        }

        // Make up a descriptor so the key prints nicely
        String desc = uid + clientAddr;
        PortalLoginKey key = new PortalLoginKey(desc);

        NtlmPasswordAuthentication pwa = null;
        String domain = getCurrentDomain();
        if (domain != null)
            pwa = new NtlmPasswordAuthentication(domain, uid, password);
        PortalLogin login = new PortalLogin(user, clientAddr, pwa);
        activeLogins.put(key, login);

        PortalLoginEvent event = new PortalLoginEvent(clientAddr, uid, true);
        eventLogger.log(event);

        return key;
    }

    private String getCurrentDomain() {
        AddressBookSettings s = addressBook.getAddressBookSettings();
        if (s.getAddressBookConfiguration() == AddressBookConfiguration.AD_AND_LOCAL)
            return s.getADRepositorySettings().getDomain();
        return null;
    }

    private void doLogout(PortalLoginKey loginKey, LogoutReason reason)
    {
        PortalLogin login = activeLogins.get(loginKey);
        if (login == null) {
            logger.warn("ignoring doLogout for already logged out key " + loginKey);
            return;
        }

        if (reason == LogoutReason.ADMINISTRATOR)
            logger.warn("Administrative logout of " + login);
        else
            logger.info("" + reason + " logout of " + login);

        activeLogins.remove(loginKey);

        PortalLogoutEvent event = new PortalLogoutEvent(login.getClientAddr(), login.getUser(),
                                                        reason);
        eventLogger.log(event);
    }

    private long getIdleTimeout(PortalUser user)
    {
        PortalHomeSettings hs = getPortalHomeSettings(user);
        return hs.getIdleTimeout();
    }


    private class TimeoutReaper implements Runnable
    {
        private long millifreq;

        private volatile Thread thread;

        TimeoutReaper(long millifreq)
        {
            this.millifreq = millifreq;
        }

        public void destroy()
        {
            Thread t = this.thread;
            if (null != t) {
                this.thread = null;
                t.interrupt();
            }
        }

        public void run()
        {
            thread = Thread.currentThread();

            while (thread == Thread.currentThread()) {
                try {
                    Thread.sleep(millifreq);
                }
                catch (InterruptedException e) {
                    continue;
                }

                checkTimeouts();
            }
        }

        private void checkTimeouts()
        {
            for (Iterator<PortalLoginKey> iter = activeLogins.keySet().iterator(); iter.hasNext();) {
                PortalLoginKey key = iter.next();
                if (key == null)
                    // Concurrency
                    continue;
                PortalLogin login = activeLogins.get(key);
                if (login == null)
                    // Concurrency
                    continue;

                PortalUser user = getUser(login.getUser());
                if (user == null) {
                    // User deleted by admin
                    doLogout(key, LogoutReason.ADMINISTRATOR);
                } else {
                    long timeout = getIdleTimeout(user);
                    if (login.getIdleTime() >= timeout)
                        doLogout(key, LogoutReason.TIMEOUT);
                }
            }
        }
    }

}
