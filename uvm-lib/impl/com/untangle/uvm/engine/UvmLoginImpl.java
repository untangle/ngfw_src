/*
 * $HeadURL$
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

import java.net.InetAddress;
import java.util.Iterator;
import java.util.Set;
import javax.security.auth.login.FailedLoginException;

import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.client.MultipleLoginsException;
import com.untangle.uvm.client.RemoteUvmContext;
import com.untangle.uvm.logging.EventLogger;
import com.untangle.uvm.security.LoginFailureReason;
import com.untangle.uvm.security.LoginSession;
import com.untangle.uvm.security.PasswordUtil;
import com.untangle.uvm.security.RegistrationInfo;
import com.untangle.uvm.security.User;
import com.untangle.uvm.security.UvmLogin;
import com.untangle.uvm.security.UvmPrincipal;
import org.apache.log4j.Logger;

/**
 * Implements UvmLogin.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
class UvmLoginImpl implements UvmLogin
{
    private static final String ACTIVATION_USER    = "admin";

    private static final String SYSTEM_USER     = "localadmin";
    private static final String SYSTEM_PASSWORD = "nimda11lacol";
    // Add two seconds to each failed login attempt to blunt the force
    // of scripted dictionary attacks.
    private static final long LOGIN_FAIL_SLEEP_TIME = 2000;

    private static final UvmLoginImpl UVM_LOGIN = new UvmLoginImpl();

    private final Logger logger = Logger.getLogger(UvmLoginImpl.class);
    private final EventLogger eventLogger = LocalUvmContextFactory.context()
        .eventLogger();

    private int loginId = 0;

    // Constructors -----------------------------------------------------------

    private UvmLoginImpl() { }

    // factories --------------------------------------------------------------

    static UvmLoginImpl uvmLogin()
    {
        return UVM_LOGIN;
    }

    // Activation methods ------------------------------------------------------
    public boolean isActivated()
    {
        return LocalUvmContextFactory.context().isActivated();
    }

    public boolean isRegistered()
    {
        return LocalUvmContextFactory.context().isRegistered();
    }

    // Key may be null.
    public RemoteUvmContext activationLogin(String key, RegistrationInfo regInfo)
        throws FailedLoginException
    {
	// This is the new way.
        if (isRegistered())
            throw new FailedLoginException("Product has already been activated");

        boolean success = LocalUvmContextFactory.context().activate(key, regInfo);
        if (!success)
            throw new FailedLoginException("Activation key invalid");

        HttpInvokerImpl invoker = HttpInvokerImpl.invoker();
        InetAddress clientAddr = invoker.getClientAddr();
        return login(ACTIVATION_USER, true, clientAddr,
                     LoginSession.LoginType.INTERACTIVE, true);
    }

    // UvmLogin methods ------------------------------------------------------

    public RemoteUvmContext interactiveLogin(final String login,
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

        Set users = LocalUvmContextFactory.context().adminManager()
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

    public RemoteUvmContext systemLogin(String username, String password)
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

    private RemoteUvmContext login(String username, boolean readOnly,
                                    InetAddress clientAddr,
                                    LoginSession.LoginType loginType,
                                    boolean force)
    {
        HttpInvokerImpl invoker = HttpInvokerImpl.invoker();

        UvmPrincipal mp = new UvmPrincipal(username, readOnly);

        LoginSession loginSession = new LoginSession(mp, nextLoginId(),
                                                     clientAddr, loginType);
        invoker.login(loginSession, force);

        return UvmContextImpl.getInstance().remoteContext();
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
