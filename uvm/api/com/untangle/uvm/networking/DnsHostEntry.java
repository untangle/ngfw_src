/* $HeadURL: svn://chef/branch/prod/release-8.0/work/src/uvm/api/com/untangle/uvm/networking/DnsHostEntry.java $ */
package com.untangle.uvm.networking;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

import com.untangle.uvm.node.HostNameList;
import com.untangle.uvm.node.IPAddress;
import com.untangle.uvm.node.Rule;

public class DnsHostEntry 
{
    /** The list of hostnames for this rule */
    private HostNameList hostNameList = HostNameList.getEmptyHostNameList();

    /** The IP address that all of these hostnames resolves to */
    private IPAddress staticAddress = null;

    public DnsHostEntry() { }

    public DnsHostEntry(HostNameList hostNameList, IPAddress staticAddress)
    {
        this.hostNameList  = hostNameList;
        this.staticAddress = staticAddress;
    }

    public HostNameList getHostNameList()
    {
        if ( hostNameList == null )
            hostNameList = HostNameList.getEmptyHostNameList();

        return hostNameList;
    }

    public void setHostNameList( HostNameList hostNameList )
    {
        this.hostNameList = hostNameList;
    }

    public IPAddress getAddress()
    {
        return this.staticAddress;
    }

    public void setAddress( IPAddress staticAddress )
    {
        this.staticAddress = staticAddress;
    }
}
