/**
 * $Id$
*/

package com.untangle.app.wireguard_vpn;

import java.io.Serializable;
import java.net.InetAddress;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.util.ValidSerializable;

/**
 * WireGuardVpn tunnel connection event
 */
@SuppressWarnings("serial")
@ValidSerializable
public class WireGuardVpnEvent extends LogEvent implements Serializable, org.json.JSONString
{
    public enum EventType
    {
        CONNECT, DISCONNECT, UNREACHABLE
    };

    private String tunnelName;
    private EventType type;

// THIS IS FOR ECLIPSE - @formatter:off

    public WireGuardVpnEvent() {}

    public WireGuardVpnEvent( String tunnelName, EventType type )
    {
        this.tunnelName    = tunnelName;
        this.type          = type;
    }
    
    public String getTunnelName() { return this.tunnelName; }
    public void setTunnelName( String newValue ) { this.tunnelName = newValue; }

    public EventType getEventType() { return this.type; }
    public void setEventType( EventType newValue ) { this.type = newValue; }
    
    @Override
    public void compileStatements( java.sql.Connection conn, java.util.Map<String,java.sql.PreparedStatement> statementCache ) throws Exception
    {
        String sql = "INSERT INTO " + schemaPrefix() + "wireguard_vpn_events" + getPartitionTablePostfix() + " " +
        "(time_stamp, tunnel_name, event_type) " +
        "values " +
        "( ?, ?, ? ) ";

        java.sql.PreparedStatement pstmt = getStatementFromCache( sql, statementCache, conn );

        int i=0;
        pstmt.setTimestamp(++i,getSqlTimeStamp());
        pstmt.setString(++i, getTunnelName());
        pstmt.setString(++i, getEventType().toString());

        pstmt.addBatch();
        return;
    }

// THIS IS FOR ECLIPSE - @formatter:on

    @Override
    public String toSummaryString()
    {
        String summary = "WireGuardVpn" + " " + getEventType() + " " + I18nUtil.marktr("event") + ": " + getTunnelName();
        return summary;
    }

    public String toJSONString()
    {
        org.json.JSONObject jO = new org.json.JSONObject(this);
        return jO.toString();
    }
}
