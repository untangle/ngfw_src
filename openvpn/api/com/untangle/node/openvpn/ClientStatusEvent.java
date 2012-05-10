/**
 * $Id: ClientStatusEvent.java 30552 2011-12-21 23:59:19Z dmorris $
 */
package com.untangle.node.openvpn;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.node.IPAddress;

/**
 * OpenVPN client status event
 */
@SuppressWarnings("serial")
public class ClientStatusEvent extends LogEvent implements Serializable
{
    private IPAddress address;
    private int port;
    private String clientName;
    private Date start; /* Start of the session */
    private Date end; /* End of the session */
    private long bytesRxTotal; /* Total bytes received */
    private long bytesTxTotal; /* Total bytes transmitted */
    private long bytesRxDelta; /* Delta bytes transmitted since last event */
    private long bytesTxDelta; /* Delta bytes transmitted since last event */

    public ClientStatusEvent() {}

    public ClientStatusEvent( Date start, IPAddress address, int port, String clientName )
    {
        this.start      = start;
        this.address    = address;
        this.clientName = clientName;
        this.port       = port;
    }

    public ClientStatusEvent( ClientStatusEvent oldEvent )
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
    public IPAddress getAddress() { return this.address; }
    public void setAddress( IPAddress address ) { this.address = address; }

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
    public Date getStart() { return this.start; }
    public void setStart( Date start )
    {
        if( start instanceof Timestamp )
            this.start = new Date(start.getTime());
        else
            this.start = start;
    }

    /**
     * Time the session ended. <b>Note that this
     * may be null if the session is still open</b>
     */
    public Date getEnd() { return this.end; }
    public void setEnd( Date end )
    {
        if( end instanceof Timestamp ) {
            this.end = new Date(end.getTime());
        } else {
            this.end = end;
        }
    }

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
    
    @Override
    public String getDirectEventSql()
    {
        String sql = "INSERT INTO reports.n_openvpn_stats " +
            "(time_stamp, start_time, end_time, rx_bytes, tx_bytes, remote_address, remote_port, client_name) " +
            "values " +
            "( " +
            "timestamp '" + new java.sql.Timestamp(getTimeStamp().getTime()) + "'" + "," +
            "timestamp '" + new java.sql.Timestamp(getStart().getTime()) + "'" + "," +
            "timestamp '" + new java.sql.Timestamp(getEnd().getTime()) + "'" + "," +
            "'" + getBytesRxDelta() + "'" + "," +
            "'" + getBytesTxDelta() + "'" + "," +
            "'" + getAddress() + "'" + "," +
            "'" + getPort() + "'" + "," +
            "'" + getClientName() + "'" + ")" +
            ";";
            return sql;
    }
}
