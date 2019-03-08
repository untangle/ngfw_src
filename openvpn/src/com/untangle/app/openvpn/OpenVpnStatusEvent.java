/**
 * $Id$
 */

package com.untangle.app.openvpn;

import java.io.Serializable;
import java.sql.Timestamp;
import java.net.InetAddress;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.util.I18nUtil;

/**
 * OpenVPN client status event
 * 
 * @author mahotz
 * 
 */
@SuppressWarnings("serial")
public class OpenVpnStatusEvent extends LogEvent implements Serializable, org.json.JSONString
{
    private InetAddress address;
    private InetAddress poolAddress;
    private int port;
    private String clientName;
    private Timestamp start; /* Start of the session */
    private Timestamp end; /* End of the session */
    private long bytesRxTotal; /* Total bytes received */
    private long bytesTxTotal; /* Total bytes transmitted */
    private long bytesRxDelta; /* Delta bytes transmitted since last event */
    private long bytesTxDelta; /* Delta bytes transmitted since last event */

    public OpenVpnStatusEvent()
    {
    }

    public OpenVpnStatusEvent(Timestamp start, InetAddress address, int port, InetAddress poolAddress, String clientName)
    {
        this.start = start;
        this.address = address;
        this.poolAddress = poolAddress;
        this.clientName = clientName;
        this.port = port;
    }

    public OpenVpnStatusEvent(Timestamp start, InetAddress address, int port, InetAddress poolAddress, String clientName, long bytesRxTotal, long bytesTxTotal, long bytesRxDelta, long bytesTxDelta)
    {
        this.start = start;
        this.address = address;
        this.poolAddress = poolAddress;
        this.clientName = clientName;
        this.port = port;
        this.bytesRxTotal = bytesRxTotal;
        this.bytesTxTotal = bytesTxTotal;
        this.bytesRxDelta = bytesRxDelta;
        this.bytesTxDelta = bytesTxDelta;
    }

    public OpenVpnStatusEvent(OpenVpnStatusEvent oldEvent)
    {
        this.address = oldEvent.address;
        this.poolAddress = oldEvent.poolAddress;
        this.port = oldEvent.port;
        this.clientName = oldEvent.clientName;
        this.start = oldEvent.start;
        this.end = oldEvent.end;
        this.bytesRxTotal = oldEvent.bytesRxTotal;
        this.bytesTxTotal = oldEvent.bytesTxTotal;
        this.bytesRxDelta = oldEvent.bytesRxDelta;
        this.bytesTxDelta = oldEvent.bytesTxDelta;
    }

// THIS IS FOR ECLIPSE - @formatter:off

    public InetAddress getAddress() { return this.address; }
    public void setAddress( InetAddress newValue ) { this.address = newValue; }

    public int getPort() { return this.port; }
    public void setPort( int port ) { this.port = port; }

    public InetAddress getPoolAddress() { return this.poolAddress; }
    public void setPoolAddress( InetAddress newValue ) { this.poolAddress = newValue; }
    
    public String getClientName() { return this.clientName; }
    public void setClientName( String newValue ) { this.clientName = newValue; }

    public Timestamp getStart() { return this.start; }
    public void setStart( Timestamp newValue ) { this.start = newValue; }

    public Timestamp getEnd() { return this.end; }
    public void setEnd( Timestamp newValue ) { this.end = newValue; }

    public long getBytesRxTotal() { return this.bytesRxTotal; }
    public void setBytesRxTotal( long newValue ) { this.bytesRxTotal = newValue; }

    public long getBytesTxTotal() { return this.bytesTxTotal; }
    public void setBytesTxTotal( long newValue ) { this.bytesTxTotal = newValue; }

    public long getBytesRxDelta() { return this.bytesRxDelta; }
    public void setBytesRxDelta( long newValue ) { this.bytesRxDelta = newValue; }

    public long getBytesTxDelta() { return this.bytesTxDelta; }
    public void setBytesTxDelta( long newValue ) { this.bytesTxDelta = newValue; }

    @Override
    public void compileStatements( java.sql.Connection conn, java.util.Map<String,java.sql.PreparedStatement> statementCache ) throws Exception
    {
        String sql = "INSERT INTO " + schemaPrefix() + "openvpn_stats" + getPartitionTablePostfix() + " " +
        "(time_stamp, start_time, end_time, rx_bytes, tx_bytes, remote_address, remote_port, pool_address, client_name) " +
        "values " +
        "( ?, ?, ?, ?, ?, ?, ?, ?, ? ) ";

        java.sql.PreparedStatement pstmt = getStatementFromCache( sql, statementCache, conn );        

        int i=0;
        pstmt.setTimestamp(++i,getTimeStamp());
        pstmt.setTimestamp(++i,getStart());
        pstmt.setTimestamp(++i,getEnd());
        pstmt.setLong(++i, getBytesRxDelta());
        pstmt.setLong(++i, getBytesTxDelta());
        pstmt.setObject(++i, getAddress().getHostAddress(), java.sql.Types.OTHER);
        pstmt.setInt(++i, getPort());
        pstmt.setObject(++i, getPoolAddress().getHostAddress(), java.sql.Types.OTHER);
        pstmt.setString(++i, getClientName());

        pstmt.addBatch();
        return;
    }

// THIS IS FOR ECLIPSE - @formatter:on

    @Override
    public String toSummaryString()
    {
        String summary = "OpenVPN" + " " + I18nUtil.marktr("status") + ": " + getClientName() + " rx:" + getBytesRxTotal() + " tx: " + getBytesTxTotal();
        return summary;
    }

    public String toJSONString()
    {
        org.json.JSONObject jO = new org.json.JSONObject(this);
        return jO.toString();
    }
}
