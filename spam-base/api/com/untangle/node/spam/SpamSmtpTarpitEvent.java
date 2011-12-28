/**
 * $Id$
 */
package com.untangle.node.spam;

import java.net.InetAddress;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;

import com.untangle.uvm.logging.PipelineEvent;
import com.untangle.uvm.logging.SyslogBuilder;
import com.untangle.uvm.logging.SyslogPriority;
import com.untangle.uvm.node.IPAddress;
import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.node.PipelineEndpoints;

/**
 * Log for Spam SMTP Tarpit events.
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
@Table(name="n_spam_smtp_tarpit_evt", schema="events")
@SuppressWarnings("serial")
public class SpamSmtpTarpitEvent extends PipelineEvent
{
    private String hostname;
    private IPAddress ipAddr;
    private String vendorName;

    // constructors -----------------------------------------------------------

    public SpamSmtpTarpitEvent() {}

    public SpamSmtpTarpitEvent(PipelineEndpoints plEndp, String hostname, IPAddress ipAddr, String vendorName)
    {
        super(plEndp);
        this.hostname = hostname;
        this.ipAddr = ipAddr;
        this.vendorName = vendorName;
    }

    public SpamSmtpTarpitEvent(PipelineEndpoints plEndp, String hostname, InetAddress ipAddrIN, String vendorName)
    {
        super(plEndp);
        this.hostname = hostname;
        this.ipAddr = new IPAddress(ipAddrIN);
        this.vendorName = vendorName;
    }

    // accessors --------------------------------------------------------------

    /**
     * Hostname of DNSBL service.
     *
     * @return hostname of DNSBL service.
     */
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

    /**
     * IP address of mail server listed on DNSBL service.
     *
     * @return IP address of mail server listed on DNSBL service.
     */
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

    /**
     * Spam scanner vendor.
     *
     * @return the vendor
     */
    @Column(name="vendor_name")
    public String getVendorName()
    {
        return vendorName;
    }

    public void setVendorName(String vendorName)
    {
        this.vendorName = vendorName;
    }

    // Syslog methods ---------------------------------------------------------

    @Transient
    public void appendSyslog(SyslogBuilder sb)
    {
        // No longer log pipeline endpoints, they are not necessary anyway.
        // PipelineEndpoints pe = getPipelineEndpoints();
        /* unable to log this event */
        // pe.appendSyslog(sb);

        sb.startSection("info");
        sb.addField("hostname", getHostname().toString());
        sb.addField("ipaddr", getIPAddr().toString());
        sb.addField("vendorName", getVendorName().toString());
    }

    @Transient
    public String getSyslogId()
    {
        return "SMTP_TARPIT";
    }

    @Transient
    public SyslogPriority getSyslogPriority()
    {
        // INFORMATIONAL = statistics or normal operation
        // WARNING = traffic altered
        return SyslogPriority.WARNING; // traffic altered
    }
}
