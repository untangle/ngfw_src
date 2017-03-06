/**
 * $Id: HostTableEvent.java 40333 2015-05-20 06:32:20Z dmorris $
 */
package com.untangle.uvm;

import java.net.InetAddress;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.util.I18nUtil;

/**
 * Log event for an update to the host table
 */
@SuppressWarnings("serial")
public class HostTableEvent extends LogEvent
{
    private InetAddress address;
    private String key;
    private String value;
    private String oldValue;
    
    public HostTableEvent() { }

    public HostTableEvent( InetAddress address, String key, String value, String oldValue )
    {
        this.address = address;
        this.key = key;
        this.value = value;
        this.oldValue = oldValue;
    }

    public InetAddress getAddress() { return address; }
    public void setAddress( InetAddress newValue ) { this.address = newValue; }

    public String getKey() { return key; }
    public void setKey( String newValue ) { this.key = newValue; }

    public String getValue() { return value; }
    public void setValue( String newValue ) { this.value = newValue; }

    public String getOldValue() { return oldValue; }
    public void setOldValue( String newValue ) { this.oldValue = newValue; }
    
    @Override
    public void compileStatements( java.sql.Connection conn, java.util.Map<String,java.sql.PreparedStatement> statementCache ) throws Exception
    {
        String sql = "INSERT INTO " + schemaPrefix() + "host_table_updates" + getPartitionTablePostfix() + " " +
            "(time_stamp, address, key, value, old_value) " +
            "values " +
            "(?, ?, ?, ?, ?); ";

        java.sql.PreparedStatement pstmt = getStatementFromCache( sql, statementCache, conn );        

        int i = 0;
        pstmt.setTimestamp(++i,getTimeStamp());
        pstmt.setObject(++i, address.getHostAddress(), java.sql.Types.OTHER);
        pstmt.setString(++i, getKey());
        pstmt.setString(++i, getValue());
        pstmt.setString(++i, getOldValue());

        pstmt.addBatch();
        return;
    }

    @Override
    public String toSummaryString()
    {
        return I18nUtil.marktr("Host Table Update") + " " + "[" + address + "] " + key + " -> " + value;
    }
    
}
