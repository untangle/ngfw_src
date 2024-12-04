/**
 * $Id: DeviceTable.java 42009 2015-12-29 19:15:19Z dmorris $
 */
package com.untangle.uvm;

import java.util.Map;
import java.util.LinkedList;

import org.json.JSONArray;

/**
 * The Device Table is responsible for storing known information about devices
 */
public interface DeviceTable
{
    public int size();

    public DeviceTableEntry addDevice( String macAddress );

    public Map<String, DeviceTableEntry> getDeviceTable();

    public DeviceTableEntry getDevice( String macAddress );
    
    public JSONArray lookupMacVendor( String macAddress );
}

