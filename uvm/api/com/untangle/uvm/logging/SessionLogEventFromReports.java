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
 * Log event from the denormalized reports.sessions table
 *
 * @author Sebastien Delafond
 * @version 1.0
 */
@Entity
    @org.hibernate.annotations.Entity(mutable=false)
    @Table(name="sessions", schema="reports")
    @SuppressWarnings("serial")
    public class SessionLogEventFromReports extends LogEvent
    {
        private Long plEndpId;
        private Date endTime;
        private String hname;
        private Long uid;
        private Long policyId;
        private InetAddress cClientAddr;
        private InetAddress cServerAddr;
        private Integer cServerPort;
        private Integer cClientPort;
        private Integer clientIntf;
        private Integer serverIntf;
        private Long c2pBytes;
        private Long p2cBytes;
        private Long s2pBytes;
        private Long p2sBytes;
        private Long bandwidthPriority;
        private Boolean firewallWasBlocked;
        private Integer firewallRuleIndex;
        private String firewallRuleDescription;
        private String pfProtocol;
        private Boolean pfBlocked;
        private Boolean ipsBlocked;
        private String ipsName;
        private String ipsDescription;
        private String swAccessIdent;

        // constructors -----------------------------------------------------------

        protected SessionLogEventFromReports() { }

        // accessors --------------------------------------------------------------
        @Column(name="pl_endp_id")
        public Long getPlEndpId() { return plEndpId; }
        public void setPlEndpId(Long id) { this.plEndpId = id; }

        @Temporal(TemporalType.TIMESTAMP)
        @Column(name="end_time")
        public Date getEndTime() { return endTime; }
        public void setEndTime(Date endTime) { this.endTime = endTime; }

        @Column(name="hname")        
        public String getHname() { return hname; }
        public void setHname(String hname) { this.hname = hname; }

        @Column(name="uid")
        public Long getUid() { return uid; }
        public void setUid(Long uid) { this.uid = uid; }

        @Column(name="policy_id")
        public Long getPolicyId() { return policyId; }
        public void setPolicyId(Long policyId) { this.policyId = policyId; }

        @Column(name="c_client_addr")
        @Type(type="com.untangle.uvm.type.InetAddressUserType")
        public InetAddress getCClientAddr() { return cClientAddr; }
        public void setCClientAddr(InetAddress cClientAddr) { this.cClientAddr = cClientAddr; }

        @Column(name="c_server_addr")
        @Type(type="com.untangle.uvm.type.InetAddressUserType")
        public InetAddress getCServerAddr() { return cServerAddr; }
        public void setCServerAddr(InetAddress cServerAddr) { this.cServerAddr = cServerAddr; }

        @Column(name="c_server_port")
        public Integer getCServerPort() { return cServerPort; }
        public void setCServerPort(Integer cServerPort) { this.cServerPort = cServerPort; }

        @Column(name="c_client_port")
        public Integer getCClientPort() { return cClientPort; }
        public void setCClientPort(Integer cClientPort) { this.cClientPort = cClientPort; }

        @Column(name="client_intf")
        public Integer getClientIntf() { return clientIntf; }
        public void setClientIntf(Integer clientIntf) { this.clientIntf = clientIntf; }

        @Column(name="server_intf")
        public Integer getServerIntf() { return serverIntf; }
        public void setServerIntf(Integer serverIntf) { this.serverIntf = serverIntf; }

        @Column(name="c2p_bytes")
        public Long getC2pBytes() { return c2pBytes; }
        public void setC2pBytes(Long c2pBytes) { this.c2pBytes = c2pBytes; }

        @Column(name="p2c_bytes")
        public Long getP2cBytes() { return p2cBytes; }
        public void setP2cBytes(Long p2cBytes) { this.p2cBytes = p2cBytes; }

        @Column(name="s2p_bytes")
        public Long getS2pBytes() { return s2pBytes; }
        public void setS2pBytes(Long s2pBytes) { this.s2pBytes = s2pBytes; }

        @Column(name="p2s_bytes")
        public Long getP2sBytes() { return p2sBytes; }
        public void setP2sBytes(Long p2sBytes) { this.p2sBytes = p2sBytes; }

        @Column(name="bandwidth_priority")
        public Long getBandwidthPriority() { return bandwidthPriority; }
        public void setBandwidthPriority(Long bandwidthPriority) { this.bandwidthPriority = bandwidthPriority; }

        @Column(name="firewall_was_blocked")
        public Boolean getFirewallWasBlocked() { return firewallWasBlocked; }
        public void setFirewallWasBlocked(Boolean firewallWasBlocked) { this.firewallWasBlocked = firewallWasBlocked; }

        @Column(name="firewall_rule_index")
        public Integer getFirewallRuleIndex() { return firewallRuleIndex; }
        public void setFirewallRuleIndex(Integer firewallRuleIndex) { this.firewallRuleIndex = firewallRuleIndex; }

        @Column(name="firewall_rule_description")
        public String getFirewallRuleDescription() { return firewallRuleDescription; }
        public void setFirewallRuleDescription(String firewallRuleDescription) { this.firewallRuleDescription = firewallRuleDescription; }

        @Column(name="pf_protocol")
        public String getPfProtocol() { return pfProtocol; }
        public void setPfProtocol(String pfProtocol) { this.pfProtocol = pfProtocol; }

        @Column(name="pf_blocked")
        public Boolean getPfBlocked() { return pfBlocked; }
        public void setPfBlocked(Boolean pfBlocked) { this.pfBlocked = pfBlocked; }

        @Column(name="ips_blocked")
        public Boolean getIpsBlocked() { return ipsBlocked; }
        public void setIpsBlocked(Boolean ipsBlocked) { this.ipsBlocked = ipsBlocked; }

        @Column(name="ips_name")
        public String getIpsName() { return ipsName; }
        public void setIpsName(String ipsName) { this.ipsName = ipsName; }

        @Column(name="ips_description")
        public String getIpsDescription() { return ipsDescription; }
        public void setIpsDescription(String ipsDescription) { this.ipsDescription = ipsDescription; }

        @Column(name="sw_access_ident")
        public String getSwAccessIdent() { return swAccessIdent; }
        public void setSwAccessIdent(String swAccessIdent) { this.swAccessIdent = swAccessIdent; }

        public void appendSyslog(SyslogBuilder sb) // FIXME: not called for now
        {
            sb.startSection("endpoints");
            sb.addField("create-date", getTimeStamp());

            Long policyId = getPolicyId();
            sb.addField("policy", ((policyId == null) ? "<none>" : policyId.toString()));

            sb.addField("client-iface", getClientIntf());
            sb.addField("client-addr", getCClientAddr());
            sb.addField("client-port", getCClientPort());
            sb.addField("server-addr", getCServerAddr());
            sb.addField("server-port", getCServerPort());

            sb.startSection("info");
            sb.addField("pf-protocol", getPfProtocol());
            sb.addField("pf-blocked", getPfBlocked());

            sb.addField("ips-name", getIpsName());
            sb.addField("ips-blocked", getIpsBlocked());

            sb.addField("fw-description", getFirewallRuleDescription());
            sb.addField("fw-blocked", getFirewallWasBlocked());

            sb.addField("sw-access", getSwAccessIdent());
        }

        @Transient
        public String getSyslogId()
        {
            return ""; // FIMXE ?
        }

        @Transient
        public SyslogPriority getSyslogPriority()
        {
            // WARNING = traffic altered
            // INFORMATIONAL = statistics or normal operation
            if (getPfBlocked() || 
                getIpsBlocked() ||
                getFirewallWasBlocked() ||
                (getSwAccessIdent() != null && getSwAccessIdent() != ""))
                return SyslogPriority.WARNING;
            else
                return SyslogPriority.INFORMATIONAL;
        }


    }
