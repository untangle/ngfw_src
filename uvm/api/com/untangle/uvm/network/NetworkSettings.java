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
    private List<DeviceSettings> devices = null;
    private List<PortForwardRule> portForwardRules = null;
    private List<NatRule> natRules = null;
    private List<BypassRule> bypassRules = null;
    private List<FilterRule> inputFilterRules = null;
    private List<FilterRule> forwardFilterRules = null;
    private List<StaticRoute> staticRoutes = null;

    private String hostName;
    private String domainName;

    private boolean dynamicDnsServiceEnabled = false;
    private String  dynamicDnsServiceName = null;
    private String  dynamicDnsServiceUsername = null;
    private String  dynamicDnsServicePassword = null;
    private String  dynamicDnsServiceHostnames = null;

    private boolean enableSipNatHelper = false;
    private boolean sendIcmpRedirects = true;
    private boolean dhcpAuthoritative = true;

    private int httpsPort = 443;
    private boolean insideHttpEnabled = true;
    private boolean outsideHttpsEnabled = false;
    
    private QosSettings qosSettings;
    private DnsSettings dnsSettings;
    
    public NetworkSettings() { }

    public List<InterfaceSettings> getInterfaces() { return this.interfaces; }
    public void setInterfaces( List<InterfaceSettings> interfaces ) { this.interfaces = interfaces; }

    public List<DeviceSettings> getDevices() { return this.devices; }
    public void setDevices( List<DeviceSettings> devices ) { this.devices = devices; }
    
    public List<PortForwardRule> getPortForwardRules() { return this.portForwardRules; }
    public void setPortForwardRules( List<PortForwardRule> portForwardRules ) { this.portForwardRules = portForwardRules; }

    public List<NatRule> getNatRules() { return this.natRules; }
    public void setNatRules( List<NatRule> natRules ) { this.natRules = natRules; }

    public List<BypassRule> getBypassRules() { return this.bypassRules; }
    public void setBypassRules( List<BypassRule> bypassRules ) { this.bypassRules = bypassRules; }

    public List<FilterRule> getInputFilterRules() { return this.inputFilterRules; }
    public void setInputFilterRules( List<FilterRule> bypassRules ) { this.inputFilterRules = bypassRules; }

    public List<FilterRule> getForwardFilterRules() { return this.forwardFilterRules; }
    public void setForwardFilterRules( List<FilterRule> bypassRules ) { this.forwardFilterRules = bypassRules; }
    
    public List<StaticRoute> getStaticRoutes() { return this.staticRoutes; }
    public void setStaticRoutes( List<StaticRoute> staticRoutes ) { this.staticRoutes = staticRoutes; }

    public String getHostName() { return this.hostName; }
    public void setHostName( String newValue ) { this.hostName = newValue; }

    public String getDomainName() { return this.domainName; }
    public void setDomainName( String newValue ) { this.domainName = newValue; }

    public boolean getDynamicDnsServiceEnabled() { return this.dynamicDnsServiceEnabled; }
    public void setDynamicDnsServiceEnabled( boolean newValue ) { this.dynamicDnsServiceEnabled = newValue; }

    public String getDynamicDnsServiceName() { return this.dynamicDnsServiceName; }
    public void setDynamicDnsServiceName( String newValue ) { this.dynamicDnsServiceName = newValue; }

    public String getDynamicDnsServiceUsername() { return this.dynamicDnsServiceUsername; }
    public void setDynamicDnsServiceUsername( String newValue ) { this.dynamicDnsServiceUsername = newValue; }

    public String getDynamicDnsServicePassword() { return this.dynamicDnsServicePassword; }
    public void setDynamicDnsServicePassword( String newValue ) { this.dynamicDnsServicePassword = newValue; }
    
    public String getDynamicDnsServiceHostnames() { return this.dynamicDnsServiceHostnames; }
    public void setDynamicDnsServiceHostnames( String newValue ) { this.dynamicDnsServiceHostnames = newValue; }
    
    /**
     * This is the port that the HTTPS server lives on
     */
    public int getHttpsPort() { return this.httpsPort; }
    public void setHttpsPort( int newValue ) { this.httpsPort = newValue ; }

    /**
     * Get whether or not local insecure access is enabled.
     */
    public boolean getInsideHttpEnabled() { return this.insideHttpEnabled; }
    public void setInsideHttpEnabled( boolean newValue ) { this.insideHttpEnabled = newValue; }

    /**
     * Retrieve whether or not administration from the internet is allowed.
     */
    public boolean getOutsideHttpsEnabled() { return this.outsideHttpsEnabled; }
    public void setOutsideHttpsEnabled( boolean newValue ) { this.outsideHttpsEnabled = newValue; }

    public boolean getEnableSipNatHelper() { return this.enableSipNatHelper; }
    public void setEnableSipNatHelper( boolean newValue ) { this.enableSipNatHelper = newValue; }

    public boolean getSendIcmpRedirects() { return this.sendIcmpRedirects; }
    public void setSendIcmpRedirects( boolean newValue ) { this.sendIcmpRedirects = newValue; }

    public boolean getDhcpAuthoritative() { return this.dhcpAuthoritative; }
    public void setDhcpAuthoritative( boolean newValue ) { this.dhcpAuthoritative = newValue; }

    public QosSettings getQosSettings() { return this.qosSettings; }
    public void setQosSettings( QosSettings newValue ) { this.qosSettings = newValue; }

    public DnsSettings getDnsSettings() { return this.dnsSettings; }
    public void setDnsSettings( DnsSettings newValue ) { this.dnsSettings = newValue; }
    
    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
