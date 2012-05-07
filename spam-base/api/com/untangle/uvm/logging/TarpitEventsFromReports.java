/**
 * $Id: TarpitEventsFromReports.java,v 1.00 2011/12/27 17:11:16 dmorris Exp $
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
 * Log event from the denormalized reports.n_spam_smtp_tarpit_events reports table
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
@Table(name="n_spam_smtp_tarpit_events", schema="reports")
@SuppressWarnings("serial")
public class TarpitEventsFromReports extends LogEventFromReports
{
    private String hostname;
    private IPAddress ipAddr;
    private String vendorName;
    private Long policyId;

    @Column(nullable=false)
    public String getHostname()
    {
        return hostname;
    }

    public void setHostname(String hostname)
    {
        this.hostname = hostname;
        return;
    }

    @Column(nullable=false)
    @Type(type="com.untangle.uvm.type.IPAddressUserType")
    public IPAddress getIPAddr()
    {
        return ipAddr;
    }

    public void setIPAddr(IPAddress ipAddr)
    {
        this.ipAddr = ipAddr;
        return;
    }

    @Column(name="vendor_name")
    public String getVendorName()
    {
        return vendorName;
    }

    public void setVendorName(String vendorName)
    {
        this.vendorName = vendorName;
    }

    @Column(name="policy_id")
    public Long getPolicyId()
    {
        return policyId;
    }

    public void setPolicyId(Long policyId)
    {
        this.policyId = policyId;
    }
    
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
