/**
 * $Id: HttpLogEventFromReports.java,v 1.00 2012/01/07 17:03:10 dmorris Exp $
 */
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
 * Log event from the denormalized reports.n_http_events table
 *
 * @author Sebastien Delafond
 * @version 1.0
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
@Table(name="n_http_events", schema="reports")
@SuppressWarnings("serial")
public class HttpLogEventFromReports extends LogEvent
{
    private Long sessionId;
    private Integer clientIntf;
    private Integer serverIntf;
    private InetAddress cClientAddr;
    private InetAddress sClientAddr;
    private InetAddress cServerAddr;
    private InetAddress sServerAddr;
    private Integer cClientPort;
    private Integer sClientPort;
    private Integer cServerPort;
    private Integer sServerPort;
    private Long policyId;
    private Long c2pBytes;
    private Long s2pBytes;
    private Long p2cBytes;
    private Long p2sBytes;
    private String uid;
    private Long requestId;
    private String method;
    private String uri;
    private String host;
    private Integer c2sContentLength;
    private Integer s2cContentLength;
    private String s2cContentType;
    private String hname;
    private String wfEsoftReason;
    private String wfEsoftCategory;
    private Boolean wfEsoftBlocked;
    private Boolean wfEsoftFlagged;
    private Boolean virusClamClean;
    private String virusClamName;
    private Boolean swBlacklisted;
    private String swCookieIdent;
    private Boolean virusKasperskyClean;
    private String virusKasperskyName;
    private Boolean virusCommtouchClean;
    private String virusCommtouchName;
    private String wfUntangleReason;
    private String wfUntangleCategory;
    private Boolean wfUntangleBlocked;
    private Boolean wfUntangleFlagged;
    private String abAction;
    private Character phishAction;

    // constructors -----------------------------------------------------------

    protected HttpLogEventFromReports() { }

