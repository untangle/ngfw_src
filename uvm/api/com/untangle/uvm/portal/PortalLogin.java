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

package com.untangle.uvm.portal;

import java.io.Serializable;
import java.net.InetAddress;
import java.security.Principal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.untangle.uvm.node.IPaddr;
// Can't include this since we live on UI too
// import jcifs.smb.NtlmPasswordAuthentication;
import org.apache.log4j.Logger;

/**
 * Portal login. Represents a logged in user.
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 */
public class PortalLogin implements Principal, Serializable
{
    private static final long serialVersionUID = -3861141760496839437L;

    private final String uid;
    private final String group;
    private final IPaddr clientAddr;
    private final Date loginDate;

    // Not for UI use:
    private transient Map ntlmAuths = new HashMap();

    // constructors -----------------------------------------------------------

    // With domain
    public PortalLogin(PortalUser user, InetAddress clientAddr,
                       Object ntlmAuth)
    {
        this.uid = user.getUid();
        PortalHomeSettings phs = user.getPortalHomeSettings();
        PortalGroup g = user.getPortalGroup();
        if (g != null) {
            this.group = g.getName();
        } else {
            this.group = null;
        }
        this.clientAddr = new IPaddr(clientAddr);
        this.loginDate = new Date(System.currentTimeMillis());
        if (ntlmAuth != null) {
            this.ntlmAuths.put(null, ntlmAuth);
            this.ntlmAuths.put(ntlmAuth.toString(), ntlmAuth);
        }
    }

    // Principal methods ------------------------------------------------------

    public String getName()
    {
        return uid;
    }

    public String toString()
    {
        return "PortalLogin of: " + uid + " on: " + loginDate
            + " from: " + clientAddr;
    }

    public boolean equals(Object o)
    {
        if (!(o instanceof PortalLogin )) {
            return false;
        } else {
            PortalLogin pl = (PortalLogin)o;
            // idle time and group aren't important.
            return uid.equals(pl.uid) && clientAddr.equals(pl.clientAddr)
                && loginDate.equals(pl.loginDate);
        }
    }

    public int hashCode()
    {
        if (null == uid || null == clientAddr || null == loginDate) {
            Logger.getLogger(getClass()).warn("null in PortalLogin: " + this);
            return 0;
        } else {
            int result = 17;
            result = 37 * result + uid.hashCode();
            result = 37 * result + clientAddr.hashCode();
            result = 37 * result + loginDate.hashCode();

            return result;
        }
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

    public void addNtlmAuth(Object auth)
    {
        this.ntlmAuths.put(auth.toString(), auth);
    }

    public Object getNtlmAuth(String name)
    {
        return ntlmAuths.get(name);
    }

    public Object getNtlmAuth()
    {
        return ntlmAuths.get(null);
    }
}
