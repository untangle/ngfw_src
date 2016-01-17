/**
 * $Id: DeviceTableEvent.java 40333 2015-05-20 06:32:20Z dmorris $
 */
package com.untangle.uvm;

import java.net.InetAddress;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.util.I18nUtil;

/**
 * Log event for an update to the host table
 */
@SuppressWarnings("serial")
public class DeviceTableEvent extends LogEvent
{
    private String macAddress;
    private String key;
    private String value;
    
    public DeviceTableEvent() { }

    public DeviceTableEvent( String macAddress, String key, String value )
    {
        this.macAddress = macAddress;
        this.key = key;
        this.value = value;
    }

    public String getMacAddress() { return macAddress; }
    public void setMacAddress( String newValue ) { this.macAddress = newValue; }

    public String getKey() { return key; }
    public void setKey( String newValue ) { this.key = newValue; }

    public String getValue() { return value; }
    public void setValue( String newValue ) { this.value = newValue; }
    
    @Override
    public void compileStatements( java.sql.Connection conn, java.util.Map<String,java.sql.PreparedStatement> statementCache ) throws Exception
    {
        String sql = "INSERT INTO reports.device_table_updates" + getPartitionTablePostfix() + " " +
            "(time_stamp, mac_address, key, value) " +
            "values " +
            "(?, ?, ?, ?); ";

        java.sql.PreparedStatement pstmt = getStatementFromCache( sql, statementCache, conn );        

        int i = 0;
        pstmt.setTimestamp(++i,getTimeStamp());
        pstmt.setString(++i, getMacAddress());
        pstmt.setString(++i, getKey());
        pstmt.setString(++i, getValue());

        pstmt.addBatch();
        return;
    }

    @Override
    public String toSummaryString()
    {
        return I18nUtil.marktr("Device Table Update") + " " + "[" + macAddress + "] " + key + " -> " + value;
    }
    
}
