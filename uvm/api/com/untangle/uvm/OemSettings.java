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
    private String oemShortName;
    private String oemProductName;
    private String oemUrl;
    private String licenseAgreementUrl = "http://www.untangle.com/legal";
    private Boolean useLocalEula = false;

    public OemSettings() { }

    public OemSettings( String oemName, String oemUrl, String licenseAgreementUrl, Boolean useLocalEula)
    {
        this.oemName = oemName;
        this.oemShortName = oemName;
        this.oemProductName = oemName;
        this.oemUrl = oemUrl;
        this.licenseAgreementUrl = licenseAgreementUrl;
        this.useLocalEula = useLocalEula;
    }

    public OemSettings( String oemName, String oemShortName, String oemProductName, String oemUrl, String licenseAgreementUrl, Boolean useLocalEula)
    {
        this.oemName = oemName;
        this.oemShortName = oemShortName;
        this.oemProductName = oemProductName;
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
     * The OEM short name, ie "Vendor2" vs "Vendor2 Router"
     */
    public String getOemShortName() { return oemShortName; }
    public void setOemShortName( String newValue ) { this.oemShortName = newValue; }

        /**
     * The OEM product name, ie "Untangle NGFW" vs "Untangle"
     */
    public String getOemProductName() { return oemProductName; }
    public void setOemProductName( String newValue ) { this.oemProductName = newValue; }

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
