/* $HeadURL$ */
package com.untangle.uvm.networking;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.json.JSONString;
import org.json.JSONObject;
import org.apache.log4j.Logger;

import com.untangle.uvm.node.IPAddress;

/**
 * This object represents the current configuration of all network settings
 */
@SuppressWarnings("serial")
public class NetworkSettings implements java.io.Serializable, JSONString
{
    private final Logger logger = Logger.getLogger(getClass());

    private List<InterfaceSettings> interfaceList = null;
    private Boolean dhcpServerEnabled = null;
    private Boolean dnsServerEnabled = null;

    private String hostname = null;
    private String dnsLocalDomain = null;

    public NetworkSettings() {}
    
    public NetworkSettings( List<InterfaceSettings> interfaceList, Boolean dhcpServerEnabled, Boolean dnsServerEnabled, String hostname )
    {
        this.interfaceList     = Collections.unmodifiableList( new LinkedList<InterfaceSettings>( interfaceList ));
        this.dhcpServerEnabled = dhcpServerEnabled;
        this.dnsServerEnabled = dnsServerEnabled;
        this.hostname = hostname;
    }
    
    public List<InterfaceSettings> getInterfaceList()
    {
        return this.interfaceList;
    }

    public void setInterfaceList( List<InterfaceSettings> interfaceList )
    {
        this.interfaceList = interfaceList;
    }

    public Boolean getDhcpServerEnabled()
    {
        return this.dhcpServerEnabled;
    }

    public void setDhcpServerEnabled( Boolean dhcpServerEnabled )
    {
        this.dhcpServerEnabled = dhcpServerEnabled;
    }
    
    public Boolean getDnsServerEnabled()
    {
        return this.dnsServerEnabled;
    }

    public void setDnsServerEnabled( Boolean isDnsServerEnabled )
    {
        this.dnsServerEnabled = dnsServerEnabled;
    }
    
    public String getHostname()
    {
        return this.hostname;
    }

    public void setHostname( String hostname )
    {
        this.hostname = hostname;
    }

    public String getDnsLocalDomain()
    {
        return this.dnsLocalDomain;
    }

    public void setDnsLocalDomain( String dnsLocalDomain )
    {
        this.dnsLocalDomain = dnsLocalDomain;
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "Network Settings\n" );
        
        sb.append( "\nInterfaces:\n" );
        for ( InterfaceSettings intf : getInterfaceList()) sb.append( intf + "\n" );
        
        return sb.toString();
    }

    public InterfaceSettings findFirstWAN()
    {
        if (interfaceList == null)
            return null;
        
        for (InterfaceSettings intf : interfaceList) {
            if (intf.isWAN())
                return intf;
        }
        
        return null;
    }

    public InterfaceSettings findSystemName(String systemName)
    {
        if (interfaceList == null)
            return null;
        if (systemName == null) {
            logger.error("Invalid systemName: null");
            return null;
        }
        
        for (InterfaceSettings intf : interfaceList) {
            if (systemName.equals(intf.getSystemName()))
                return intf;
        }

        return null;
    }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}

