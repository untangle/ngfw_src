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
 * Log event from the denormalized reports.n_cpd_block_events reports table
 *
 * @author Sebastien Delafond
 * @version 1.0
 */
@Entity
    @org.hibernate.annotations.Entity(mutable=false)
    @Table(name="n_cpd_block_events", schema="reports")
    @SuppressWarnings("serial")
    public class CpdBlockEventsFromReports extends LogEvent
    {
        private InetAddress clientAddress;
        private InetAddress serverAddress;
        private Integer clientPort;
        private Integer serverPort;
        private Integer clientIntf;
        private Integer proto;

        @Column(name="client_address")
        @Type(type="com.untangle.uvm.type.InetAddressUserType")
        public InetAddress getClientAddress() { return clientAddress; }
        public void setClientAddress(InetAddress clientAddress) { this.clientAddress = clientAddress; }

        @Column(name="server_address")
        @Type(type="com.untangle.uvm.type.InetAddressUserType")
        public InetAddress getServerAddress() { return serverAddress; }
        public void setServerAddress(InetAddress serverAddress) { this.serverAddress = serverAddress; }

        @Column(name="client_port")
        public Integer getClientPort() { return clientPort; }
        public void setClientPort(Integer clientPort) { this.clientPort = clientPort; }

        @Column(name="server_port")
        public Integer getServerPort() { return serverPort; }
        public void setServerPort(Integer serverPort) { this.serverPort = serverPort; }

        @Column(name="proto")
        public Integer getProto() { return proto; }
        public void setProto(Integer proto) { this.proto = proto; }

        @Column(name="client_intf")
        public Integer getClientIntf() { return clientIntf; }
        public void setClientIntf(Integer clientIntf) { this.clientIntf = clientIntf; }

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
