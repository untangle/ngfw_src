/**
 * $Id$
 */
package com.untangle.node.spam;

import java.net.InetAddress;

import com.untangle.uvm.logging.LogEvent;
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
    private InetAddress ipAddr;
    private String vendorName;

    // constructors -----------------------------------------------------------

    public SpamSmtpTarpitEvent() {}

    public SpamSmtpTarpitEvent(SessionEvent sessionEvent, String hostname, InetAddress ipAddr, String vendorName)
    {
        this.sessionEvent = sessionEvent;
        this.hostname = hostname;
        this.ipAddr = ipAddr;
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
    public InetAddress getIPAddr() { return ipAddr; }
    public void setIPAddr( InetAddress ipAddr ) { this.ipAddr = ipAddr; }

    /**
     * Spam scanner vendor.
     */
    public String getVendorName() { return vendorName; }
    public void setVendorName( String vendorName ) { this.vendorName = vendorName; }


    public Long getSessionId() { return sessionEvent.getSessionId(); }
    public void setSessionId( Long sessionId ) { this.sessionEvent.setSessionId(sessionId); }

    public SessionEvent getSessionEvent() { return sessionEvent; }
    public void setSessionEvent( SessionEvent sessionEvent ) { this.sessionEvent = sessionEvent; }

    private static String sql = "INSERT INTO reports.spam_smtp_tarpit_events " +
        "(time_stamp, ipaddr, hostname, vendor_name, policy_id) " +
        "values " +
        "( ?, ?, ?, ?, ? ) ";

    @Override
    public java.sql.PreparedStatement getDirectEventSql( java.sql.Connection conn ) throws Exception
    {
        java.sql.PreparedStatement pstmt = conn.prepareStatement( sql );

        int i=0;
        pstmt.setTimestamp(++i,getTimeStamp());
        pstmt.setObject(++i, getIPAddr().getHostAddress(), java.sql.Types.OTHER);
        pstmt.setString(++i, getHostname());
        pstmt.setString(++i, getVendorName());
        pstmt.setLong(++i, sessionEvent.getPolicyId());

        return pstmt;
    }
}
