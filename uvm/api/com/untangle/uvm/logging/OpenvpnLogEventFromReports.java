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

import com.untangle.uvm.logging.PipelineEvent;
import com.untangle.uvm.logging.SyslogBuilder;
import com.untangle.uvm.logging.SyslogPriority;
import com.untangle.uvm.node.PipelineEndpoints;

/**
 * Log event from the denormalized reports.n_openvpn_stats
 *
 * @author Sebastien Delafond
 * @version 1.0
 */
@Entity
    @org.hibernate.annotations.Entity(mutable=false)
    @Table(name="n_openvpn_stats", schema="reports")
    @SuppressWarnings("serial")
    public class OpenvpnLogEventFromReports extends LogEvent
    {
        private Long rxBytes;
        private Long txBytes;
        private Float seconds;
        private Date endTime;
        private Date startTime;
        private InetAddress remoteAddress;
        private Integer remotePort;
        private String clientName;

        @Column(name="rx_bytes")
            public Long getRxBytes() { return rxBytes; }
        public void setRxBytes(Long rxBytes) { this.rxBytes = rxBytes; }

        @Column(name="tx_bytes")
            public Long getTxBytes() { return txBytes; }
        public void setTxBytes(Long txBytes) { this.txBytes = txBytes; }

        @Column(name="seconds")
            public Float getSeconds() { return seconds; }
        public void setSeconds(Float seconds) { this.seconds = seconds; }

        @Temporal(TemporalType.TIMESTAMP)
        @Column(name="end_time")
        public Date getEndTime() { return endTime; }
        public void setEndTime(Date endTime) { this.endTime = endTime; }

        @Temporal(TemporalType.TIMESTAMP)
        @Column(name="start_time")
        public Date getStartTime() { return startTime; }
        public void setStartTime(Date startTime) { this.startTime = startTime; }

        @Column(name="remote_address")
        @Type(type="com.untangle.uvm.type.InetAddressUserType")
            public InetAddress getRemoteAddress() { return remoteAddress; }
        public void setRemoteAddress(InetAddress remoteAddress) { this.remoteAddress = remoteAddress; }

        @Column(name="remote_port")
            public Integer getRemotePort() { return remotePort; }
        public void setRemotePort(Integer remotePort) { this.remotePort = remotePort; }

        @Column(name="client_name")
            public String getClientName() { return clientName; }
        public void setClientName(String clientName) { this.clientName = clientName; }

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
