/**
 * $Id$
 */
package com.untangle.node.spam;

import java.net.InetAddress;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.logging.SyslogBuilder;
import com.untangle.uvm.logging.SyslogPriority;
import com.untangle.uvm.node.IPAddress;
import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.node.SessionEvent;

/**
 * Log for Spam SMTP Tarpit events.
 */
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
     */
    public String getHostname() { return hostname; }
    public void setHostname( String hostname ) { this.hostname = hostname; }

    /**
     * IP address of mail server listed on DNSBL service.
     */
    public IPAddress getIPAddr() { return ipAddr; }
    public void setIPAddr( IPAddress ipAddr ) { this.ipAddr = ipAddr; }

    /**
     * Spam scanner vendor.
     */
    public String getVendorName() { return vendorName; }
    public void setVendorName( String vendorName ) { this.vendorName = vendorName; }


    public Long getSessionId() { return sessionEvent.getSessionId(); }
    public void setSessionId( Long sessionId ) { this.sessionEvent.setSessionId(sessionId); }

    public SessionEvent getSessionEvent() { return sessionEvent; }
    public void setSessionEvent( SessionEvent sessionEvent ) { this.sessionEvent = sessionEvent; }

    @Override
    public String getDirectEventSql()
    {
        String sql = "INSERT INTO reports.n_spam_smtp_tarpit_events " +
            "(time_stamp, ipaddr, hostname, vendor_name, policy_id) " +
            "values " +
            "( " +
            "timestamp '" + new java.sql.Timestamp(getTimeStamp().getTime()) + "'" + "," +
            "'" + getIPAddr() + "'" + "," +
            "'" + getHostname() + "'" + "," +
            "'" + getVendorName() + "'" + "," +
            "'" + sessionEvent.getPolicyId() + "'" + ")" +
            ";";
            return sql;
    }

    // Syslog methods ---------------------------------------------------------

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

    public String getSyslogId()
    {
        return "SMTP_TARPIT";
    }

    public SyslogPriority getSyslogPriority()
    {
        // INFORMATIONAL = statistics or normal operation
        // WARNING = traffic altered
        return SyslogPriority.WARNING; // traffic altered
    }
}
