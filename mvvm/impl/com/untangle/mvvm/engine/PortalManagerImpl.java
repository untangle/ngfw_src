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

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.naming.ServiceUnavailableException;
import javax.servlet.http.HttpSession;

import com.untangle.mvvm.addrbook.AddressBook;
import com.untangle.mvvm.addrbook.AddressBookConfiguration;
import com.untangle.mvvm.addrbook.AddressBookSettings;
import com.untangle.mvvm.addrbook.UserEntry;
import com.untangle.mvvm.logging.EventLogger;
import com.untangle.mvvm.logging.EventLoggerFactory;
import com.untangle.mvvm.portal.Application;
import com.untangle.mvvm.portal.Bookmark;
import com.untangle.mvvm.portal.LocalPortalManager;
import com.untangle.mvvm.portal.PortalGlobal;
import com.untangle.mvvm.portal.PortalGroup;
import com.untangle.mvvm.portal.PortalHomeSettings;
import com.untangle.mvvm.portal.PortalLogin;
import com.untangle.mvvm.portal.PortalLoginEvent;
import com.untangle.mvvm.portal.PortalLogoutEvent;
import com.untangle.mvvm.portal.PortalSettings;
import com.untangle.mvvm.portal.PortalUser;
import com.untangle.mvvm.portal.RemoteApplicationManager;
import com.untangle.mvvm.security.LoginFailureReason;
import com.untangle.mvvm.security.LogoutReason;
import com.untangle.mvvm.tran.Transform;
import com.untangle.mvvm.tran.TransformContext;
import com.untangle.mvvm.tran.TransformStats;
import com.untangle.mvvm.util.TransactionWork;
import jcifs.smb.NtlmPasswordAuthentication;
import org.apache.catalina.Realm;
import org.apache.catalina.authenticator.AuthenticatorBase;
import org.apache.catalina.authenticator.FormAuthenticator;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.catalina.realm.RealmBase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

/**
 * Implementation of the PortalManager.
 */
class PortalManagerImpl implements LocalPortalManager
{
    private static final Application[] protoArr = new Application[] { };

    public static final long REAPING_FREQ = 5000;

    private static final int LOGIN_COUNTER = Transform.GENERIC_0_COUNTER;
    private static final int LOGOUT_COUNTER = Transform.GENERIC_4_COUNTER;

    // Add two seconds to each failed login attempt to blunt the force
    // of scripted dictionary attacks.
    private static final long LOGIN_FAIL_SLEEP_TIME = 2000;

    private final MvvmContextImpl mvvmContext;
    private final PortalApplicationManagerImpl appManager;
    private final RemotePortalApplicationManagerImpl remoteAppManager;
    private final PortalRealm portalRealm;
    private final AddressBook addressBook;
    private final LoginReaper reaper;
    private final ThreadLocal<String> localAddr = new ThreadLocal<String>();

    private final Map<String, PortalLoginDesc> activeLogins;

    private final Logger logger = Logger.getLogger(PortalManagerImpl.class);

    private PortalSettings portalSettings;
    private EventLogger portalLogger;

    private PortalTransformStats stats = new PortalTransformStats();

