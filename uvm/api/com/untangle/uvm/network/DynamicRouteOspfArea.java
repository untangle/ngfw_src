/**
 * $Id$
 */
package com.untangle.uvm.network;

import java.util.List;
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
public class DynamicRouteOspfArea implements JSONString, Serializable
{
    private final Logger logger = Logger.getLogger(getClass());
    
    private Integer ruleId = null;
    private String description = null;
    private String area = "";
    private Integer type = 0;
    private Integer authentication = 0;
    private List<InetAddress> virtualLinks = null;

    public DynamicRouteOspfArea() {}

    public Integer getRuleId() { return this.ruleId; }
    public void setRuleId(Integer ruleId) { this.ruleId = ruleId; }

    public String getDescription() { return description; }
    public void setDescription( String description ) { this.description = description; }

    public String getArea() { return area; }
    public void setArea( String area ) { this.area = area;}

    public Integer getType() { return type; }
    public void setType( Integer type ) { this.type = type;}

    public Integer getAuthentication() { return authentication; }
    public void setAuthentication( Integer authentication ) { this.authentication = authentication;}

    public List<InetAddress> getVirtualLinks() { return virtualLinks; }
    public void setVirtualLinks( List<InetAddress> virtualLinks ) { this.virtualLinks = virtualLinks;}

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

}