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

package com.untangle.mvvm.portal;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.untangle.mvvm.logging.SyslogBuilder;
import com.untangle.mvvm.logging.SyslogPriority;
import com.untangle.mvvm.security.LoginFailureReason;
import com.untangle.mvvm.tran.IPaddr;
import javax.persistence.Entity;
import org.hibernate.annotations.Type;

/**
 * Log event for a login/login-attempt.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
@Table(name="portal_login_evt", schema="events")
public class PortalLoginEvent extends PortalEvent implements Serializable
{
    private static final long serialVersionUID = 5003003603098874917L;

    private IPaddr clientAddr;
    private String uid;
    private boolean succeeded;
    private LoginFailureReason reason;

    // constructors -----------------------------------------------------------

    public PortalLoginEvent() { }

    // For successes
    public PortalLoginEvent(InetAddress clientAddr, String uid,
                            boolean succeeded)
    {
        this(clientAddr, uid, succeeded, null);
    }

    // For failures
    public PortalLoginEvent(InetAddress clientAddr, String uid,
                            boolean succeeded, LoginFailureReason reason)
    {
        this.clientAddr = new IPaddr(clientAddr);
        this.uid = uid;
        this.succeeded = succeeded;
        this.reason = reason;
    }

    // accessors --------------------------------------------------------------

    /**
     * Client address
     *
     * @return the address of the client
     */
    @Column(name="client_addr")
    @Type(type="com.untangle.mvvm.type.IPaddrUserType")
    public IPaddr getClientAddr()
    {
        return clientAddr;
    }

    public void setClientAddr(IPaddr clientAddr)
    {
        this.clientAddr = clientAddr;
    }

    /**
     * Login used to login.  May be  used to join to PORTAL_USER.
     *
     * @return a <code>String</code> giving the uid for the user
     */
    public String getUid()
    {
        return uid;
    }

    public void setUid(String uid)
    {
        this.uid = uid;
    }

    /**
     * Whether or not the login succeeeded, if not there will be a
     * reason.
     *
     * @return whether or not the login was successful
     */
    @Column(nullable=false)
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
     */
    @Type(type="com.untangle.mvvm.security.LoginFailureReasonUserType")
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
        sb.addField("uid", uid);
        sb.addField("succeeded", succeeded);
        if (reason != null)
            // Don't need a reason for success
            sb.addField("reason", reason.toString());
    }

    @Transient
    public String getSyslogId()
    {
        return "PortalLogin";
    }

    @Transient
    public SyslogPriority getSyslogPriority()
    {
        if (false == succeeded) {
            return SyslogPriority.WARNING; // login attempt failed
        } else {
            return SyslogPriority.INFORMATIONAL;
        }
    }

    // Object methods ---------------------------------------------------------

    public String toString()
    {
        return "PortalLoginEvent id: " + getId() + " uid: " + uid
            + " succeeded: " + succeeded;
    }
}
