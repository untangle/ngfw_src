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

import com.untangle.mvvm.logging.LogEvent;
import com.untangle.mvvm.logging.SyslogBuilder;
import com.untangle.mvvm.logging.SyslogPriority;
import com.untangle.mvvm.tran.IPaddr;
import javax.persistence.Entity;
import org.hibernate.annotations.Type;

/**
 * Log event for an application launch
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
@Table(name="portal_app_launch_evt", schema="events")
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
    public PortalAppLaunchEvent(IPaddr clientAddr, String uid,
                                boolean succeeded, Application app,
                                String destination)
    {
        this(clientAddr, uid, succeeded, null, app, destination);
    }

    // For failures
    public PortalAppLaunchEvent(IPaddr clientAddr, String uid,
                                boolean succeeded,
                                AppLaunchFailureReason reason,
                                Application app, String destination)
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
     * uid used to appLaunch.  May be  used to join to PORTAL_USER.
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
     * Whether or not the appLaunch succeeeded, if not there will be a
     * reason.
     *
     * @return whether or not the appLaunch was successful
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
     * Reason for appLaunch failure.
     *
     * @return the reason.
     */
    @Type(type="com.untangle.mvvm.portal.AppLaunchFailureReasonUserType")
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
     */
    @Column(name="app_name")
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
     * @return a <code>String</code> giving the destination of the
     * app, if any, otherwise null.
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

    @Transient
    public String getSyslogId()
    {
        return "PortalAppLaunch";
    }

    @Transient
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
        return "PortalAppLaunchEvent id: " + getId() + " uid: "
            + uid + " succeeded: " + succeeded + " app: " + appName;
    }
}