    // accessors --------------------------------------------------------------
    @Column(name="session_id")
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }

    @Column(name="client_intf")
    public Integer getClientIntf() { return clientIntf; }
    public void setClientIntf(Integer clientIntf) { this.clientIntf = clientIntf; }

    @Column(name="server_intf")
    public Integer getServerIntf() { return serverIntf; }
    public void setServerIntf(Integer serverIntf) { this.serverIntf = serverIntf; }

    @Column(name="c_client_addr")
    @Type(type="com.untangle.uvm.type.InetAddressUserType")
    public InetAddress getCClientAddr() { return cClientAddr; }
    public void setCClientAddr(InetAddress cClientAddr) { this.cClientAddr = cClientAddr; }

    @Column(name="s_client_addr")
    @Type(type="com.untangle.uvm.type.InetAddressUserType")
    public InetAddress getSClientAddr() { return sClientAddr; }
    public void setSClientAddr(InetAddress sClientAddr) { this.sClientAddr = sClientAddr; }

    @Column(name="c_server_addr")
    @Type(type="com.untangle.uvm.type.InetAddressUserType")
    public InetAddress getCServerAddr() { return cServerAddr; }
    public void setCServerAddr(InetAddress cServerAddr) { this.cServerAddr = cServerAddr; }

    @Column(name="s_server_addr")
    @Type(type="com.untangle.uvm.type.InetAddressUserType")
    public InetAddress getSServerAddr() { return sServerAddr; }
    public void setSServerAddr(InetAddress sServerAddr) { this.sServerAddr = sServerAddr; }

    @Column(name="c_client_port")
    public Integer getCClientPort() { return cClientPort; }
    public void setCClientPort(Integer cClientPort) { this.cClientPort = cClientPort; }

    @Column(name="s_client_port")
    public Integer getSClientPort() { return sClientPort; }
    public void setSClientPort(Integer sClientPort) { this.sClientPort = sClientPort; }

    @Column(name="c_server_port")
    public Integer getCServerPort() { return cServerPort; }
    public void setCServerPort(Integer cServerPort) { this.cServerPort = cServerPort; }

    @Column(name="s_server_port")
    public Integer getSServerPort() { return sServerPort; }
    public void setSServerPort(Integer sServerPort) { this.sServerPort = sServerPort; }

    @Column(name="policy_id")
    public Long getPolicyId() { return policyId; }
    public void setPolicyId(Long policyId) { this.policyId = policyId; }

    @Column(name="uid")
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    @Column(name="request_id")
    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) { this.requestId = requestId; }

    @Column(name="method")
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    @Column(name="uri")
    public String getUri() { return uri; }
    public void setUri(String uri) { this.uri = uri; }

    @Column(name="host")
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    @Column(name="c2s_content_length")
    public Integer getC2sContentLength() { return c2sContentLength; }
    public void setC2sContentLength(Integer c2sContentLength) { this.c2sContentLength = c2sContentLength; }

    @Column(name="s2c_content_length")
    public Integer getS2cContentLength() { return s2cContentLength; }
    public void setS2cContentLength(Integer s2cContentLength) { this.s2cContentLength = s2cContentLength; }

    @Column(name="s2c_content_type")
    public String getS2cContentType() { return s2cContentType; }
    public void setS2cContentType(String s2cContentType) { this.s2cContentType = s2cContentType; }

    @Column(name="hname")
    public String getHname() { return hname; }
    public void setHname(String hname) { this.hname = hname; }

    @Column(name="wf_esoft_reason")
    public String getWfEsoftReason() { return wfEsoftReason; }
    public void setWfEsoftReason(String wfEsoftReason) { this.wfEsoftReason = wfEsoftReason; }

    @Column(name="wf_esoft_category")
    public String getWfEsoftCategory() { return wfEsoftCategory; }
    public void setWfEsoftCategory(String wfEsoftCategory) { this.wfEsoftCategory = wfEsoftCategory; }

    @Column(name="wf_esoft_blocked")
    public Boolean getWfEsoftBlocked() { return wfEsoftBlocked; }
    public void setWfEsoftBlocked(Boolean wfEsoftBlocked) { this.wfEsoftBlocked = wfEsoftBlocked; }

    @Column(name="wf_esoft_flagged")
    public Boolean getWfEsoftFlagged() { return wfEsoftFlagged; }
    public void setWfEsoftFlagged(Boolean wfEsoftFlagged) { this.wfEsoftFlagged = wfEsoftFlagged; }

    @Column(name="virus_clam_clean")
    public Boolean getVirusClamClean() { return virusClamClean; }
    public void setVirusClamClean(Boolean virusClamClean) { this.virusClamClean = virusClamClean; }

    @Column(name="virus_clam_name")
    public String getVirusClamName() { return virusClamName; }
    public void setVirusClamName(String virusClamName) { this.virusClamName = virusClamName; }

    @Column(name="sw_blacklisted")
    public Boolean getSwBlacklisted() { return swBlacklisted; }
    public void setSwBlacklisted(Boolean swBlacklisted) { this.swBlacklisted = swBlacklisted; }

    @Column(name="sw_cookie_ident")
    public String getSwCookieIdent() { return swCookieIdent; }
    public void setSwCookieIdent(String swCookieIdent) { this.swCookieIdent = swCookieIdent; }

    @Column(name="virus_kaspersky_clean")
    public Boolean getVirusKasperskyClean() { return virusKasperskyClean; }
    public void setVirusKasperskyClean(Boolean virusKasperskyClean) { this.virusKasperskyClean = virusKasperskyClean; }

    @Column(name="virus_kaspersky_name")
    public String getVirusKasperskyName() { return virusKasperskyName; }
    public void setVirusKasperskyName(String virusKasperskyName) { this.virusKasperskyName = virusKasperskyName; }

    @Column(name="virus_commtouch_clean")
    public Boolean getVirusCommtouchClean() { return virusCommtouchClean; }
    public void setVirusCommtouchClean(Boolean virusCommtouchClean) { this.virusCommtouchClean = virusCommtouchClean; }

    @Column(name="virus_commtouch_name")
    public String getVirusCommtouchName() { return virusCommtouchName; }
    public void setVirusCommtouchName(String virusCommtouchName) { this.virusCommtouchName = virusCommtouchName; }

    @Column(name="wf_untangle_reason")
    public String getWfUntangleReason() { return wfUntangleReason; }
    public void setWfUntangleReason(String wfUntangleReason) { this.wfUntangleReason = wfUntangleReason; }

    @Column(name="wf_untangle_category")
    public String getWfUntangleCategory() { return wfUntangleCategory; }
    public void setWfUntangleCategory(String wfUntangleCategory) { this.wfUntangleCategory = wfUntangleCategory; }

    @Column(name="wf_untangle_blocked")
    public Boolean getWfUntangleBlocked() { return wfUntangleBlocked; }
    public void setWfUntangleBlocked(Boolean wfUntangleBlocked) { this.wfUntangleBlocked = wfUntangleBlocked; }

    @Column(name="wf_untangle_flagged")
    public Boolean getWfUntangleFlagged() { return wfUntangleFlagged; }
    public void setWfUntangleFlagged(Boolean wfUntangleFlagged) { this.wfUntangleFlagged = wfUntangleFlagged; }

    @Column(name="ab_action")
    public String getAbAction() { return abAction; }
    public void setAbAction(String abAction) { this.abAction = abAction; }

    @Column(name="phish_action")
    public Character getPhishAction() { return phishAction; }
    public void setPhishAction(Character phishAction) { this.phishAction = phishAction; }
    
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