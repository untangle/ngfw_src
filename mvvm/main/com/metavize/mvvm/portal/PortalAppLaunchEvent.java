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
 * table="PORTAL_APP_LAUNCH_EVT"
 * mutable="false"
 */
public class PortalAppLaunchEvent extends LogEvent implements Serializable
{
    private static final long serialVersionUID = 1003003630088749917L;
    
    private IPaddr clientAddr;
    private String uid;
    private boolean succeeded;
    private AppLaunchFailureReason reason;
    private String appName;
    private String destination;

    // constructors -----------------------------------------------------------

    public PortalAppLaunchEvent() { }

    // For successes
    public PortalAppLaunchEvent(IPaddr clientAddr, String uid, boolean succeeded, Application app, String destination)
    {
        this(clientAddr, uid, succeeded, null, app, destination);
    }

    // For failures
    public PortalAppLaunchEvent(IPaddr clientAddr, String uid, boolean succeeded, AppLaunchFailureReason reason, Application app, String destination)
    {
        this.clientAddr = clientAddr;
        this.uid = uid;
        this.succeeded = succeeded;
        this.reason = reason;
        this.appName = app.getName();
        this.destination = destination;
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
     * uid used to appLaunch.  May be  used to join to PORTAL_USER.
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
     * type="com.metavize.mvvm.portal.AppLaunchFailureReasonUserType"
     */
    public AppLaunchFailureReason getReason()
    {
        return reason;
    }

    public void setReason(AppLaunchFailureReason reason)
    {
        this.reason = reason;
    }

    /**
     * application named launched.
     *
     * @return a <code>String</code> naming the app that was launched
     * @hibernate.property
     * column="app_name"
     */
    public String getAppName()
    {
        return appName;
    }

    public void setAppName(String appName)
    {
        this.appName = appName;
    }

    /**
     * Destination of application.  May be null (for non host services)
     *
     * @return a <code>String</code> giving the destination of the app, if any, otherwise null
     * @hibernate.property
     * column="destination"
     */
    public String getDestination()
    {
        return destination;
    }

    public void setDestination(String destination)
    {
        this.destination = destination;
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
        sb.addField("app-name", appName);
        if (destination != null)
            sb.addField("destination", destination);
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
        return "PortalAppLaunchEvent id: " + getId() + " uid: " + uid + " succeeded: " + succeeded +
            " app: " + appName;
    }
}
