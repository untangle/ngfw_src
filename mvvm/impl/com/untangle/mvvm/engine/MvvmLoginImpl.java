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

import java.net.InetAddress;
import java.util.Iterator;
import java.util.Set;
import javax.security.auth.login.FailedLoginException;

import com.untangle.mvvm.MvvmContextFactory;
import com.untangle.mvvm.client.MultipleLoginsException;
import com.untangle.mvvm.client.MvvmRemoteContext;
import com.untangle.mvvm.logging.EventLogger;
import com.untangle.mvvm.security.LoginFailureReason;
import com.untangle.mvvm.security.LoginSession;
import com.untangle.mvvm.security.MvvmLogin;
import com.untangle.mvvm.security.MvvmPrincipal;
import com.untangle.mvvm.security.PasswordUtil;
import com.untangle.mvvm.security.User;
import org.apache.log4j.Logger;

class MvvmLoginImpl implements MvvmLogin
{
    private static final String ACTIVATION_USER    = "admin";

    private static final String SYSTEM_USER     = "localadmin";
    private static final String SYSTEM_PASSWORD = "nimda11lacol";
    // Add two seconds to each failed login attempt to blunt the force
    // of scripted dictionary attacks.
    private static final long LOGIN_FAIL_SLEEP_TIME = 2000;

    private static final MvvmLoginImpl MVVM_LOGIN = new MvvmLoginImpl();

    private final Logger logger = Logger.getLogger(MvvmLoginImpl.class);
    private final EventLogger eventLogger = MvvmContextFactory.context()
        .eventLogger();

    private int loginId = 0;

    // Constructors -----------------------------------------------------------

    private MvvmLoginImpl() { }

    // factories --------------------------------------------------------------

    static MvvmLoginImpl mvvmLogin()
    {
        return MVVM_LOGIN;
    }

    // Activation methods ------------------------------------------------------
    public boolean isActivated()
    {
        return MvvmContextFactory.context().isActivated();
    }

    public MvvmRemoteContext activationLogin(String key)
        throws FailedLoginException
    {
        if (isActivated())
            throw new FailedLoginException("Product has already been activated");

        boolean success = MvvmContextFactory.context().activate(key);
        if (!success)
            throw new FailedLoginException("Activation key invalid");

        HttpInvokerImpl invoker = HttpInvokerImpl.invoker();
        InetAddress clientAddr = invoker.getClientAddr();
        return login(ACTIVATION_USER, true, clientAddr,
                     LoginSession.LoginType.INTERACTIVE, true);
    }

    // MvvmLogin methods ------------------------------------------------------

    public MvvmRemoteContext interactiveLogin(final String login,
                                              String password,
                                              boolean force)
        throws FailedLoginException, MultipleLoginsException
    {
        if (!isActivated()) {
            logger.error("Attempt to login as " + login + " without activating first");
            throw new FailedLoginException("Product has not been activated");
        }

        HttpInvokerImpl invoker = HttpInvokerImpl.invoker();
        InetAddress clientAddr = invoker.getClientAddr();

        Set users = MvvmContextFactory.context().adminManager()
            .getAdminSettings().getUsers();
        User user = null;
        for (Iterator iter = users.iterator(); iter.hasNext(); ) {
            user = (User)iter.next();
            if (login.equals(user.getLogin()))
                break;
            user = null;
        }

        if (null == user) {
            logger.debug("no user found with login: " + login);
            eventLogger.log(new LoginEvent(clientAddr, login, false, false,
                                           LoginFailureReason.UNKNOWN_USER));
            try {
                Thread.sleep(LOGIN_FAIL_SLEEP_TIME);
            } catch (InterruptedException exn) { }

            throw new FailedLoginException("no such user: " + login);
        } else if (!PasswordUtil.check(password, user.getPassword())) {
            logger.debug("password check failed");
            eventLogger.log(new LoginEvent(clientAddr, login, false, false,
                                           LoginFailureReason.BAD_PASSWORD));

            try {
                Thread.sleep(LOGIN_FAIL_SLEEP_TIME);
            } catch (InterruptedException exn) { }

            throw new FailedLoginException("incorrect password");
        } else {
            logger.debug("password check succeeded");
            eventLogger.log(new LoginEvent(clientAddr, login, false, true));

            return login(login, user.isReadOnly(), clientAddr,
                         LoginSession.LoginType.INTERACTIVE, force);
        }

    }

    public MvvmRemoteContext systemLogin(String username, String password)
        throws FailedLoginException
    {
        HttpInvokerImpl invoker = HttpInvokerImpl.invoker();
        InetAddress clientAddr = invoker.getClientAddr();

        // Do something better here. XXX
        logger.debug("attempting local login");
        if (!clientAddr.isLoopbackAddress()) {
            logger.debug("systemLogin failed, not localhost");

            eventLogger.log(new LoginEvent(clientAddr, username, true,
                                           false));

            try {
                Thread.sleep(LOGIN_FAIL_SLEEP_TIME);
            } catch (InterruptedException exn) { }

            throw new FailedLoginException("system login not from localhost");
        } else if (!isSystemLogin(username, password)) {
            eventLogger.log(new LoginEvent(clientAddr, username, true,
                                           false));
            try {
                Thread.sleep(LOGIN_FAIL_SLEEP_TIME);
            } catch (InterruptedException exn) { }

            throw new FailedLoginException("bad system login");
        } else {
            logger.debug("local login succeeded");
            eventLogger.log(new LoginEvent(clientAddr, username, true, true));

            return login(username, false, clientAddr,
                         LoginSession.LoginType.SYSTEM, false);
        }
    }

    private MvvmRemoteContext login(String username, boolean readOnly,
                                    InetAddress clientAddr,
                                    LoginSession.LoginType loginType,
                                    boolean force)
    {
        HttpInvokerImpl invoker = HttpInvokerImpl.invoker();

        MvvmPrincipal mp = new MvvmPrincipal(username, readOnly);

        LoginSession loginSession = new LoginSession(mp, nextLoginId(),
                                                     clientAddr, loginType);
        invoker.login(loginSession, force);

        return MvvmContextImpl.getInstance().remoteContext();
    }

    // private methods --------------------------------------------------------

    private boolean isSystemLogin(String username, String passwd)
    {
        return username.equals(SYSTEM_USER) && passwd.equals(SYSTEM_PASSWORD);
    }

    private int nextLoginId()
    {
        synchronized (this) {
            return ++loginId;
        }
    }
}
