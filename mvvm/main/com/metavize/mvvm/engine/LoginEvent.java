/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: HttpBlockerEvent.java 194 2005-04-06 19:13:55Z rbscott $
 */

package com.metavize.mvvm.engine;

import com.metavize.mvvm.security.LoginFailureReason;
import com.metavize.mvvm.logging.LogEvent;

/**
 * Log event for a login/login-attempt.
 *
 * @author <a href="mailto:amread@nyx.net">Aaron Read</a>
 * @version 1.0
 * @hibernate.class
 * table="MVVM_LOGIN_EVT"
 * mutable="false"
 */
public class LoginEvent extends LogEvent
{
    private String login;
    private boolean local;
    private boolean succeeded;
    private LoginFailureReason reason;

    // constructors -----------------------------------------------------------

    public LoginEvent() { }

    // For successes
    public LoginEvent(String login, boolean local, boolean succeeded)
    {
        this.login = login;
        this.local = local;
        this.succeeded = succeeded;
        this.reason = null;
    }

    // For non-local failures
    public LoginEvent(String login, boolean local, boolean succeeded, LoginFailureReason reason)
    {
        this.login = login;
        this.local = local;
        this.succeeded = succeeded;
        this.reason = reason;
    }

    // accessors --------------------------------------------------------------

    /**
     * Login used to login.  May be  used to join to MVVM_USER.
     *
     * @return a <code>String</code> giving the login for the user
     * @hibernate.property
     * column="LOGIN"
     */
    public String getLogin()
    {
        return login;
    }

    public void setLogin(String login)
    {
        this.login = login;
    }

    /**
     * Whether or not the login is local (from internal tools on system, ignored for reporting).
     *
     * @return whether or not the login was local
     * @hibernate.property
     * column="LOCAL"
     */
    public boolean isLocal()
    {
        return local;
    }

    public void setLocal(boolean local)
    {
        this.local = local;
    }

    /**
     * Whether or not the login succeeeded, if not there will be a reason.
     *
     * @return whether or not the login was successful
     * @hibernate.property
     * column="SUCCEEDED"
     */
    public boolean isSucceeded()
    {
        return succeeded;
    }

    public void setSucceeded(boolean succeeded)
    {
        this.succeeded = succeeded;
    }

    /**
     * Reason for login failure.
     *
     * @return the reason.
     * @hibernate.property
     * column="REASON"
     * type="com.metavize.mvvm.security.LoginFailureReasonUserType"
     */
    public LoginFailureReason getReason()
    {
        return reason;
    }

    public void setReason(LoginFailureReason reason)
    {
        this.reason = reason;
    }

    // Object methods ---------------------------------------------------------

    public String toString()
    {
        return "LoginEvent id: " + getId() + " login: " + login + " succeeded: " + succeeded;
    }
}
