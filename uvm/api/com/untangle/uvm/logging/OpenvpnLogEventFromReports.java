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
        private Long seconds;

        @Column(name="rx_bytes")
            public Long getRxBytes() { return rxBytes; }
        public void setRxBytes(Long rxBytes) { this.rxBytes = rxBytes; }

        @Column(name="tx_bytes")
            public Long getTxBytes() { return txBytes; }
        public void setTxBytes(Long txBytes) { this.txBytes = txBytes; }

        @Column(name="seconds")
            public Long getSeconds() { return seconds; }
        public void setSeconds(Long seconds) { this.seconds = seconds; }

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
