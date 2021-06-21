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
    private String oemLicenseAgreementUrl;
    private Boolean isOem;
    private Boolean useLocalEula;

    public OemSettings() { }

    public OemSettings( String oemName, String oemUrl, String oemLicenseAgreementUrl, Boolean isOem, Boolean useLocalEula)
    {
        this.oemName = oemName;
        this.oemUrl = oemUrl;
        this.oemLicenseAgreementUrl = oemLicenseAgreementUrl;
        this.isOem = isOem;
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
     * The OEM license agreement ie https://www.untangle.com/legal/ 
     */
    public String getOemLicenseAgreementUrl() { return oemLicenseAgreementUrl; }
    public void setOemLicenseAgreementUrl( String newValue ) { this.oemLicenseAgreementUrl = newValue; }

    /**
     * If we're building an oem
     */
    public Boolean getIsOem() { return isOem; }
    public void setIsOem( Boolean newValue ) { this.isOem = newValue; }

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
