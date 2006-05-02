/*
 * Copyright (c) 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.portal;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.Date;

import com.metavize.mvvm.tran.IPaddr;
import jcifs.smb.NtlmPasswordAuthentication;

/**
 * Portal login.  Contains a PortalUser.
 *
 * @author <a href="mailto:jdi@metavize.com">John Irwin</a>
 */
public class PortalLogin implements Serializable
{
    private static final long serialVersionUID = -3861141760496839437L;

    private String uid;
    private String group;
    private IPaddr clientAddr;
    private Date loginDate;
    private long idleTime;

    // Not for UI use:
    private transient NtlmPasswordAuthentication ntlmAuth;

    // constructors -----------------------------------------------------------
    // Not for user consumption, only used by PortalManagerImpl

    // With domain
    public PortalLogin(PortalUser user, InetAddress clientAddr, NtlmPasswordAuthentication ntlmAuth)
    {
        this.uid = user.getUid();
        PortalGroup g = user.getPortalGroup();
        if (g != null)
            this.group = g.getName();
        else
            this.group = null;
        this.clientAddr = new IPaddr(clientAddr);
        this.loginDate = new Date(System.currentTimeMillis());
        this.idleTime = 0;
        this.ntlmAuth = ntlmAuth;
    }


    // accessors --------------------------------------------------------------

    /**
     * Gets the user associated with this login.
     */
    public String getUser()
    {
        return uid;
    }

    /**
     * Gets the group of the user associated with this login (if any,
     * otherwise null).
     */
    public String getGroup()
    {
        return group;
    }

    public NtlmPasswordAuthentication getNtlmAuth()
    {
        return ntlmAuth;
    }

    /**
     * Client address.
     *
     * @return the address of the client
     */
    public IPaddr getClientAddr()
    {
        return clientAddr;
    }

    /**
     * Date the user logged in.
     *
     * @return the date of login
     */
    public Date getLoginDate()
    {
        return loginDate;
    }

    /**
     * How long the login session has been idle.
     *
     * @return the idle time for the login session, in millis.
     */
    public long getIdleTime()
    {
        return idleTime;
    }

    public void setActive()
    {
        this.idleTime = 0;
    }

    public int hashCode()
    {
        if ( uid == null || clientAddr == null || loginDate == null )
            // shouldn't happen
            return 0;

        return uid.hashCode() * 37 + clientAddr.hashCode() * 7 + loginDate.hashCode();
    }

    public boolean equals( Object o )
    {
        if (!(o instanceof PortalLogin ))
            return false;

        PortalLogin other = (PortalLogin)o;
        if (uid.equals(other.uid) &&
            clientAddr.equals(other.clientAddr) &&
            loginDate.equals(other.loginDate))
            // idle time and group aren't important.
            return true;

        return false;
    }

    public String toString()
    {
        return "PortalLogin of " + uid + " on " + loginDate + " from " + clientAddr;
    }
}
