/**
 * $Id$
 */

package com.untangle.app.wan_failover;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import org.json.JSONObject;
import org.json.JSONString;

/**
 * Stores the settings for the Wan Failover application
 */
@SuppressWarnings("serial")
public class WanFailoverSettings implements Serializable, JSONString
{
    private List<WanTestSettings> tests;

    private boolean resetUdpOnWanStateChange = true;
    
    public WanFailoverSettings()
    {
        this.tests = new LinkedList<WanTestSettings>();
    }

    public List<WanTestSettings> getTests() { return tests; }
    public void setTests( List<WanTestSettings> tests ) { this.tests = tests; }

    public boolean getResetUdpOnWanStateChange() { return this.resetUdpOnWanStateChange; }
    public void setResetUdpOnWanStateChange( boolean newValue ) { this.resetUdpOnWanStateChange = newValue; }
    
    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
