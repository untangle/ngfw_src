/*
 * $HeadURL$
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

package com.untangle.node.cpd;

import java.net.InetAddress;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;

import com.untangle.node.cpd.CPDSettings.AuthenticationType;
import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.logging.SyslogBuilder;
import com.untangle.uvm.logging.SyslogPriority;

/**
 * Log event for a login/login-attempt.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
@Table(name="n_cpd_login_evt", schema="events")
public class CPDLoginEvent extends LogEvent
{
    private static final long serialVersionUID = 1716114286650532644L;

    public enum EventType { LOGIN, UPDATE, FAILED, LOGOUT };

    private InetAddress clientAddr;
    private String loginName;
    private String authenticationTypeValue;
    private String eventValue;

    // constructors --------------------------------------------------------

    public CPDLoginEvent() { }

    public CPDLoginEvent(InetAddress clientAddr, String loginName, AuthenticationType type, EventType event)
    {
        this.clientAddr = clientAddr;
        this.loginName = loginName;
        setAuthenticationType(type);
        setEvent(event);
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
     * Get the <code>Event</code> value.
     *
     * @return a <code>String</code> value
     */
    @SuppressWarnings("unused")
    @Column(name="event")
    private String getEventValue() {
        return eventValue;
    }

    /**
     * Set the <code>Event</code> value.
     *
     * @param newEvent The new Event value.
     */
    @SuppressWarnings("unused")
    private void setEventValue(String newEvent) {
        this.eventValue = newEvent;
    }
    

    @Transient
    public EventType getEvent() {
        return EventType.valueOf(this.eventValue);
    }

    public void setEvent(EventType newEvent) {
        this.eventValue = newEvent.toString();
    }
    
    /**
     * Get the <code>Method</code> value.
     *
     * @return a <code>String</code> value
     */
    @SuppressWarnings("unused")
    @Column(name="auth_type")
    private String getAuthenticationTypeValue() {
        return authenticationTypeValue;
    }

    /**
     * Set the <code>Event</code> value.
     *
     * @param newEvent The new Event value.
     */
    @SuppressWarnings("unused")
    private void setAuthenticationTypeValue(String newValue) {
        this.authenticationTypeValue = newValue;
    }
    

    @Transient
    public AuthenticationType getAuthenticationType() {
        return AuthenticationType.valueOf(this.authenticationTypeValue);
    }

    public void setAuthenticationType(AuthenticationType newValue) {
        this.authenticationTypeValue = newValue.toString();
    }
    
    // Syslog methods ------------------------------------------------------

    public void appendSyslog(SyslogBuilder sb)
    {
        sb.startSection("info");
        sb.addField("client-addr", clientAddr);
        sb.addField("login-name", loginName);
        sb.addField("auth-type", authenticationTypeValue);
        sb.addField("type", eventValue);
    }

    @Transient
    public String getSyslogId()
    {
        return "CPD Login";
    }

    @Transient
    public SyslogPriority getSyslogPriority()
    {
        return SyslogPriority.INFORMATIONAL;
    }

    // Object methods ------------------------------------------------------

    public String toString()
    {
        return "CPDLoginEvent id: " + getId() + " login-name: " + loginName + " authenticationType: " + authenticationTypeValue +
        " event: " + eventValue;
    }
}
