package com.untangle.uvm.logging;

import java.net.InetAddress;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;

import com.untangle.uvm.logging.SyslogBuilder;
import com.untangle.uvm.logging.SyslogPriority;

/**
 * Log event from the denormalized reports.n_cpd_login_events reports table
 *
 * @author Sebastien Delafond
 * @version 1.0
 */
@Entity
    @org.hibernate.annotations.Entity(mutable=false)
    @Table(name="n_cpd_login_events", schema="reports")
    @SuppressWarnings("serial")
    public class CpdLoginEventsFromReports extends LogEvent
    {
        private String loginName;
        private String event;
        private String authType;
        private InetAddress clientAddr;

        @Column(name="login_name")
            public String getLoginName() { return loginName; }
        public void setLoginName(String loginName) { this.loginName = loginName; }

        @Column(name="event")
            public String getEvent() { return event; }
        public void setEvent(String event) { this.event = event; }

        @Column(name="auth_type")
            public String getAuthType() { return authType; }
        public void setAuthType(String authType) { this.authType = authType; }

        @Column(name="client_addr")
        @Type(type="com.untangle.uvm.type.InetAddressUserType")
        public InetAddress getClientAddr() { return clientAddr; }
        public void setClientAddr(InetAddress clientAddr) { this.clientAddr = clientAddr; }

        public void appendSyslog(SyslogBuilder sb) // FIXME: not called for now
        {
        }

        @Transient
        public String getSyslogId()
        {
            return ""; // FIMXE ?
        }

        @Transient
        public SyslogPriority getSyslogPriority()
        {
            // FIXME
            return SyslogPriority.INFORMATIONAL;
        }

    }
