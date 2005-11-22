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

import java.net.InetAddress;

import com.metavize.mvvm.logging.LogEvent;
import com.metavize.mvvm.logging.SyslogBuilder;
import com.metavize.mvvm.logging.SyslogPriority;
import com.metavize.mvvm.security.LoginFailureReason;

/**
 * Log event for a login/login-attempt.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 * @hibernate.class
 * table="MVVM_LOGIN_EVT"
 * mutable="false"
 */
public class LoginEvent extends LogEvent
{
    private InetAddress clientAddr;
    private String login;
    private boolean local;
    private boolean succeeded;
    private LoginFailureReason reason;

    // constructors -----------------------------------------------------------

    public LoginEvent() { }

    // For successes
    public LoginEvent(InetAddress clientAddr, String login, boolean local, boolean succeeded)
    {
        this(clientAddr, login, local, succeeded, null);
    }

    // For failures
    public LoginEvent(InetAddress clientAddr, String login, boolean local, boolean succeeded, LoginFailureReason reason)
    {
        if (login.length() > DEFAULT_STRING_SIZE) login = login.substring(0, DEFAULT_STRING_SIZE);
        this.clientAddr = clientAddr;
        this.login = login;
        this.local = local;
        this.succeeded = succeeded;
        this.reason = reason;
    }

    // accessors --------------------------------------------------------------

    /**
     * Client address
     *
     * @return the address of the client
     * @hibernate.property
     * type="com.metavize.mvvm.type.InetAddressUserType"
     * @hibernate.column
     * name="CLIENT_ADDR"
     * sql-type="inet"
     */
    public InetAddress getClientAddr()
    {
        return clientAddr;
    }

    public void setClientAddr(InetAddress clientAddr)
    {
        this.clientAddr = clientAddr;
    }

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
        if (login.length() > DEFAULT_STRING_SIZE) login = login.substring(0, DEFAULT_STRING_SIZE);
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

    // Syslog methods ---------------------------------------------------------

    public void appendSyslog(SyslogBuilder sb)
    {
        sb.startSection("info");
        sb.addField("client-addr", clientAddr);
        sb.addField("login", login);
        sb.addField("local", local);
        sb.addField("succeeded", succeeded);
        sb.addField("reason", reason.toString());
    }

    public SyslogPriority getSyslogPrioritiy()
    {
        if (!succeeded) {
            return SyslogPriority.NOTICE;
        } else {
            // local logins are not as interesting to enduser
            return local ? SyslogPriority.DEBUG : SyslogPriority.INFORMATIONAL;
        }
    }

    // Object methods ---------------------------------------------------------

    public String toString()
    {
        return "LoginEvent id: " + getId() + " login: " + login + " succeeded: " + succeeded;
    }
}
