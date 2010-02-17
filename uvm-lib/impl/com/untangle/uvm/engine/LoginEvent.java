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

package com.untangle.uvm.engine;

import java.net.InetAddress;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.logging.SyslogBuilder;
import com.untangle.uvm.logging.SyslogPriority;
import com.untangle.uvm.security.LoginFailureReason;

/**
 * Log event for a login/login-attempt.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
    @Table(name="u_login_evt", schema="events")
    public class LoginEvent extends LogEvent
    {
        private InetAddress clientAddr;
        private String login;
        private boolean local;
        private boolean succeeded;
        private LoginFailureReason reason;

        // constructors --------------------------------------------------------

        public LoginEvent() { }

        // For successes
        public LoginEvent(InetAddress clientAddr, String login, boolean local, boolean succeeded)
        {
            this(clientAddr, login, local, succeeded, null);
        }

        // For failures
        public LoginEvent(InetAddress clientAddr, String login, boolean local, boolean succeeded, LoginFailureReason reason)
        {
            if (login.length() > DEFAULT_STRING_SIZE) login = login.substring(0, DEFAULT_STRING_SIZE);
            this.clientAddr = clientAddr;
            this.login = login;
            this.local = local;
            this.succeeded = succeeded;
            this.reason = reason;
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
         * Login used to login.  May be  used to join to UVM_USER.
         *
         * @return a <code>String</code> giving the login for the user
         */
        public String getLogin()
        {
            return login;
        }

        public void setLogin(String login)
        {
            if (login.length() > DEFAULT_STRING_SIZE) login = login.substring(0, DEFAULT_STRING_SIZE);
            this.login = login;
        }

        /**
         * Whether or not the login is local (from internal tools on
         * system, ignored for reporting).
         *
         * @return whether or not the login was local
         */
        @Column(nullable=false)
        public boolean isLocal()
        {
            return local;
        }

        public void setLocal(boolean local)
        {
            this.local = local;
        }

        /**
         * Whether or not the login succeeeded, if not there will be a reason.
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
        @Type(type="com.untangle.uvm.security.LoginFailureReasonUserType")
        public LoginFailureReason getReason()
        {
            return reason;
        }

        public void setReason(LoginFailureReason reason)
        {
            this.reason = reason;
        }

        // Syslog methods ------------------------------------------------------

        public void appendSyslog(SyslogBuilder sb)
        {
            sb.startSection("info");
            sb.addField("client-addr", clientAddr);
            sb.addField("login", login);
            sb.addField("local", local);
            sb.addField("succeeded", succeeded);
            if (reason != null)
                // Don't need a reason for success
                sb.addField("reason", reason.toString());
        }

        @Transient
        public String getSyslogId()
        {
            return "AdminLogin"; // XXX
        }

        @Transient
        public SyslogPriority getSyslogPriority()
        {
            if (false == succeeded) {
                return SyslogPriority.WARNING; // login attempt failed
            } else {
                // local logins are not as interesting to enduser
                return true == local ? SyslogPriority.DEBUG : SyslogPriority.INFORMATIONAL;
            }
        }

        // Object methods ------------------------------------------------------

        public String toString()
        {
            return "LoginEvent id: " + getId() + " login: " + login + " succeeded: " + succeeded;
        }
    }
