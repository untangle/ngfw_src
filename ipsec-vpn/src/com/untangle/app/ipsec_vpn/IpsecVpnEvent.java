/**
 * $Id: IpsecVpnEvent.java 39739 2015-02-26 20:45:55Z dmorris $
 */

package com.untangle.app.ipsec_vpn;

import java.io.Serializable;
import java.sql.Timestamp;
import java.net.InetAddress;

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
public class IpsecVpnEvent extends LogEvent implements Serializable
{
    public enum EventType
    {
        CONNECT, DISCONNECT, UNREACHABLE
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

    /**
     * Address of the remote server
     */
    public String getRemoteAddress()
    {
        return this.remoteAddress;
    }

    public void setRemoteAddress(String newValue)
    {
        this.remoteAddress = newValue;
    }

    /**
     * Address of the local server
     */
    public String getLocalAddress()
    {
        return this.localAddress;
    }

    public void setLocallAddress(String newValue)
    {
        this.localAddress = newValue;
    }

    /**
     * Description of the tunnel for which the event was recorded
     */
    public String getTunnelDescription()
    {
        return this.tunnelDescription;
    }

    public void setTunnelDescription(String newValue)
    {
        this.tunnelDescription = newValue;
    }

    /**
     * Type of event
     */
    public EventType getEventType()
    {
        return this.type;
    }

    public void setEventType(EventType newValue)
    {
        this.type = newValue;
    }

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
