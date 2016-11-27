/**
 * $Id: OpenVpnEvent.java 39739 2015-02-26 20:45:55Z dmorris $
 */
package com.untangle.node.openvpn;

import java.io.Serializable;
import java.sql.Timestamp;
import java.net.InetAddress;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.util.I18nUtil;

/**
 * OpenVPN client status event
 */
@SuppressWarnings("serial")
public class OpenVpnEvent extends LogEvent implements Serializable
{
    public enum EventType
    {
        CONNECT, DISCONNECT
    };

    private InetAddress address;
    private InetAddress poolAddress;
    private String clientName;
    private EventType type;

    public OpenVpnEvent() {}

    public OpenVpnEvent( InetAddress address, InetAddress poolAddress, String clientName, EventType type )
    {
        this.type = type;
        this.address     = address;
        this.poolAddress = poolAddress;
        this.clientName  = clientName;
    }
    
    /**
     * Address where the client connected from.
     */
    public InetAddress getAddress() { return this.address; }
    public void setAddress( InetAddress newValue ) { this.address = newValue; }

    /**
     * Address where the client connected from.
     */
    public InetAddress getPoolAddress() { return this.poolAddress; }
    public void setPoolAddress( InetAddress newValue ) { this.poolAddress = newValue; }
    
    /**
     * Name of the client that was connected.
     */
    public String getClientName() { return this.clientName; }
    public void setClientName( String newValue ) { this.clientName = newValue; }

    /**
     * Name of the client that was connected.
     */
    public EventType getType() { return this.type; }
    public void setType( EventType newValue ) { this.type = newValue; }
    
    @Override
    public void compileStatements( java.sql.Connection conn, java.util.Map<String,java.sql.PreparedStatement> statementCache ) throws Exception
    {
        String sql = "INSERT INTO " + getSchemaPrefix() + "openvpn_events" + getPartitionTablePostfix() + " " +
        "(time_stamp, remote_address, pool_address, client_name, type) " +
        "values " +
        "( ?, ?, ?, ?, ? ) ";

        java.sql.PreparedStatement pstmt = getStatementFromCache( sql, statementCache, conn );        

        int i=0;
        pstmt.setTimestamp(++i,getTimeStamp());
        pstmt.setObject(++i, getAddress().getHostAddress(), java.sql.Types.OTHER);
        pstmt.setObject(++i, getPoolAddress().getHostAddress(), java.sql.Types.OTHER);
        pstmt.setString(++i, getClientName());
        pstmt.setString(++i, getType().toString());

        pstmt.addBatch();
        return;
    }

    @Override
    public String toSummaryString()
    {
        String summary = "OpenVPN" + " " + getType() + " " + I18nUtil.marktr("event") + ": " + getClientName();
        return summary;
    }

}
