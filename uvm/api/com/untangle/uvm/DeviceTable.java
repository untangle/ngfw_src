/**
 * $Id: DeviceTable.java 42009 2015-12-29 19:15:19Z dmorris $
 */
package com.untangle.uvm;

import java.util.Map;
import java.util.LinkedList;

/**
 * The Device Table is responsible for storing known information about devices
 */
public interface DeviceTable
{
    public DeviceTableEntry addDevice( String macAddress );

    public Map<String, DeviceTableEntry> getDeviceTable();

    public LinkedList<DeviceTableEntry> getDevices();

    public DeviceTableEntry getDevice( String macAddress );

    public String lookupMacVendor( String macAddress );

    public void saveDevices();
}

