/**
 * $Id: StaticRoute.java,v 1.00 2013/01/14 14:50:10 dmorris Exp $
 */
package com.untangle.uvm.network;

import java.util.List;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.regex.Pattern;

import org.json.JSONObject;
import org.json.JSONString;
import org.apache.log4j.Logger;

import com.untangle.uvm.node.IPMaskedAddress;

/**
 * This in the implementation of a Static Route
 *
 * A route is basically a masked address with a destination
 * A destination can be an interface or local IP
 */
@SuppressWarnings("serial")
public class StaticRoute implements JSONString, Serializable
{
    private final Logger logger = Logger.getLogger(getClass());
    
    private Integer ruleId;
    private String description; 
    private IPMaskedAddress network;
    private String nextHop; /* Can store the dev name "eth1" or IP "1.2.3.4" */
    
    public StaticRoute() {}

    public StaticRoute( IPMaskedAddress network, String nextHop )
    {
        this.setNetwork( network );
        this.setNextHop( nextHop );
    }
    
    public Integer getRuleId() { return this.ruleId; }
    public void setRuleId(Integer ruleId) { this.ruleId = ruleId; }

    public String getDescription() { return description; }
    public void setDescription( String description ) { this.description = description; }

    public IPMaskedAddress getNetwork() { return network; }
    public void setNetwork( IPMaskedAddress network ) { this.network = network; }

    public String getNextHop() { return nextHop; }
    public void setNextHop( String nextHop ) { this.nextHop = nextHop; }

    public boolean getToAddr()
    {
        return Pattern.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}", nextHop);
    }

    public boolean getToDev()
    {
        return !getToAddr();
    }
    
    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}

