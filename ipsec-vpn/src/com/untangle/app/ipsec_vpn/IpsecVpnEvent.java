/**
 * $Id: IpsecVpnEvent.java 39739 2015-02-26 20:45:55Z dmorris $
 */

package com.untangle.app.ipsec_vpn;

import java.io.Serializable;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.util.I18nUtil;

/**
 * This class defines the event record used to record IPsec connection status
 * events to the database.
 * 
 * @author mahotz
 * 
 */

@SuppressWarnings("serial")
public class IpsecVpnEvent extends LogEvent implements Serializable, org.json.JSONString
{
    public enum EventType
    {
        CONNECT, DISCONNECT, UNREACHABLE, RESTART
    };

    private String localAddress;
    private String remoteAddress;
    private String tunnelDescription;
    private EventType type;

    public IpsecVpnEvent()
    {
    }

    public IpsecVpnEvent(String localAddress, String remoteAddress, String tunnelDescription, EventType type)
    {
        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
        this.tunnelDescription = tunnelDescription;
        this.type = type;
    }

    // THIS IS FOR ECLIPSE - @formatter:off
    
    public String getRemoteAddress() { return this.remoteAddress; }
    public void setRemoteAddress(String newValue) { this.remoteAddress = newValue; }

    public String getLocalAddress() { return this.localAddress; }
    public void setLocallAddress(String newValue) { this.localAddress = newValue; }

    public String getTunnelDescription() { return this.tunnelDescription; }
    public void setTunnelDescription(String newValue) { this.tunnelDescription = newValue; }

    public EventType getEventType() { return this.type; }
    public void setEventType(EventType newValue) { this.type = newValue; }

    // THIS IS FOR ECLIPSE - @formatter:on

    @Override
    public void compileStatements(java.sql.Connection conn, java.util.Map<String, java.sql.PreparedStatement> statementCache) throws Exception
    {
        String sql = "INSERT INTO " + schemaPrefix() + "ipsec_vpn_events" + getPartitionTablePostfix() + " " + "(time_stamp, local_address, remote_address, tunnel_description, event_type) " + "values " + "( ?, ?, ?, ?, ? ) ";

        java.sql.PreparedStatement pstmt = getStatementFromCache(sql, statementCache, conn);

        int i = 0;
        pstmt.setTimestamp(++i, getTimeStamp());
        pstmt.setString(++i, localAddress);
        pstmt.setString(++i, remoteAddress);
        pstmt.setString(++i, getTunnelDescription());
        pstmt.setString(++i, getEventType().toString());

        pstmt.addBatch();
        return;
    }

    @Override
    public String toSummaryString()
    {
        String summary = "IPsec " + " " + getEventType() + " " + I18nUtil.marktr("event") + ": " + getTunnelDescription();
        return summary;
    }
}
