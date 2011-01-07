/* $HeadURL: svn://chef/branch/prod/release-8.0/work/src/uvm-lib/api/com/untangle/uvm/networking/DhcpLeaseRule.java $ */
package com.untangle.uvm.networking;

import java.util.Date;

import com.untangle.uvm.node.firewall.MACAddress;
import com.untangle.uvm.node.IPaddr;

public class DhcpLease 
{
    /* The MAC address associated with this lease */
    private MACAddress macAddress;

    /* The address of this lease */
    private IPaddr address;

    
    public MACAddress getMacAddress()
    {
        return macAddress;
    }

    public void setMacAddress( MACAddress macAddress )
    {
        this.macAddress = macAddress;
    }

    public IPaddr getAddress()
    {
        return this.address;
    }

    public void setAddress( IPaddr address )
    {
        this.address = address;
    }
}
