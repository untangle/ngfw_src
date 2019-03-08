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
 * Dynamic route class used for BGP and ospf. 
 * Only ospf uses the area field.
 */
@SuppressWarnings("serial")
public class DynamicRouteNetwork implements JSONString, Serializable
{
    private final Logger logger = Logger.getLogger(getClass());
    
    private Integer ruleId = null;
    private Boolean enabled = false;
    private String description = null; 
    private InetAddress network = null;
    private Integer prefix = null ; /* 0-32 */
    private Integer area = 0; /* Used by OSPF */

    public DynamicRouteNetwork() {}

    public Integer getRuleId() { return this.ruleId; }
    public void setRuleId(Integer ruleId) { this.ruleId = ruleId; }

    public Boolean getEnabled() { return this.enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public String getDescription() { return description; }
    public void setDescription( String description ) { this.description = description; }

    public InetAddress getNetwork() { return network; }
    public void setNetwork( InetAddress network ) { this.network = network; this.recalculateNetwork();}

    public Integer getPrefix() { return prefix; }
    public void setPrefix( Integer prefix ) { this.prefix = prefix; this.recalculateNetwork(); }
    
    public Integer getArea() { return area; }
    public void setArea( Integer area ) { this.area = area; }

    // public boolean getToAddr()
    // {
    //     return Pattern.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}", nextHop);
    // }

    // public boolean getToDev()
    // {
    //     return !getToAddr();
    // }
    
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

