/**
 * $Id: SessionEvent.java 33103 2012-09-25 23:46:26Z dmorris $
 */
package com.untangle.uvm.node;

import java.net.InetAddress;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.node.SessionTuple;

/**
 * Used to record the Session endpoints at session end time.
 * SessionStatsEvent and SessionEvent used to be the PiplineInfo
 * object.
 */
@SuppressWarnings("serial")
public class HostTableEvent extends LogEvent
{
    private InetAddress address;
    private String key;
    private String value;
    
    public HostTableEvent()
    {
        super();
    }

    public HostTableEvent( InetAddress address, String key, String value)
    {
        super();
        this.address = address;
        this.key = key;
        this.value = value;
    }

    public InetAddress getAddress() { return address; }
    public void setAddress( InetAddress address ) { this.address = address; }

    public String getKey() { return key; }
    public void setKey( String key ) { this.key = key; }

    public String getValue() { return value; }
    public void setValue( String value ) { this.value = value; }
    
    private static String sql = "INSERT INTO reports.host_table_updates " +
        "(time_stamp, address, key, value) " +
        "values " +
        "(?, ?, ?, ?); ";

    @Override
    public java.sql.PreparedStatement getDirectEventSql( java.sql.Connection conn ) throws Exception
    {
        java.sql.PreparedStatement pstmt = conn.prepareStatement( sql );

        int i=0;
        pstmt.setTimestamp(++i,getTimeStamp());
        pstmt.setObject(++i, getAddress().getHostAddress(), java.sql.Types.OTHER);
        pstmt.setString(++i, getKey());
        pstmt.setString(++i, getValue());

        return pstmt;
    }
    
    public String toString()
    {
        String address = (getAddress() != null ? getAddress().getHostAddress() : "null");
        
        return "HostTableEvent: [" + address + "] " + getKey() + " -> " + getValue();
    }
}
