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
        private Long sessionId;
        private Date endTime;
        private String hname;
        private String uid;
        private Long policyId;
        private InetAddress cClientAddr;
        private InetAddress cServerAddr;
        private Integer cServerPort;
        private Integer cClientPort;
        private InetAddress sClientAddr;
        private InetAddress sServerAddr;
        private Integer sServerPort;
        private Integer sClientPort;
        private Integer clientIntf;
        private Integer serverIntf;
        private Long c2pBytes;
        private Long p2cBytes;
        private Long s2pBytes;
        private Long p2sBytes;
        private Long bandwidthPriority;
        private Long bandwidthRule;
        private Boolean firewallWasBlocked;
        private Integer firewallRuleIndex;
        private String firewallRuleDescription;
        private String pfProtocol;
        private Boolean pfBlocked;
        private String classdApplication;
        private String classdProtoChain;
        private String classdDetail;
        private Boolean classdBlocked;
        private Boolean classdFlagged;
        private Integer classdConfidence;
        private Integer classdRuleId;
        private Boolean ipsBlocked;
        private String ipsName;
        private String ipsDescription;
        private String swAccessIdent;

        // constructors -----------------------------------------------------------

        protected SessionLogEventFromReports() { }

        // accessors --------------------------------------------------------------
        @Column(name="session_id")
        public Long getSessionId() { return sessionId; }
        public void setSessionId(Long sessionId) { this.sessionId = sessionId; }

        @Temporal(TemporalType.TIMESTAMP)
        @Column(name="end_time")
        public Date getEndTime() { return endTime; }
        public void setEndTime(Date endTime) { this.endTime = endTime; }

        @Column(name="hname")        
        public String getHname() { return hname; }
        public void setHname(String hname) { this.hname = hname; }

        @Column(name="uid")
        public String getUid() { return uid; }
        public void setUid(String uid) { this.uid = uid; }

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

        @Column(name="s_client_addr")
        @Type(type="com.untangle.uvm.type.InetAddressUserType")
        public InetAddress getSClientAddr() { return sClientAddr; }
        public void setSClientAddr(InetAddress sClientAddr) { this.sClientAddr = sClientAddr; }

        @Column(name="s_server_addr")
        @Type(type="com.untangle.uvm.type.InetAddressUserType")
        public InetAddress getSServerAddr() { return sServerAddr; }
        public void setSServerAddr(InetAddress sServerAddr) { this.sServerAddr = sServerAddr; }

        @Column(name="s_server_port")
        public Integer getSServerPort() { return sServerPort; }
        public void setSServerPort(Integer sServerPort) { this.sServerPort = sServerPort; }

        @Column(name="s_client_port")
        public Integer getSClientPort() { return sClientPort; }
        public void setSClientPort(Integer sClientPort) { this.sClientPort = sClientPort; }
        
        @Column(name="client_intf")
        public Integer getClientIntf() { return clientIntf; }
        public void setClientIntf(Integer clientIntf) { this.clientIntf = clientIntf; }

        @Column(name="server_intf")
        public Integer getServerIntf() { return serverIntf; }
        public void setServerIntf(Integer serverIntf) { this.serverIntf = serverIntf; }

        @Column(name="bandwidth_priority")
        public Long getBandwidthPriority() { return bandwidthPriority; }
        public void setBandwidthPriority(Long bandwidthPriority) { this.bandwidthPriority = bandwidthPriority; }

        @Column(name="bandwidth_rule")
        public Long getBandwidthRule() { return bandwidthRule; }
        public void setBandwidthRule(Long bandwidthRule) { this.bandwidthRule = bandwidthRule; }

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

        @Column(name="classd_application")
        public String getClassdApplication() { return classdApplication; }
        public void setClassdApplication(String classdApplication) { this.classdApplication = classdApplication; }

        @Column(name="classd_protochain")
        public String getClassdProtoChain() { return classdProtoChain; }
        public void setClassdProtoChain(String classdProtoChain) { this.classdProtoChain = classdProtoChain; }

        @Column(name="classd_detail")
        public String getClassdDetail() { return classdDetail; }
        public void setClassdDetail(String classdDetail) { this.classdDetail = classdDetail; }
        
        @Column(name="classd_blocked")
        public Boolean getClassdBlocked() { return classdBlocked; }
        public void setClassdBlocked(Boolean classdBlocked) { this.classdBlocked = classdBlocked; }

        @Column(name="classd_flagged")
        public Boolean getClassdFlagged() { return classdFlagged; }
        public void setClassdFlagged(Boolean classdFlagged) { this.classdFlagged = classdFlagged; }

        @Column(name="classd_confidence")
        public Integer getClassdConfidence() { return classdConfidence; }
        public void setClassdConfidence(Integer classdConfidence) { this.classdConfidence = classdConfidence; }

        @Column(name="classd_ruleid")
        public Integer getClassdRuleId() { return classdRuleId; }
        public void setClassdRuleId(Integer classdRuleId) { this.classdRuleId = classdRuleId; }
        
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
//             sb.startSection("endpoints");
//             sb.addField("create-date", getTimeStamp());

//             Long policyId = getPolicyId();
//             sb.addField("policy", ((policyId == null) ? "<none>" : policyId.toString()));

//             sb.addField("client-iface", getClientIntf());
//             sb.addField("client-addr", getCClientAddr());
//             sb.addField("client-port", getCClientPort());
//             sb.addField("server-addr", getCServerAddr());
//             sb.addField("server-port", getCServerPort());

//             sb.startSection("info");
//             sb.addField("pf-protocol", getPfProtocol());
//             sb.addField("pf-blocked", getPfBlocked());

//             sb.addField("ips-name", getIpsName());
//             sb.addField("ips-blocked", getIpsBlocked());

//             sb.addField("fw-description", getFirewallRuleDescription());
//             sb.addField("fw-blocked", getFirewallWasBlocked());

//             sb.addField("sw-access", getSwAccessIdent());
        }

        @Transient
        public String getSyslogId()
        {
            return ""; // FIMXE ?
        }

        @Transient
        public SyslogPriority getSyslogPriority()
        {
//             // WARNING = traffic altered
//             // INFORMATIONAL = statistics or normal operation
//             if (getPfBlocked() || 
//                 getIpsBlocked() ||
//                 getFirewallWasBlocked() ||
//                 (getSwAccessIdent() != null && getSwAccessIdent() != ""))
//                 return SyslogPriority.WARNING;
//             else
            return SyslogPriority.INFORMATIONAL;
        }


    }
