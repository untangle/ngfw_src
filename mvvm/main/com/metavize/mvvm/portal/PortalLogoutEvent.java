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

package com.metavize.mvvm.portal;

import java.net.InetAddress;
import java.io.Serializable;
import java.util.Date;
import java.sql.Timestamp;

import com.metavize.mvvm.logging.LogEvent;
import com.metavize.mvvm.logging.SyslogBuilder;
import com.metavize.mvvm.logging.SyslogPriority;
import com.metavize.mvvm.security.LogoutReason;
import com.metavize.mvvm.tran.IPaddr;

/**
 * Log event for a portal logout.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 * @hibernate.class
 * table="PORTAL_LOGOUT_EVT"
 * mutable="false"
 */
public class PortalLogoutEvent extends LogEvent implements Serializable
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
     * @hibernate.property
     * type="com.metavize.mvvm.type.IPaddrUserType"
     * @hibernate.column
     * name="CLIENT_ADDR"
     * sql-type="inet"
     */
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
     * @hibernate.property
     * column="UID"
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
     * @hibernate.property
     * column="REASON"
     * type="com.metavize.mvvm.security.LogoutReasonUserType"
     */
    public LogoutReason getReason()
    {
        return reason;
    }

    public void setReason(LogoutReason reason)
    {
        this.reason = reason;
    }

    // Syslog methods ---------------------------------------------------------

    public void appendSyslog(SyslogBuilder sb)
    {
        sb.startSection("info");
        sb.addField("client-addr", clientAddr);
        sb.addField("uid", uid);
        sb.addField("reason", null == reason ? "none" : reason.toString());
    }

    public String getSyslogId()
    {
        return "PortalLogout";
    }

    public SyslogPriority getSyslogPriority()
    {
        if (reason == LogoutReason.ADMINISTRATOR) {
            return SyslogPriority.WARNING; // logout by admin
        } else {
            return SyslogPriority.INFORMATIONAL;
        }
    }

    // Object methods ---------------------------------------------------------

    public String toString()
    {
        return "PortalLogoutEvent id: " + getId() + " uid: " + uid + " reaso: " + reason;
    }
}
