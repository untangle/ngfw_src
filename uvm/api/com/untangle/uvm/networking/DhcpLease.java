/**
 * $Id: DhcpLease.java,v 1.00 2013/02/25 16:00:48 dmorris Exp $
 */
package com.untangle.uvm.networking;

import java.util.Date;
import java.net.InetAddress;

public class DhcpLease 
{
    /* The MAC address associated with this lease */
    private String macAddress;

    /* The address of this lease */
    private InetAddress address;

    
    public String getMacAddress()
    {
        return macAddress;
    }

    public void setMacAddress( String macAddress )
    {
        this.macAddress = macAddress;
    }

    public InetAddress getAddress()
    {
        return this.address;
    }

    public void setAddress( InetAddress address )
    {
        this.address = address;
    }
}
