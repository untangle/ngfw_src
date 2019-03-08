/**
 * $Id: DeviceTableEvent.java 40333 2015-05-20 06:32:20Z dmorris $
 */
package com.untangle.uvm;

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
    private String oldValue;

    /**
     * this is stored only so it will appear in the JSON serialiazed string in this event
     */    
    private DeviceTableEntry device;
    
    public DeviceTableEvent() { }

    public DeviceTableEvent( DeviceTableEntry device, String macAddress, String key, String value, String oldValue )
    {
        this.device = device;
        this.macAddress = macAddress;
        this.key = key;
        this.value = value;
        this.oldValue = oldValue;
    }

    public String getMacAddress() { return macAddress; }
    public void setMacAddress( String newValue ) { this.macAddress = newValue; }

    public String getKey() { return key; }
    public void setKey( String newValue ) { this.key = newValue; }

    public String getValue() { return value; }
    public void setValue( String newValue ) { this.value = newValue; }

    public DeviceTableEntry getDevice() { return device; }
    public void setDevice( DeviceTableEntry newValue ) { this.device = newValue; }

    public String getOldValue() { return oldValue; }
    public void setOldValue( String newValue ) { this.oldValue = newValue; }
    
    @Override
    public void compileStatements( java.sql.Connection conn, java.util.Map<String,java.sql.PreparedStatement> statementCache ) throws Exception
    {
        String sql = "INSERT INTO " + schemaPrefix() + "device_table_updates" + getPartitionTablePostfix() + " " +
            "(time_stamp, mac_address, key, value, old_value) " +
            "values " +
            "(?, ?, ?, ?, ?); ";

        java.sql.PreparedStatement pstmt = getStatementFromCache( sql, statementCache, conn );        

        int i = 0;
        pstmt.setTimestamp(++i,getTimeStamp());
        pstmt.setString(++i, getMacAddress());
        pstmt.setString(++i, getKey());
        pstmt.setString(++i, getValue());
        pstmt.setString(++i, getOldValue());

        pstmt.addBatch();
        return;
    }

    @Override
    public String toSummaryString()
    {
        String maker = ( device == null ? "" : "(" + device.getMacVendor() + ")");
        switch (key) {
        case "add":
            return I18nUtil.marktr("Device Table Update:") + " " + key + " " + macAddress + " " + maker;
        default:
            return I18nUtil.marktr("Device Table Update:") + " " + key + " -> \"" + value + "\" " + macAddress + " " + maker;
        }
    }
    
}
