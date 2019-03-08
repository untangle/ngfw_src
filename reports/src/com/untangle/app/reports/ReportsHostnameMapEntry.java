/**
 * $Id$
 */
package com.untangle.app.reports;

import java.io.Serializable;

import org.json.JSONObject;
import org.json.JSONString;

import com.untangle.uvm.app.IPMaskedAddress;

/**
 * Mapping of hostname to IP address.
 */
@SuppressWarnings("serial")
public class ReportsHostnameMapEntry implements Serializable, JSONString
{
    private IPMaskedAddress address;
    private String hostname;

    public ReportsHostnameMapEntry() {}

    public IPMaskedAddress getAddress() { return this.address; }
    public void setAddress( IPMaskedAddress address ) { this.address = address; }

    public String getHostname() { return this.hostname; }
    public void setHostname( String hostname ) { this.hostname = hostname; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
