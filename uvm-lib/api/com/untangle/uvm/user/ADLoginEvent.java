/*
 * $HeadURL: svn://chef/work/src/uvm-lib/impl/com/untangle/uvm/user/ADLoginEvent.java $
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.uvm.user;

import java.net.InetAddress;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.logging.SyslogBuilder;
import com.untangle.uvm.logging.SyslogPriority;

/**
 * Log event for a login/login-attempt.
 *
 * @author <a href="mailto:seb@untangle.com">Sébastien Delafond</a>
 * @version 1.0
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
@Table(name="n_login_evt", schema="events")
@SuppressWarnings("serial")
public class ADLoginEvent extends LogEvent
{

    public static String EVENT_LOGIN = "I";
    public static String EVENT_UPDATE = "U";
    public static String EVENT_LOGOUT = "O";

    private InetAddress clientAddr;
    private String loginName;
    private String domain;
    private String event;

    // constructors --------------------------------------------------------

    public ADLoginEvent() { }

    public ADLoginEvent(InetAddress clientAddr, String loginName, String domain, String event)
    {
        this.clientAddr = clientAddr;
        this.loginName = loginName;
        this.domain = domain;
        this.event = event;
    }

    // accessors -----------------------------------------------------------

    /**
     * Client address
     *
     * @return the address of the client
     */
    @Column(name="client_addr")
    @Type(type="com.untangle.uvm.type.InetAddressUserType")
    public InetAddress getClientAddr()
    {
        return clientAddr;
    }

    public void setClientAddr(InetAddress clientAddr)
    {
        this.clientAddr = clientAddr;
    }

    /**
     * Login used to login
     *
     * @return a <code>String</code> giving the login for the user
     */
    @Column(name="login_name")
    public String getLoginName()
    {
        return loginName;
    }

    public void setLoginName(String loginName)
    {
        this.loginName = loginName;
    }

    /**
     * Get the <code>Domain</code> value.
     *
     * @return a <code>String</code> value
     */
    @Column(name="domain")
    public String getDomain() {
        return domain;
    }

    /**
     * Set the <code>Domain</code> value.
     *
     * @param newDomain The new Domain value.
     */
    public void setDomain(String newDomain) {
        this.domain = newDomain;
    }

    /**
     * Get the <code>Event</code> value.
     *
     * @return a <code>String</code> value
     */
    @Column(name="type")
    public String getEvent() {
        return event;
    }

    /**
     * Set the <code>Event</code> value.
     *
     * @param newEvent The new Event value.
     */
    public void setEvent(String newEvent) {
        this.event = newEvent;
    }

    // Syslog methods ------------------------------------------------------

    public void appendSyslog(SyslogBuilder sb)
    {
        sb.startSection("info");
        sb.addField("client-addr", clientAddr);
        sb.addField("login-name", loginName);
    }

    @Transient
    public String getSyslogId()
    {
        return "AD login";
    }

    @Transient
    public SyslogPriority getSyslogPriority()
    {
        return SyslogPriority.INFORMATIONAL;
    }

    // Object methods ------------------------------------------------------

    public String toString()
    {
        return "ADLoginEvent id: " + getId() + " login-name: " + loginName + " domain: " + domain;
    }
}
