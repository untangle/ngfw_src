/**
 * $Id$
 */
package com.untangle.app.license;

import java.io.Serializable;
import java.util.List;
import java.util.LinkedList;

import org.json.JSONObject;
import org.json.JSONString;

import com.untangle.uvm.app.License;

/**
 * License setttings.
 */
@SuppressWarnings("serial")
public class LicenseSettings implements Serializable, JSONString
{
    private Integer settingsVersion = new Integer(1);

    private List<License> licenses;
    
    public LicenseSettings( )
    {
        this.licenses = new LinkedList<>();
    }

    public LicenseSettings( List<License> licenses )
    {
        this.licenses = licenses;
    }

    public Integer getSettingsVersion()
    {
        return settingsVersion;
    }

    public void setSettingsVersion( Integer settingsVersion )
    {
        this.settingsVersion = settingsVersion;
    }

    public List<License> getLicenses()
    {
        return this.licenses;
    }

    public void setLicenses(List<License> licenses)
    {
        this.licenses = licenses;
    }
    
    /**
     * Convert settings to JSON string.
     *
     * @return
     *      JSON string.
     */
    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
