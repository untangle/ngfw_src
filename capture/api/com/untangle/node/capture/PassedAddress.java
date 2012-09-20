package com.untangle.node.capture;

import com.untangle.uvm.node.IPMatcher;

@SuppressWarnings("serial")
public class PassedAddress 
{
    private boolean live = true;
    private String description = "";
    private IPMatcher address = IPMatcher.getNilMatcher();
    
    public IPMatcher getAddress() { return this.address; }
    public void setAddress( IPMatcher newValue ) { this.address = newValue; }

    public boolean getLive() { return live; }
    public void setLive(boolean live) { this.live = live; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
