/* $HeadURL: svn://chef/work/src/uvm-lib/localapi/com/untangle/uvm/networking/InterfaceSettings.java $ */
package com.untangle.uvm.networking;

import java.util.List;
import java.util.LinkedList;
import java.net.InetAddress;
import org.json.JSONString;
import org.json.JSONObject;
import org.apache.log4j.Logger;

import com.untangle.uvm.node.ParseException;

@SuppressWarnings("serial")
public class InterfaceSettings implements java.io.Serializable, JSONString
{
    private final Logger logger = Logger.getLogger(getClass());

    public static final String CONFIG_STATIC  = "static";
    public static final String CONFIG_DYNAMIC = "dynamic";
    public static final String CONFIG_BRIDGE  = "bridge";
    public static final String CONFIG_PPPOE   = "pppoe";

    /* The unique identifier of this interface */
    private Integer interfaceId = null;
    /* The system name of the interface (eg eth0) */
    private String systemName = null;
    /* This is the user representation of the interface name (eg. Internal/External) */
    private String name = null;
    /* This is the configuration type of this interface */
    private String configType = null;

    /**
     * common configuration
     */
    private Boolean isWAN = null;
    private IPNetwork primaryAddress = null;
    private String    primaryAddressStr = null;
    private List<IPNetwork> aliases = null;
    private List<String>    aliasesStr = null;
    private InetAddress gateway = null;
    private String      gatewayStr = null;
    private InetAddress dns1 = null;
    private String      dns1Str = null;
    private InetAddress dns2 = null;
    private String      dns2Str = null;
    private Integer mtu = null;
    
    /**
     * dynamic unique configuration
     */
    private InetAddress overrideIPAddress = null;
    private InetAddress overrideNetmask = null;
    private InetAddress overrideGateway = null;
    private InetAddress overrideDns1 = null;
    private InetAddress overrideDns2 = null;

    /**
     * bridge unique configuration
     */
    private String bridgedTo = null;

    /**
     * pppoe unique configuration
     */
    private String pppoeUsername = null;
    private String pppoePassword = null;
    
    /**
     * Transient objects
     */
    private String connectionState;
    private String currentMedia;
    
    /**
     * Constructor for Static
     */
    public InterfaceSettings() {}

//     private InterfaceSettings( Integer id, String systemName, String name, String configType,
//                                Boolean isWAN,
//                                IPNetwork primaryAddress, List<IPNetwork> aliases,
//                                InetAddress dns1, InetAddress dns2,
//                                Integer mtu, String ethernetNegotiation)
//     {
//         this.interfaceId = id;
//         this.systemName = systemName;
//         this.name = name;
//         this.configType = configType;
//         this.isWAN = isWAN;
//         this.primaryAddress = primaryAddress;
//         this.primaryAddressStr = primaryAddress.toString();
//         this.aliases = aliases;
//         this.dns1 = dns1;
//         this.dns1Str = dns1.toString();
//         this.dns2 = dns2;
//         this.dns2Str = dns2.toString();
//         this.mtu = mtu;
//         this.ethernetNegotiation = ethernetNegotiation;
//     }

    public Integer getInterfaceId()
    {
        return this.interfaceId;
    }

    public void setInterfaceId( Integer interfaceId )
    {
        this.interfaceId = interfaceId;
    }
    
    public String getSystemName()
    {
        return this.systemName;
    }

    public void setSystemName( String systemName )
    {
        this.systemName = systemName;
    }
    
    public String getName()
    {
        return this.name;
    }

    public void setName( String name)
    {
        this.name = name;
    }

    public String getConfigType()
    {
        return this.configType;
    }

    public void setConfigType( String configType )
    {
        this.configType = configType;
    }

    public Boolean isWAN()
    {
        return this.isWAN;
    }

    public void setWAN( Boolean isWAN )
    {
        this.isWAN = isWAN;
    }

    public String getPrimaryAddressStr()
    {
        return this.primaryAddressStr;
    }
    
    public void setPrimaryAddressStr( String primaryAddressStr )
    {
        this.primaryAddressStr = primaryAddressStr;
        try {
            this.primaryAddress = IPNetwork.parse(primaryAddressStr);
        } catch (ParseException e) {
            logger.warn("Unable to parse IP: " + gateway, e);
        }
    }

    public IPNetwork getPrimaryAddress()
    {
        return this.primaryAddress;
    }
    
    public List<String> getAliasesStr()
    {
        return this.aliasesStr;
    }

    public void setAliasesStr( List<String> aliasesStr )
    {
        this.aliasesStr = aliasesStr;

        /* build a new parsed aliases list */
        List<IPNetwork> aliases = new LinkedList<IPNetwork>();
        for (String aliasStr : aliasesStr) {
            try {
                aliases.add(IPNetwork.parse(aliasStr));
            } catch (ParseException e) {
                logger.warn("Unable to parse IP: " + gateway, e);
            }
        }

        this.aliases = aliases;
    }

    public List<IPNetwork> getAliases()
    {
        return this.aliases;
    }
    
    public String getGatewayStr()
    {
        return this.gatewayStr;
    }

    public void setGatewayStr( String gatewayStr )
    {
        this.gatewayStr = gatewayStr;
        try {
            this.gateway = InetAddress.getByName(gatewayStr);
        } catch (java.net.UnknownHostException e) {
            logger.warn("Unable to parse IP: " + gateway, e);
        }
    }

    public InetAddress getGateway()
    {
        return this.gateway;
    }
    
    public String getDns1Str()
    {
        return this.dns1Str;
    }

    public void setDns1Str( String dns1Str )
    {
        this.dns1Str = dns1Str;
        try {
            this.dns1 = InetAddress.getByName(dns1Str);
        } catch (java.net.UnknownHostException e) {
            logger.warn("Unable to parse IP: " + dns1, e);
        }
    }

    public InetAddress getDns1()
    {
        return this.dns1;
    }

    public String getDns2Str()
    {
        return this.dns2Str;
    }

    public void setDns2Str( String dns2Str )
    {
        this.dns2Str = dns2Str;
        try {
            this.dns2 = InetAddress.getByName(dns2Str);
        } catch (java.net.UnknownHostException e) {
            logger.warn("Unable to parse IP: " + dns2, e);
        }
    }

    public InetAddress getDns2()
    {
        return this.dns2;
    }

    public Integer getMtu()
    {
        return this.mtu;
    }

    public void setMtu( Integer mtu )
    {
        this.mtu = mtu;
    }

    public String getPPPoEUsername()
    {
        return this.pppoeUsername;
    }

    public void setPPPoEUsername( String pppoeUsername )
    {
        this.pppoeUsername = pppoeUsername;
    }

    public String getPPPoEPassword()
    {
        return this.pppoePassword;
    }

    public void setPPPoEPassword( String pppoePassword )
    {
        this.pppoePassword = pppoePassword;
    }

    public String getConnectionState()
    {
        return this.connectionState;
    }

    public void setConnectionState(String connectionState)
    {
        this.connectionState = connectionState;
    }

    public String getCurrentMedia()
    {
        return this.currentMedia;
    }

    public void setCurrentMedia(String currentMedia)
    {
        this.currentMedia = currentMedia;
    }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
