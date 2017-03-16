/**
 * $Id$
 */
package com.untangle.app.reports;

import java.io.Serializable;

import com.untangle.uvm.app.IPMaskedAddress;

@SuppressWarnings("serial")
public class ReportsHostnameMapEntry implements Serializable
{
    private IPMaskedAddress address;
    private String hostname;

    public ReportsHostnameMapEntry() {}

    public IPMaskedAddress getAddress() { return this.address; }
    public void setAddress( IPMaskedAddress address ) { this.address = address; }

    public String getHostname() { return this.hostname; }
    public void setHostname( String hostname ) { this.hostname = hostname; }
}
