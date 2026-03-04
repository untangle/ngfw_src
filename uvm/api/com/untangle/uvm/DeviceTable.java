/**
 * $Id: DeviceTable.java 42009 2015-12-29 19:15:19Z dmorris $
 */
package com.untangle.uvm;

import org.json.JSONArray;

/**
 * The Device Table is responsible for storing known information about devices
 */
public interface DeviceTable
{
    public int size();

    public DeviceTableEntry addDevice( String macAddress );

    public DevicesSettings getDevicesSettings();

    public void setDevicesSettings(DevicesSettings newSettings);

    public DeviceTableEntry getDevice( String macAddress );
    
    public JSONArray lookupMacVendor( String macAddress );
    
    String getMacVendorFromMacAddress(String macAddress);

    public void saveDevicesSettings(boolean lastSaveTimeCheck);
}

