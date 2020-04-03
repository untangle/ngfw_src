/**
 * $Id: BandwidthControlSettings.java,v 1.00 2018/03/20 20:08:13 dmorris Exp $
 */
package com.untangle.app.bandwidth_control;

import java.util.List;
import org.json.JSONString;
import org.json.JSONObject;
    
/**
 * The Bandwidth Control settings object
 */
@SuppressWarnings("serial")
public class BandwidthControlSettings implements java.io.Serializable, JSONString
{
    private Integer settingsVersion = 5; /* current version is 5 */

    private Boolean configured = Boolean.FALSE;
    
    private List<BandwidthControlRule> rules;
    
    public Integer getSettingsVersion()
    {
        return settingsVersion;
    }

    public void setSettingsVersion( Integer settingsVersion )
    {
        this.settingsVersion = settingsVersion;
    }

    public Boolean getConfigured()
    {
        return configured;
    }

    public void setConfigured( Boolean configured )
    {
        this.configured = configured;
    }
    
    public List<BandwidthControlRule> getRules()
    {
        return rules;
    }

    public void setRules( List<BandwidthControlRule> rules )
    {
        this.rules = rules;
    }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
