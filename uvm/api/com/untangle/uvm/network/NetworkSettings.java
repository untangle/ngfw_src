/**
 * $Id: NetworkSettings.java 32043 2012-05-31 21:31:47Z dmorris $
 */
package com.untangle.uvm.network;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.net.InetAddress;

import org.json.JSONObject;
import org.json.JSONString;

import com.untangle.uvm.network.PortForwardRule;
import com.untangle.uvm.network.NatRule;
import com.untangle.uvm.network.BypassRule;
import com.untangle.uvm.network.StaticRoute;

/**
 * Network settings.
 */
@SuppressWarnings("serial")
public class NetworkSettings implements Serializable, JSONString
{
    private List<InterfaceSettings> interfaces = null;
    private List<PortForwardRule> portForwardRules = null;
    private List<NatRule> natRules = null;
    private List<BypassRule> bypassRules = null;
    private List<StaticRoute> staticRoutes = null;

    private String hostName;
    private String domainName;

    private Boolean dynamicDnsServiceEnabled;
    private String  dynamicDnsServiceName = null;
    private String  dynamicDnsServiceUsername = null;
    private String  dynamicDnsServicePassword = null;
    private String  dynamicDnsServiceHostnames = null;

    private Boolean enableSipNatHelper;
    private Boolean sendIcmpRedirects;

    public NetworkSettings() { }

    public List<InterfaceSettings> getInterfaces() { return this.interfaces; }
    public void setInterfaces( List<InterfaceSettings> interfaces ) { this.interfaces = interfaces; }
    
    public List<PortForwardRule> getPortForwardRules() { return this.portForwardRules; }
    public void setPortForwardRules( List<PortForwardRule> portForwardRules ) { this.portForwardRules = portForwardRules; }

    public List<NatRule> getNatRules() { return this.natRules; }
    public void setNatRules( List<NatRule> natRules ) { this.natRules = natRules; }

    public List<BypassRule> getBypassRules() { return this.bypassRules; }
    public void setBypassRules( List<BypassRule> bypassRules ) { this.bypassRules = bypassRules; }

    public List<StaticRoute> getStaticRoutes() { return this.staticRoutes; }
    public void setStaticRoutes( List<StaticRoute> staticRoutes ) { this.staticRoutes = staticRoutes; }

    public String getHostName() { return this.hostName; }
    public void setHostName( String newValue ) { this.hostName = newValue; }

    public String getDomainName() { return this.domainName; }
    public void setDomainName( String newValue ) { this.domainName = newValue; }

    public Boolean getDynamicDnsServiceEnabled() { return this.dynamicDnsServiceEnabled; }
    public void setDynamicDnsServiceEnabled( Boolean newValue ) { this.dynamicDnsServiceEnabled = newValue; }

    public String getDynamicDnsServiceName() { return this.dynamicDnsServiceName; }
    public void setDynamicDnsServiceName( String newValue ) { this.dynamicDnsServiceName = newValue; }

    public String getDynamicDnsServiceUsername() { return this.dynamicDnsServiceUsername; }
    public void setDynamicDnsServiceUsername( String newValue ) { this.dynamicDnsServiceUsername = newValue; }

    public String getDynamicDnsServicePassword() { return this.dynamicDnsServicePassword; }
    public void setDynamicDnsServicePassword( String newValue ) { this.dynamicDnsServicePassword = newValue; }
    
    public String getDynamicDnsServiceHostnames() { return this.dynamicDnsServiceHostnames; }
    public void setDynamicDnsServiceHostnames( String newValue ) { this.dynamicDnsServiceHostnames = newValue; }
    
    public Boolean getEnableSipNatHelper() { return this.enableSipNatHelper; }
    public void setEnableSipNatHelper( Boolean newValue ) { this.enableSipNatHelper = newValue; }

    public Boolean getSendIcmpRedirects() { return this.sendIcmpRedirects; }
    public void setSendIcmpRedirects( Boolean newValue ) { this.sendIcmpRedirects = newValue; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    /**
     * Convenience method to find the InterfaceSettings for the specified Id
     */
    public InterfaceSettings findInterfaceId( int interfaceId )
    {
        if ( getInterfaces() == null)
            return null;
        
        for ( InterfaceSettings intf : getInterfaces() ) {
            if ( intf.getInterfaceId() == interfaceId )
                return intf;
        }

        return null;
    }

    /**
     * Convenience method to find the InterfaceSettings for the specified systemDev
     */
    public InterfaceSettings findInterfaceSystemDev( String systemDev )
    {
        if ( getInterfaces() == null)
            return null;
        
        for ( InterfaceSettings intf : getInterfaces() ) {
            if ( intf.getSystemDev().equals( systemDev ) )
                return intf;
        }

        return null;
    }

    /**
     * Convenience method to find the InterfaceSettings for the first WAN
     */
    public InterfaceSettings findInterfaceFirstWan( )
    {
        if ( getInterfaces() == null)
            return null;
        
        for ( InterfaceSettings intf : getInterfaces() ) {
            if ( intf.getIsWan() )
                return intf;
        }

        return null;
    }
    
}