    PortalManagerImpl(MvvmContextImpl mvvmContext)
    {
        this.mvvmContext = mvvmContext;

        activeLogins = new ConcurrentHashMap<String, PortalLoginDesc>();

        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    Query q = s.createQuery("from PortalSettings");
                    portalSettings = (PortalSettings)q.uniqueResult();

                    if (null == portalSettings) {
                        portalSettings = PortalSettings.getBlankSettings();
                        s.save(portalSettings);
                    }
                    return true;
                }
            };
        mvvmContext.runTransaction(tw);

        appManager = PortalApplicationManagerImpl.applicationManager();
        remoteAppManager = new RemotePortalApplicationManagerImpl(appManager);

        portalRealm = new PortalRealm();

        portalLogger = EventLoggerFactory.factory().getEventLogger();

        addressBook = mvvmContext.appAddressBook();

        reaper = new LoginReaper();
        mvvmContext.newThread(reaper).start();

        logger.info("Initialized PortalManager");
    }

    // public methods ---------------------------------------------------------

    public TransformStats getStats()
    {
        return stats;
    }

    public RemoteApplicationManager remoteApplicationManager()
    {
        return remoteAppManager;
    }

    // LocalPortalManager methods ---------------------------------------------

    public PortalApplicationManagerImpl applicationManager()
    {
        return appManager;
    }

    public PortalSettings getPortalSettings()
    {
        return portalSettings;
    }

    public void setPortalSettings(final PortalSettings settings)
    {
        updateIdleTimes(settings);

        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    s.saveOrUpdate(settings);
                    return true;
                }
            };
        mvvmContext.runTransaction(tw);

        this.portalSettings = settings;
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

    public PortalUser getUser(String uid)
    {
        for (PortalUser user : (List<PortalUser>)portalSettings.getUsers()) {
            if (uid.equals(user.getUid())) {
                return user;
            }
        }
        return null;
    }

    public List<Bookmark> getAllBookmarks(PortalUser user)
    {
        List<Bookmark> result = new ArrayList<Bookmark>();

        List<Bookmark> userBookmarks = user.getBookmarks();
        if (userBookmarks != null)
            result.addAll(userBookmarks);

        PortalGroup userGroup = user.getPortalGroup();
        if (userGroup != null) {
            List<Bookmark> groupBookmarks = userGroup.getBookmarks();
            if (groupBookmarks != null)
                result.addAll(groupBookmarks);
        }

        List<Bookmark> globalBookmarks = portalSettings.getGlobal().getBookmarks();
        if (globalBookmarks != null)
            result.addAll(globalBookmarks);

        return result;
    }

    public Bookmark addUserBookmark(final PortalUser user, String name,
                                    Application application, String target)
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
        mvvmContext.runTransaction(tw);

        return result;
    }

    public Bookmark editUserBookmark(final PortalUser user, Long id,
                                     String name, Application application,
                                     String target)
    {
        Bookmark result = user.editBookmark(id, name, application, target);

        if (null != result) {
            TransactionWork tw = new TransactionWork()
                {
                    public boolean doWork(Session s)
                    {
                        s.saveOrUpdate(user);
                        return true;
                    }
                };
            mvvmContext.runTransaction(tw);
        }

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
        mvvmContext.runTransaction(tw);
    }

    public void removeUserBookmarks(final PortalUser user, Set<Long> ids)
    {
        user.removeBookmarks(ids);

        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    s.saveOrUpdate(user);
                    return true;
                }
            };
        mvvmContext.runTransaction(tw);
    }

    public void logout(PortalLogin login)
    {
        doLogout(login, LogoutReason.USER);
    }

    public void forceLogout(PortalLogin login)
    {
        doLogout(login, LogoutReason.ADMINISTRATOR);
    }

    public List<PortalLogin> getActiveLogins()
    {
        List<PortalLogin> l = new ArrayList<PortalLogin>();

        for (PortalLoginDesc pld : activeLogins.values()) {
            l.add(pld.getPortalLogin());
        }

        return l;
    }

    public EventLogger getEventLogger(TransformContext tctx)
    {
        return portalLogger;
    }

    // package protected methods ----------------------------------------------

    AuthenticatorBase newPortalAuthenticator()
    {
        return new PortalAuthenticator();
    }

    boolean isLive(Principal p)
    {
        PortalLoginDesc pld = activeLogins.get(p.getName());
        if (pld == null)
            return false;
        return pld.isLive();
    }

    Realm getPortalRealm()
    {
        return portalRealm;
    }

    void destroy() {
        reaper.destroy();
    }

    // private methods --------------------------------------------------------

    private void updateIdleTimes(PortalSettings settings)
    {
        for (PortalUser pu : (List<PortalUser>)settings.getUsers()) {
            String uid = pu.getUid();
            PortalLoginDesc pld = activeLogins.get(uid);
            if (null != pld) {
                PortalHomeSettings phs = getPortalHomeSettings(pu);
                long it;
                if (null == phs) {
                    logger.warn("null PortalHomeSettings: " + pu);
                    it = Long.MAX_VALUE;
                } else {
                    it = phs.getIdleTimeout();
                }
                pld.setIdleTimeout(it);
            }
        }
    }

    private String getCurrentDomain()
    {
        AddressBookSettings s = addressBook.getAddressBookSettings();
        if (AddressBookConfiguration.AD_AND_LOCAL == s.getAddressBookConfiguration()) {
            return s.getADRepositorySettings().getDomain();
        } else {
            return null;
        }
    }

    public void incrementStatCounter(int num)
    {
        stats.incrementCount(num);
    }

    private PortalLogin login(String uid, String password, InetAddress addr)
    {
        try {
            boolean authenticated = addressBook.authenticate(uid, password);

            if (!authenticated) {
                UserEntry ue = addressBook.getEntry(uid);
                if (null == ue) {
                    logger.debug("no user found with login: " + uid);
                    PortalLoginEvent event = new PortalLoginEvent(addr, uid, false, LoginFailureReason.UNKNOWN_USER);
                    portalLogger.log(event);
                } else {
                    logger.debug("password check failed");
                    PortalLoginEvent event = new PortalLoginEvent(addr, uid, false, LoginFailureReason.BAD_PASSWORD);
                    portalLogger.log(event);
                }

                try {
                    Thread.sleep(LOGIN_FAIL_SLEEP_TIME);
                } catch (InterruptedException exn) { }

                return null;
            }
        } catch (ServiceUnavailableException x) {
            /* This occurs if the user puts in invalid authentication information for the
             * active directory server */
            logger.warn("Unable to authenticate user", x);
            return null;
        }

        PortalUser user = getUser(uid);

        if (null == user) {
            if (portalSettings.getGlobal().isAutoCreateUsers()) {
                logger.debug(uid + " didn't exist, auto creating");
                PortalUser newUser = portalSettings.addUser(uid);
                setPortalSettings(portalSettings);
                user = newUser;
            } else {
                logger.debug("no user found with login: " + uid);
                PortalLoginEvent event = new PortalLoginEvent(addr, uid, false, LoginFailureReason.UNKNOWN_USER);
                portalLogger.log(event);

                try {
                    Thread.sleep(LOGIN_FAIL_SLEEP_TIME);
                } catch (InterruptedException exn) { }

                return null;
            }
        }

        if (!user.isLive()) {
            logger.debug("user is disabled");
            PortalLoginEvent event = new PortalLoginEvent(addr, uid, false, LoginFailureReason.DISABLED);
            portalLogger.log(event);
            return null;
        }

        NtlmPasswordAuthentication pwa = null;
        String domain = getCurrentDomain();
        if (null != domain) {
            pwa = new NtlmPasswordAuthentication(domain, uid, password);

        }
        PortalLogin pl = new PortalLogin(user, addr, pwa);
        PortalHomeSettings phs = getPortalHomeSettings(user);
        if (null == phs) {
            logger.warn("null PortalHomeSettings: " + user);
        }
        long it = null == phs ? Long.MAX_VALUE : phs.getIdleTimeout();
        PortalLoginDesc pld = new PortalLoginDesc(pl, it);
        activeLogins.put(uid, pld);

        stats.incSessions();
        PortalLoginEvent event = new PortalLoginEvent(addr, uid, true);
        portalLogger.log(event);

        return pl;
    }

    private void timeoutLogout(PortalLogin login)
    {
        doLogout(login, LogoutReason.TIMEOUT);
    }

    private void doLogout(PortalLogin login, LogoutReason reason)
    {
        if (LogoutReason.ADMINISTRATOR == reason) {
            logger.warn("Administrative logout of " + login);
        } else {
            logger.info("" + reason + " logout of " + login);
        }

        activeLogins.remove(login.getUser());

        stats.decSessions();
        incrementStatCounter(LOGOUT_COUNTER);
        PortalLogoutEvent evt = new PortalLogoutEvent(login.getClientAddr(),
                                                      login.getUser(), reason);
        portalLogger.log(evt);
    }

    // private classes --------------------------------------------------------

    private class PortalTransformStats extends TransformStats
    {
        // Everything is empty/ignored/zero EXCEPT:
        // tcpSessionTotal -- how many user sessions ever since mvvm boot
        void incSessions() {
            tcpSessionCount++;
            tcpSessionTotal++;
        }

        void decSessions() {
            tcpSessionCount--;
        }

        void incSessionRequests() {
            tcpSessionRequestTotal++;
        }

        // tcpSessionCount -- how many user sessions right now
        // tcpRequestTotal -- how many login attempts ever
    }

    private static class PortalLoginDesc
    {
        private final Logger logger = Logger.getLogger(PortalLoginDesc.class);

        private volatile PortalLogin portalLogin;

        private volatile long idleTimeout;
        private volatile long lastActivity;

        PortalLoginDesc(PortalLogin portalLogin, long idleTimeout)
        {
            this.portalLogin = portalLogin;
            this.idleTimeout = idleTimeout;
            this.lastActivity = System.currentTimeMillis();
        }

        PortalLogin getPortalLogin()
        {
            return portalLogin;
        }

        String getUser()
        {
            return portalLogin.getUser();
        }

        void setIdleTimeout(long idleTimeout)
        {
            this.idleTimeout = idleTimeout;
        }

        void activity()
        {
            lastActivity = System.currentTimeMillis();
        }

        boolean isLive()
        {
            long ct = System.currentTimeMillis();
            return (ct - lastActivity < idleTimeout);
        }
    }

    private class PortalAuthenticator extends FormAuthenticator
    {
        protected static final String info =
            "com.untangle.mvvm.engine.PortalAuthenticator/4.0";

        private final Log log = LogFactory.getLog(getClass());

        PortalAuthenticator() {
            super();
            //            setCache(false);
        }

        // Realm methods ------------------------------------------------------

        @Override
        public String getInfo()
        {
            return info;
        }

        public boolean authenticate(Request request, Response response,
                                    LoginConfig config)
            throws IOException
        {
            Principal p = request.getUserPrincipal();

            if (null != p) {
                if (log.isDebugEnabled())
                    log.debug("Principal: " + p);
                PortalLoginDesc pld = activeLogins.get(p.getName());
                if (pld != null) {
                    // Here's where we update our idle time.
                    pld.activity();
                } else {
                    // Happens once when timeout occurs
                    if (log.isDebugEnabled())
                        log.debug("No active login for " + p);
                }
            } else {
                log.debug("No principal, calling super");
            }

            // org.apache.catalina.Session session = request.getSessionInternal(false);
            // if (session != null) {
            //     p = session.getPrincipal();
            // }
            localAddr.set(request.getRemoteAddr());
            return super.authenticate(request, response, config);
        }
    }

    private class PortalRealm extends RealmBase
    {
        // Realm methods ------------------------------------------------------

        public Principal authenticate(String username, String credentials)
        {
            Principal result = null;
            String password = credentials;
            InetAddress addr;

            String addrStr = localAddr.get();
            if (null == addrStr) {
                result = null;
            } else {
                try {
                    addr = InetAddress.getByName(addrStr);
                    stats.incSessionRequests();
                    incrementStatCounter(LOGIN_COUNTER);
                    result = login(username, password, addr);
                } catch (UnknownHostException exn) {
                    result = null;
                }
            }
            if (result == null)
                incrementStatCounter(LOGOUT_COUNTER);

            return result;

        }

        @Override
        public boolean hasRole(Principal p, String role)
        {
            return null != role && role.equalsIgnoreCase("user")
                && p instanceof PortalLogin;
        }

        // RealmBase methods --------------------------------------------------

        @Override
        protected String getPassword(String username)
        {
            return null;
        }

        @Override
        protected Principal getPrincipal(String username)
        {
            return null;
        }

        @Override
        protected String getName()
        {
            return "PortalRealm";
        }
    }

    private class LoginReaper implements Runnable
    {
        private volatile Thread thread;

        public void run()
        {
            Thread currentThread = thread = Thread.currentThread();
            while (currentThread == thread) {
                for (PortalLoginDesc pld : activeLogins.values()) {
                    if (!pld.isLive()) {
                        PortalLogin login = pld.getPortalLogin();
                        logger.info("Reaping login of " + login);
                        timeoutLogout(login);
                    }
                }

                try {
                    Thread.sleep(REAPING_FREQ);
                } catch (InterruptedException exn) { /* eval loop cond */ }
            }
        }

        void destroy()
        {
            Thread t = thread;
            thread = null;
            t.interrupt();
        }
    }
}
