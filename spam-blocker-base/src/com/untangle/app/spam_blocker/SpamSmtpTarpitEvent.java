/**
 * $Id$
 */

package com.untangle.app.spam_blocker;

import java.net.InetAddress;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.app.SessionEvent;
import com.untangle.uvm.util.I18nUtil;

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
    private String testsString;

    public SpamSmtpTarpitEvent()
    {
    }

    public SpamSmtpTarpitEvent(SessionEvent sessionEvent, String hostname, InetAddress ipAddr, String vendorName)
    {
        this.sessionEvent = sessionEvent;
        this.hostname = hostname;
        this.ipAddr = ipAddr;
        this.vendorName = vendorName;
    }

    public String getHostname()
    {
        return hostname;
    }

    public void setHostname(String hostname)
    {
        this.hostname = hostname;
    }

    public InetAddress getIPAddr()
    {
        return ipAddr;
    }

    public void setIPAddr(InetAddress ipAddr)
    {
        this.ipAddr = ipAddr;
    }

    public String getVendorName()
    {
        return vendorName;
    }

    public void setVendorName(String vendorName)
    {
        this.vendorName = vendorName;
    }

    public Long getSessionId()
    {
        return sessionEvent.getSessionId();
    }

    public void setSessionId(Long sessionId)
    {
        this.sessionEvent.setSessionId(sessionId);
    }

    public SessionEvent getSessionEvent()
    {
        return sessionEvent;
    }

    public void setSessionEvent(SessionEvent sessionEvent)
    {
        this.sessionEvent = sessionEvent;
    }

    @Override
    public void compileStatements(java.sql.Connection conn, java.util.Map<String, java.sql.PreparedStatement> statementCache) throws Exception
    {
        String sql = "INSERT INTO " + schemaPrefix() + "smtp_tarpit_events" + getPartitionTablePostfix() + " " + "(time_stamp, ipaddr, hostname, vendor_name, policy_id) " + "values " + "( ?, ?, ?, ?, ? ) ";

        java.sql.PreparedStatement pstmt = getStatementFromCache(sql, statementCache, conn);

        int i = 0;
        pstmt.setTimestamp(++i, getTimeStamp());
        pstmt.setObject(++i, getIPAddr().getHostAddress(), java.sql.Types.OTHER);
        pstmt.setString(++i, getHostname());
        pstmt.setString(++i, getVendorName());
        pstmt.setLong(++i, sessionEvent.getPolicyId());

        pstmt.addBatch();
        return;
    }

    @Override
    public String toSummaryString()
    {
        String appName;
        switch (vendorName)
        {
        case "spamassassin":
            appName = "Spam Blocker Lite";
            break;
        case "spamblocker":
            appName = "Spam Blocker";
            break;
        default:
            appName = "Spam Blocker";
            break;
        }

        String summary = appName + " " + I18nUtil.marktr("tarpit") + " " + I18nUtil.marktr("blocked") + " " + sessionEvent.toSummaryString();
        return summary;
    }
}
