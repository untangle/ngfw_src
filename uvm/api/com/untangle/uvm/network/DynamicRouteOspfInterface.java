/**
 * $Id$
 */
package com.untangle.uvm.network;

import java.io.Serializable;

import org.json.JSONObject;
import org.json.JSONString;
import org.apache.log4j.Logger;

/**
 * Dynamic route class used for BGP and ospf. 
 * Only ospf uses the area field.
 */
@SuppressWarnings("serial")
public class DynamicRouteOspfInterface implements JSONString, Serializable
{
    private final Logger logger = Logger.getLogger(getClass());
    
    private Integer ruleId = null;
    private String description = null;
    private Boolean enabled = false;

    private String dev = "";

    private Integer helloInterval = 10;
    private Integer deadInterval = 40;
    private Integer retransmitInterval = 5;
    private Integer transmitDelay = 1;

    private Boolean autoInterfaceCost = true;
    private Integer interfaceCost = 0;

    private Integer authentication = 0;
    private String authenticationPassword = "";
    private String authenticationKeyId = "";
    private String authenticationKey = "";

    private Integer routerPriority = 1;

    public DynamicRouteOspfInterface() {}

    public Integer getRuleId() { return this.ruleId; }
    public void setRuleId(Integer ruleId) { this.ruleId = ruleId; }

    public Boolean getEnabled() { return this.enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public String getDescription() { return description; }
    public void setDescription( String description ) { this.description = description; }

    public String getDev() { return dev; }
    public void setDev( String dev ) { this.dev = dev;}

    public Integer getHelloInterval() { return helloInterval; }
    public void setHelloInterval( Integer helloInterval ) { this.helloInterval = helloInterval;}

    public Integer getDeadInterval() { return deadInterval; }
    public void setDeadInterval( Integer deadInterval ) { this.deadInterval = deadInterval;}

    public Integer getRetransmitInterval() { return retransmitInterval; }
    public void setRetransmitInterval( Integer retransmitInterval ) { this.retransmitInterval = retransmitInterval;}

    public Integer getTransmitDelay() { return transmitDelay; }
    public void setTransmitDelay( Integer transmitDelay ) { this.transmitDelay = transmitDelay;}

    public Boolean getAutoInterfaceCost() { return autoInterfaceCost; }
    public void setAutoInterfaceCost( Boolean autoInterfaceCost ) { this.autoInterfaceCost = autoInterfaceCost;}

    public Integer getInterfaceCost() { return interfaceCost; }
    public void setInterfaceCost( Integer interfaceCost ) { this.interfaceCost = interfaceCost;}

    public Integer getAuthentication() { return authentication; }
    public void setAuthentication( Integer authentication ) { this.authentication = authentication;}

    public String getAuthenticationPassword() { return authenticationPassword; }
    public void setAuthenticationPassword( String authenticationPassword ) { this.authenticationPassword = authenticationPassword;}

    public String getAuthenticationKeyId() { return authenticationKeyId; }
    public void setAuthenticationKeyId( String authenticationKeyId ) { this.authenticationKeyId = authenticationKeyId;}

    public String getAuthenticationKey() { return authenticationKey; }
    public void setAuthenticationKey( String authenticationKey ) { this.authenticationKey = authenticationKey;}

    public Integer getRouterPriority() { return routerPriority; }
    public void setRouterPriority( Integer routerPriority ) { this.routerPriority = routerPriority;}

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

}