/**
 * $Id$
 */
package com.untangle.app.license;

import java.io.Serializable;
import java.util.List;
import java.util.LinkedList;

import org.json.JSONObject;
import org.json.JSONString;

import com.untangle.uvm.app.UserLicenseMessage;
import com.untangle.uvm.app.License;

/**
 * License setttings.
 */
@SuppressWarnings("serial")
public class LicenseSettings implements Serializable, JSONString
{
    private Integer settingsVersion = 1;

    private List<License> licenses;

    private List<UserLicenseMessage> userLicenseMessages = new LinkedList<>();

    private boolean isRestricted = false;
    
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

    public boolean getIsRestricted()
    {
        return this.isRestricted;
    }

    public void setIsRestricted(boolean restrict)
    {
        this.isRestricted = restrict;
    }
    
    public List<UserLicenseMessage> getUserLicenseMessages() 
    {
        return this.userLicenseMessages;
    }

    public void setUserLicenseMessages(List<UserLicenseMessage> newUserLicenseMessages)
    {
        this.userLicenseMessages = newUserLicenseMessages;
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
