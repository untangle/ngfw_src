/**
 * $Id$
 */
package com.untangle.uvm.network.generic;

import java.io.Serializable;

import org.json.JSONObject;
import org.json.JSONString;

/**
 * Arp Entry.
 */
public class ArpGeneric implements Serializable, JSONString {

    private String address;
    private String macAddress;
    private String vendor;

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getMacAddress() { return macAddress; }
    public void setMacAddress(String macAddress) { this.macAddress = macAddress; }

    public String getVendor() { return vendor; }
    public void setVendor(String vendor) { this.vendor = vendor; }

    public String toJSONString() {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

}
