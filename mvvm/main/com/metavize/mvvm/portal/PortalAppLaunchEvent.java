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
import com.metavize.mvvm.tran.IPaddr;

/**
 * Log event for an application launch
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_PORTAL_APP_LAUNCH_EVT"
 * mutable="false"
 */
public class PortalAppLaunchEvent extends LogEvent implements Serializable
{
    private static final long serialVersionUID = 1003003630088749917L;
    
    private IPaddr clientAddr;
    private String uid;
    private boolean succeeded;
    private AppLaunchFailureReason reason;

    // constructors -----------------------------------------------------------

    public PortalAppLaunchEvent() { }

    // For successes
    public PortalAppLaunchEvent(IPaddr clientAddr, String uid, boolean succeeded)
    {
        this(clientAddr, uid, succeeded, null);
    }

    // For failures
    public PortalAppLaunchEvent(IPaddr clientAddr, String uid, boolean succeeded, AppLaunchFailureReason reason)
    {
        this.clientAddr = clientAddr;
        this.uid = uid;
        this.succeeded = succeeded;
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
     * AppLaunch used to appLaunch.  May be  used to join to PORTAL_USER.
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
     * Whether or not the appLaunch succeeeded, if not there will be a reason.
     *
     * @return whether or not the appLaunch was successful
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
     * Reason for appLaunch failure.
     *
     * @return the reason.
     * @hibernate.property
     * column="REASON"
     * type="com.metavize.mvvm.security.AppLaunchFailureReasonUserType"
     */
    public AppLaunchFailureReason getReason()
    {
        return reason;
    }

    public void setReason(AppLaunchFailureReason reason)
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

    public String getSyslogId()
    {
        return "PortalAppLaunch";
    }

    public SyslogPriority getSyslogPriority()
    {
        if (false == succeeded) {
            return SyslogPriority.WARNING; // appLaunch attempt failed
        } else {
            return SyslogPriority.INFORMATIONAL;
        }
    }

    // Object methods ---------------------------------------------------------

    public String toString()
    {
        return "PortalAppLaunchEvent id: " + getId() + " uid: " + uid + " succeeded: " + succeeded;
    }
}
