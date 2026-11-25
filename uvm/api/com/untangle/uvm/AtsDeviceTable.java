package com.untangle.uvm;

import org.json.JSONArray;

/**
 * Combined Safe and additional ATS operations.
 */
public interface AtsDeviceTable extends SafeDeviceTable
{
    /**
     * lookupMacVendor through the additional ATS contract.
     * @param macAddress The macAddress argument
     * @return The delegated result
     */
    JSONArray lookupMacVendor(String macAddress);
}
