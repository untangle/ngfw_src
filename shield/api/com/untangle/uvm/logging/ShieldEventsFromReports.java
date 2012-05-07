/**
 * $Id: ShieldEventsFromReports.java,v 1.00 2011/12/27 17:11:16 dmorris Exp $
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

import com.untangle.uvm.node.IPAddress;
import com.untangle.uvm.logging.SyslogBuilder;
import com.untangle.uvm.logging.SyslogPriority;

/**
 * Log event from the denormalized reports.n_shield_rejection_totals reports table
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
@Table(name="n_shield_rejection_totals", schema="reports")
@SuppressWarnings("serial")
public class ShieldEventsFromReports extends LogEventFromReports
{
    private IPAddress clientAddr;
    private Integer clientIntf;
    private Integer mode;
    private Integer limited;
    private Integer dropped;
    private Integer rejected;
    private Double  reputation;

    @Column(name="client_addr", nullable=false)
    @Type(type="com.untangle.uvm.type.IPAddressUserType")
    public IPAddress getClientAddr() { return clientAddr; }
    public void setClientAddr(IPAddress clientAddr) { this.clientAddr = clientAddr; }

    @Column(name="client_intf")
    public Integer getClientIntf() { return clientIntf; }
    public void setClientIntf(Integer clientIntf) { this.clientIntf = clientIntf; }

    @Column(name="mode")
    public Integer getMode() { return mode; }
    public void setMode(Integer mode) { this.mode = mode; }

    @Column(name="limited")
    public Integer getLimited() { return limited; }
    public void setLimited(Integer limited) { this.limited = limited; }

    @Column(name="dropped")
    public Integer getDropped() { return dropped; }
    public void setDropped(Integer dropped) { this.dropped = dropped; }

    @Column(name="rejected")
    public Integer getRejected() { return rejected; }
    public void setRejected(Integer rejected) { this.rejected = rejected; }

    @Column(name="reputation")
    public Double getReputation() { return reputation; }
    public void setReputation(Double reputation) { this.reputation = reputation; }
    
    
    public void appendSyslog(SyslogBuilder sb) 
    {
    }

    @Transient
    public String getSyslogId()
    {
        return ""; 
    }

    @Transient
    public SyslogPriority getSyslogPriority()
    {
        return SyslogPriority.INFORMATIONAL;
    }

}
