/**
 * $Id$
 */
package com.untangle.uvm.network;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.regex.Pattern;

import org.json.JSONObject;
import org.json.JSONString;
import org.apache.log4j.Logger;

import com.untangle.uvm.app.IPMaskedAddress;

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
    
    private Integer ruleId = null;
    private String description = null; 
    private InetAddress network = null;
    private Integer prefix = null ; /* 0-32 */
    private String nextHop = null; /* Can store the dev name "eth1" or IP "1.2.3.4" */
    
    public StaticRoute() {}

    public StaticRoute( InetAddress network, String nextHop )
    {
        this.setNetwork( network );
        this.setNextHop( nextHop );
    }
    
    public Integer getRuleId() { return this.ruleId; }
    public void setRuleId(Integer ruleId) { this.ruleId = ruleId; }

    public String getDescription() { return description; }
    public void setDescription( String description ) { this.description = description; }

    public InetAddress getNetwork() { return network; }
    public void setNetwork( InetAddress network ) { this.network = network; this.recalculateNetwork();}

    public Integer getPrefix() { return prefix; }
    public void setPrefix( Integer prefix ) { this.prefix = prefix; this.recalculateNetwork(); }
    
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

    private void recalculateNetwork()
    {
        if (this.network == null)
            return;
        
        try {
            IPMaskedAddress maskedAddr = new IPMaskedAddress( this.network, this.prefix );
            this.network = maskedAddr.getMaskedAddress();
        } catch (Exception e) {
            logger.warn("Exception: ",e);
        }
        
    }
    
}

