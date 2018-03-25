/**
 * $Id$
 */
package com.untangle.uvm.network;

import java.io.Serializable;
import java.net.InetAddress;

import org.json.JSONObject;
import org.json.JSONString;
import org.apache.log4j.Logger;

/**
 * Dynamic route class used for BGP and ospf. 
 * Only ospf uses the area field.
 */
@SuppressWarnings("serial")
public class DynamicRouteBgpNeighbor implements JSONString, Serializable
{
    private final Logger logger = Logger.getLogger(getClass());
    
    private Integer ruleId = null;
    private Boolean enabled = false;
    private String description = null;     
    private InetAddress ipAddress = null;
    private String as = "";

    public DynamicRouteBgpNeighbor() {}

    public Integer getRuleId() { return this.ruleId; }
    public void setRuleId(Integer ruleId) { this.ruleId = ruleId; }

    public Boolean getEnabled() { return this.enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public String getDescription() { return description; }
    public void setDescription( String description ) { this.description = description; }

    public InetAddress getIpAddress() { return ipAddress; }
    public void setIpAddress( InetAddress ipAddress ) { this.ipAddress = ipAddress; }

    public String getAs() { return as; }
    public void setAs( String as ) { this.as = as; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

}

