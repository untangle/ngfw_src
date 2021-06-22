/**
 * $Id$
 */
package com.untangle.uvm;

import java.io.Serializable;

import org.json.JSONObject;
import org.json.JSONString;

/**
 * Uvm administrator settings.
 */
@SuppressWarnings("serial")
public class OemSettings implements Serializable, JSONString
{
    private String oemName;
    private String oemUrl;
    private String licenseAgreementUrl = "http://www.untangle.com/legal";
    private Boolean useLocalEula = false;

    public OemSettings() { }

    public OemSettings( String oemName, String oemUrl, String licenseAgreementUrl, Boolean useLocalEula)
    {
        this.oemName = oemName;
        this.oemUrl = oemUrl;
        this.licenseAgreementUrl = licenseAgreementUrl;
        this.useLocalEula = useLocalEula;
    }

    /**
     * The OEM name, ie "Untangle"
     */
    public String getOemName() { return oemName; }
    public void setOemName( String newValue ) { this.oemName = newValue; }

    /**
     * The OEM URL, ie "http://untangle.com"
     */
    public String getOemUrl() { return oemUrl; }
    public void setOemUrl( String newValue ) { this.oemUrl = newValue; }

    /**
     * The license agreement ie https://www.untangle.com/legal/ 
     */
    public String getLicenseAgreementUrl() { return licenseAgreementUrl; }
    public void setLicenseAgreementUrl( String newValue ) { this.licenseAgreementUrl = newValue; }

    /**
     If should use saved legal.html
     */
    public Boolean getUseLocalEula() { return useLocalEula; }
    public void setUseLocalEula( Boolean newValue ) { this.useLocalEula = newValue; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
