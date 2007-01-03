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
import java.sql.Timestamp;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.untangle.mvvm.logging.SyslogBuilder;
import com.untangle.mvvm.logging.SyslogPriority;
import com.untangle.mvvm.security.LogoutReason;
import com.untangle.mvvm.tran.IPaddr;
import javax.persistence.Entity;
import org.hibernate.annotations.Type;

/**
 * Log event for a portal logout.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
@Table(name="portal_logout_evt", schema="events")
public class PortalLogoutEvent extends PortalEvent implements Serializable
{
    private static final long serialVersionUID = 3003003630980879147L;

    private IPaddr clientAddr;
    private String uid;
    private LogoutReason reason;

    // constructors -----------------------------------------------------------

    public PortalLogoutEvent() { }

    public PortalLogoutEvent(IPaddr clientAddr, String uid, LogoutReason reason)
    {
        this.clientAddr = clientAddr;
        this.uid = uid;
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
     * Logout used to logout.  May be  used to join to PORTAL_USER.
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
     * Reason for logout .
     *
     * @return the reason.
     */
    @Type(type="com.untangle.mvvm.security.LogoutReasonUserType")
    public LogoutReason getReason()
    {
        return reason;
    }

    public void setReason(LogoutReason reason)
    {
        this.reason = reason;
    }

    // Syslog methods ----------------------------------------------------------

    public void appendSyslog(SyslogBuilder sb)
    {
        sb.startSection("info");
        sb.addField("client-addr", clientAddr);
        sb.addField("uid", uid);
        sb.addField("reason", null == reason ? "none" : reason.toString());
    }

    @Transient
    public String getSyslogId()
    {
        return "PortalLogout";
    }

    @Transient
    public SyslogPriority getSyslogPriority()
    {
        if (reason == LogoutReason.ADMINISTRATOR) {
            return SyslogPriority.WARNING; // logout by admin
        } else {
            return SyslogPriority.INFORMATIONAL;
        }
    }

    // Object methods ----------------------------------------------------------

    public String toString()
    {
        return "PortalLogoutEvent id: " + getId() + " uid: " + uid
            + " reason: " + reason;
    }
}
