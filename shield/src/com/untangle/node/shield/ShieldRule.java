/*
 * $Id: ShieldRule.java,v 1.00 2011/09/06 14:25:45 dmorris Exp $
 */
package com.untangle.node.shield;

import java.util.Map;
import java.util.HashMap;
import java.net.UnknownHostException;

import com.untangle.uvm.node.IPMaskedAddress;

/**
 * Rule for the shield
 */
@SuppressWarnings("serial")
public class ShieldRule implements java.io.Serializable
{
    private int id;
    private boolean enabled = true;
    private String description = "";
        
    private IPMaskedAddress address = null;
    private int multiplier = 1;
    
    public ShieldRule() { }

    public int getId() { return this.id; }
    public void setId( int newValue ) { this.id = newValue; }

    public Boolean getEnabled() { return this.enabled; }
    public void setEnabled( Boolean newValue ) { this.enabled = newValue; }

    public String getDescription() { return this.description; }
    public void setDescription( String newValue ) { this.description = newValue; }
    
    public IPMaskedAddress getAddress() { return this.address; }
    public void setAddress( IPMaskedAddress newValue ) { this.address = newValue; }

    public int getMultiplier() { return this.multiplier; }
    public void setMultiplier( int newValue ) { this.multiplier = newValue; }
}
