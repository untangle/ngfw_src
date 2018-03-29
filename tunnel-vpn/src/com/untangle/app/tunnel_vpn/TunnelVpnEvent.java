/**
 * $Id: TunnelVpnEvent.java 39739 2015-02-26 20:45:55Z dmorris $
 */

package com.untangle.app.tunnel_vpn;

import java.io.Serializable;
import java.net.InetAddress;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.util.I18nUtil;

/**
 * TunnelVPN tunnel connection event
 */
@SuppressWarnings("serial")
public class TunnelVpnEvent extends LogEvent implements Serializable, org.json.JSONString
{
    public enum EventType
    {
        CONNECT, DISCONNECT
    };

    private InetAddress serverAddress;
    private InetAddress localAddress;
    private String tunnelName;
    private EventType type;

// THIS IS FOR ECLIPSE - @formatter:off

    public TunnelVpnEvent() {}

    public TunnelVpnEvent( InetAddress serverAddress, InetAddress localAddress, String tunnelName, EventType type )
    {
        this.serverAddress = serverAddress;
        this.localAddress  = localAddress;
        this.tunnelName    = tunnelName;
        this.type          = type;
    }
    
    public InetAddress getServerAddress() { return this.serverAddress; }
    public void setServerAddress( InetAddress newValue ) { this.serverAddress = newValue; }

    public InetAddress getLocalAddress() { return this.localAddress; }
    public void setLocallAddress( InetAddress newValue ) { this.localAddress = newValue; }
    
    public String getTunnelName() { return this.tunnelName; }
    public void setTunnelName( String newValue ) { this.tunnelName = newValue; }

    public EventType getEventType() { return this.type; }
    public void setEventType( EventType newValue ) { this.type = newValue; }
    
    @Override
    public void compileStatements( java.sql.Connection conn, java.util.Map<String,java.sql.PreparedStatement> statementCache ) throws Exception
    {
        String sql = "INSERT INTO " + schemaPrefix() + "tunnel_vpn_events" + getPartitionTablePostfix() + " " +
        "(time_stamp, server_address, local_address, tunnel_name, event_type) " +
        "values " +
        "( ?, ?, ?, ?, ? ) ";

        java.sql.PreparedStatement pstmt = getStatementFromCache( sql, statementCache, conn );

        String serverAddress = (getServerAddress() == null ? "unknown" : getServerAddress().getHostAddress().toString());
        String localAddress = (getLocalAddress() == null ? "unknown" : getLocalAddress().getHostAddress().toString());

        int i=0;
        pstmt.setTimestamp(++i,getTimeStamp());
        pstmt.setObject(++i, serverAddress, java.sql.Types.OTHER);
        pstmt.setObject(++i, localAddress, java.sql.Types.OTHER);
        pstmt.setString(++i, getTunnelName());
        pstmt.setString(++i, getEventType().toString());

        pstmt.addBatch();
        return;
    }

// THIS IS FOR ECLIPSE - @formatter:on

    @Override
    public String toSummaryString()
    {
        String summary = "TunnelVPN" + " " + getEventType() + " " + I18nUtil.marktr("event") + ": " + getTunnelName();
        return summary;
    }

    public String toJSONString()
    {
        org.json.JSONObject jO = new org.json.JSONObject(this);
        return jO.toString();
    }
}
