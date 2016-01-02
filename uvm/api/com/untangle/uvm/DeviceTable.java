/**
 * $Id: DeviceTable.java 42009 2015-12-29 19:15:19Z dmorris $
 */
package com.untangle.uvm;

import java.util.Map;

/**
 * The Device Table is responsible for storing known information about devices
 */
public interface DeviceTable
{
    public Map<String, DeviceTableEntry> getDeviceTable();

    public DeviceTableEntry getDevice( String macAddress );

    public void addDevice( String macAddress );
    
    public String lookupMacVendor( String macAddress );

    public void saveDevices();
}

