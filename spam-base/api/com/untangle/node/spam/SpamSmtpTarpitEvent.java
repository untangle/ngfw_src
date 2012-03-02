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

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.logging.SyslogBuilder;
import com.untangle.uvm.logging.SyslogPriority;
import com.untangle.uvm.node.IPAddress;
import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.node.SessionEvent;

/**
 * Log for Spam SMTP Tarpit events.
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
@Table(name="n_spam_smtp_tarpit_evt", schema="events")
@SuppressWarnings("serial")
public class SpamSmtpTarpitEvent extends LogEvent
{
    private SessionEvent sessionEvent;
    private String hostname;
    private IPAddress ipAddr;
    private String vendorName;

    // constructors -----------------------------------------------------------

    public SpamSmtpTarpitEvent() {}

    public SpamSmtpTarpitEvent(SessionEvent sessionEvent, String hostname, IPAddress ipAddr, String vendorName)
    {
        this.sessionEvent = sessionEvent;
        this.hostname = hostname;
        this.ipAddr = ipAddr;
        this.vendorName = vendorName;
    }

    public SpamSmtpTarpitEvent(SessionEvent sessionEvent, String hostname, InetAddress ipAddrIN, String vendorName)
    {
        this.sessionEvent = sessionEvent;
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

    @Column(name="session_id", nullable=false)
    public Long getSessionId()
    {
        return sessionEvent.getSessionId();
    }

    public void setSessionId( Long sessionId )
    {
        this.sessionEvent.setSessionId(sessionId);
    }

    @Transient
    public SessionEvent getSessionEvent()
    {
        return sessionEvent;
    }

    public void setSessionEvent(SessionEvent sessionEvent)
    {
        this.sessionEvent = sessionEvent;
    }

    // Syslog methods ---------------------------------------------------------

    @Transient
    public void appendSyslog(SyslogBuilder sb)
    {
        // No longer log pipeline endpoints, they are not necessary anyway.
        // SessionEvent pe = getSessionEvent();
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
