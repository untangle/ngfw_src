/**
 * $Id: ReportingHostnameMapEntry.java,v 1.00 2012/05/16 14:43:52 dmorris Exp $
 */
package com.untangle.node.reporting;

import java.io.Serializable;

import com.untangle.uvm.node.IPMaskedAddress;

@SuppressWarnings("serial")
public class ReportingHostnameMapEntry implements Serializable
{
    private IPMaskedAddress address;
    private String hostname;

    public ReportingHostnameMapEntry() {}

    public IPMaskedAddress getAddress() { return this.address; }
    public void setAddress( IPMaskedAddress address ) { this.address = address; }

    public String getHostname() { return this.hostname; }
    public void setHostname( String hostname ) { this.hostname = hostname; }
}
