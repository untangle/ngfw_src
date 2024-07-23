/**
 * $Id$
 */
package com.untangle.uvm;

import java.io.Serializable;

import org.json.JSONObject;
import org.json.JSONString;
import com.untangle.uvm.util.ValidSerializable;

/**
 * Uvm administrator settings.
 */
@SuppressWarnings("serial")
@ValidSerializable
public class OemSettings implements Serializable, JSONString
{
    private String oemName;
    private String oemShortName;
    private String oemProductName;
    private String oemUrl;
    private String licenseAgreementUrl = "https://edge.arista.com/legal";

    public OemSettings() { }

    public OemSettings( String oemName, String oemUrl, String licenseAgreementUrl)
    {
        this.oemName = oemName;
        this.oemShortName = oemName;
        this.oemProductName = oemName;
        this.oemUrl = oemUrl;
        this.licenseAgreementUrl = licenseAgreementUrl;
    }

    public OemSettings( String oemName, String oemShortName, String oemProductName, String oemUrl, String licenseAgreementUrl)
    {
        this.oemName = oemName;
        this.oemShortName = oemShortName;
        this.oemProductName = oemProductName;
        this.oemUrl = oemUrl;
        this.licenseAgreementUrl = licenseAgreementUrl;
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
     * The OEM URL, ie "https://edge.arista.com"
     */
    public String getOemUrl() { return oemUrl; }
    public void setOemUrl( String newValue ) { this.oemUrl = newValue; }

    /**
     * The license agreement ie https://edge.arista.com/legal 
     */
    public String getLicenseAgreementUrl() { return licenseAgreementUrl; }
    public void setLicenseAgreementUrl( String newValue ) { this.licenseAgreementUrl = newValue; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
