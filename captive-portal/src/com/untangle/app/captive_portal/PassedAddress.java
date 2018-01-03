/**
 * $Id$
 */

package com.untangle.app.captive_portal;

import com.untangle.uvm.app.IPMatcher;

/**
 * This class represents a Passed Address and is used to keep a list of allowed
 * client and server IP addresses. We added these special lists to simplify
 * configuration for those who find rules confusing.
 * 
 * @author mahotz
 * 
 */

@SuppressWarnings("serial")
public class PassedAddress implements java.io.Serializable, org.json.JSONString
{
    private boolean enabled = true;
    private boolean log = false;
    private IPMatcher address = IPMatcher.getNilMatcher();
    private String description = null;

// THIS IS FOR ECLIPSE - @formatter:off

    public IPMatcher getAddress() { return this.address; }
    public void setAddress(IPMatcher newValue) { this.address = newValue; }

    public boolean getEnabled() { return this.enabled; }
    public void setEnabled( boolean newValue ) { this.enabled = newValue; }

    /* deprecated - live renamed to enabled - this remains so json serialization works */
    public void setLive(boolean live) { this.enabled = live; }

    public boolean getLog() { return log; }
    public void setLog(boolean log) { this.log = log; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

// THIS IS FOR ECLIPSE - @formatter:on

    public String toJSONString()
    {
        org.json.JSONObject jO = new org.json.JSONObject(this);
        return jO.toString();
    }
}
