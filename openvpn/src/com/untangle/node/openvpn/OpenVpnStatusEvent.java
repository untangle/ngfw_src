/**
 * $Id: OpenVpnStatusEvent.java 30552 2011-12-21 23:59:19Z dmorris $
 */
package com.untangle.node.openvpn;

import java.io.Serializable;
import java.sql.Timestamp;
import java.net.InetAddress;

import com.untangle.uvm.logging.LogEvent;

/**
 * OpenVPN client status event
 */
@SuppressWarnings("serial")
public class OpenVpnStatusEvent extends LogEvent implements Serializable
{
    private InetAddress address;
    private int port;
    private String clientName;
    private Timestamp start; /* Start of the session */
    private Timestamp end; /* End of the session */
    private long bytesRxTotal; /* Total bytes received */
    private long bytesTxTotal; /* Total bytes transmitted */
    private long bytesRxDelta; /* Delta bytes transmitted since last event */
    private long bytesTxDelta; /* Delta bytes transmitted since last event */

    public OpenVpnStatusEvent() {}

    public OpenVpnStatusEvent( Timestamp start, InetAddress address, int port, String clientName )
    {
        this.start      = start;
        this.address    = address;
        this.clientName = clientName;
        this.port       = port;
    }

    public OpenVpnStatusEvent( OpenVpnStatusEvent oldEvent )
    {
        this.address = oldEvent.address;
        this.port = oldEvent.port;
        this.clientName = oldEvent.clientName;
        this.start = oldEvent.start;
        this.end = oldEvent.end;
        this.bytesRxTotal = oldEvent.bytesRxTotal;
        this.bytesTxTotal = oldEvent.bytesTxTotal;
        this.bytesRxDelta = oldEvent.bytesRxDelta;
        this.bytesTxDelta = oldEvent.bytesTxDelta;
    }
    
    /**
     * Address where the client connected from.
     */
    public InetAddress getAddress() { return this.address; }
    public void setAddress( InetAddress address ) { this.address = address; }

    /**
     * Port used to connect
     */
    public int getPort() { return this.port; }
    public void setPort( int port ) { this.port = port; }

    /**
     * Name of the client that was connected.
     */
    public String getClientName() { return this.clientName; }
    public void setClientName( String clientName ) { this.clientName = clientName; }

    /**
     * Time the session started.
     */
    public Timestamp getStart() { return this.start; }
    public void setStart( Timestamp start ) { this.start = start; }

    /**
     * Time the session ended. <b>Note that this
     * may be null if the session is still open</b>
     */
    public Timestamp getEnd() { return this.end; }
    public void setEnd( Timestamp end ) { this.end = end; }

    /**
     * Total bytes received during this session.
     */
    public long getBytesRxTotal() { return this.bytesRxTotal; }
    public void setBytesRxTotal( long bytesRxTotal ) { this.bytesRxTotal = bytesRxTotal; }

    /**
     * Total transmitted received during this session.
     */
    public long getBytesTxTotal() { return this.bytesTxTotal; }
    public void setBytesTxTotal( long bytesTxTotal ) { this.bytesTxTotal = bytesTxTotal; }

    /**
     * Delta bytes received since last event
     */
    public long getBytesRxDelta() { return this.bytesRxDelta; }
    public void setBytesRxDelta( long bytesRxDelta ) { this.bytesRxDelta = bytesRxDelta; }

    /**
     * Delta bytes transmitted since last event
     */
    public long getBytesTxDelta() { return this.bytesTxDelta; }
    public void setBytesTxDelta( long bytesTxDelta ) { this.bytesTxDelta = bytesTxDelta; }
    
    private static String sql = "INSERT INTO reports.openvpn_stats " +
        "(time_stamp, start_time, end_time, rx_bytes, tx_bytes, remote_address, remote_port, client_name) " +
        "values " +
        "( ?, ?, ?, ?, ?, ?, ?, ? ) ";

    @Override
    public java.sql.PreparedStatement getDirectEventSql( java.sql.Connection conn ) throws Exception
    {
        java.sql.PreparedStatement pstmt = conn.prepareStatement( sql );

        int i=0;
        pstmt.setTimestamp(++i,getTimeStamp());
        pstmt.setTimestamp(++i,getStart());
        pstmt.setTimestamp(++i,getEnd());
        pstmt.setLong(++i, getBytesRxDelta());
        pstmt.setLong(++i, getBytesTxDelta());
        pstmt.setObject(++i, getAddress().getHostAddress(), java.sql.Types.OTHER);
        pstmt.setInt(++i, getPort());
        pstmt.setString(++i, getClientName());

        return pstmt;
    }
}
