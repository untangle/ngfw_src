/**
 * $Id$
 */
package com.untangle.app.branding_manager;

import java.io.Serializable;

import com.untangle.uvm.util.I18nUtil;
import org.json.JSONObject;
import org.json.JSONString;

/**
 * Contains properties that a vendor may use to rebrand the product.
 */
@SuppressWarnings("serial")
public class BrandingManagerSettings implements Serializable, JSONString
{
    private byte[] logo = null;
    private String companyName = "MyCompany";
    private String companyUrl = "http://www.example.com";
    private String contactName = "your network administrator";
    private String contactEmail = null;
    private boolean defaultLogo = true;
    private String bannerMessage = "";

    public BrandingManagerSettings()
    {
        setDefaultLogo(null == binary_getLogo());
    }

    public BrandingManagerSettings( BrandingManagerSettings copy )
    {
        this.setCompanyName(copy.getCompanyName());
        this.setCompanyUrl(copy.getCompanyUrl());
        this.setContactName(copy.getContactName());
        this.setContactEmail(copy.getContactEmail());
        this.binary_setLogo(copy.binary_getLogo());
        this.setDefaultLogo(copy.getDefaultLogo());
        this.setBannerMessage(copy.getBannerMessage());
    }

    public String getCompanyName() { return null == companyName ? "Untangle" : companyName; }
    public void setCompanyName( String companyName ) { this.companyName = companyName; }

    public String getCompanyUrl() { return null == companyUrl ? "http://www.untangle.com" : companyUrl; }
    public void setCompanyUrl( String companyUrl ) { this.companyUrl = companyUrl; }

    public String getContactName() { return null == contactName ? I18nUtil.marktr("your network administrator") : contactName; }
    public void setContactName( String name ) { this.contactName = name; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail( String contactEmail ) { this.contactEmail = contactEmail; }

    public String getBannerMessage() { return bannerMessage; }
    public void setBannerMessage( String bannerMessage ) { this.bannerMessage = bannerMessage; }

    /**
     * The vendor logo to use.
     * logo.
     * @return
     *  Vendor logo as a byte stream.  If null, default Untangle.
     */
    public byte[] binary_getLogo() { return logo; }
    public void binary_setLogo( byte[] logo ) { this.logo = logo; setDefaultLogo(null == logo); }

    public void setLogo(String str)
    {
        if ((str == null) || (str.length() == 0)){
            logo = null;
            setDefaultLogo(true);
            return;
        }

        // generate the binary logo from the argumented nibble string
        int len = (str.length() / 2);
        logo = new byte[len];

        for(int x = 0;x < len;x++){
            int lowNibble = (str.charAt((x * 2) + 0) - 'A');
            int highNibble = (str.charAt((x * 2) + 1) - 'A');
            int value = (((highNibble << 4) & 0xF0) | lowNibble);
            logo[x] = (byte)value;
        }

        setDefaultLogo(false);
    }

    public String getLogo()
    {
        if (logo == null) return(null);
        if (logo.length == 0) return(null);

        // generate the nibble string version from binary
        StringBuilder local = new StringBuilder();

        for(int x = 0;x < logo.length;x++){
            char lowNibble = (char)((logo[x] & 0x0F) + 'A');
            char highNibble = (char)(((logo[x] >> 4) & 0x0F) + 'A');
            local.append(lowNibble);
            local.append(highNibble);
        }

        return(local.toString());
    }

    public boolean getDefaultLogo()
    {
        return defaultLogo;
    }

    public void setDefaultLogo(boolean defaultLogo)
    {
        this.defaultLogo = defaultLogo;

        if (defaultLogo && binary_getLogo() != null)
            binary_setLogo(null);
    }

    public String grabContactHtml()
    {
        if (null != contactEmail && !contactEmail.trim().equals("")) {
            return "<a href='mailto:" + contactEmail + "'>" + contactName + "</a>";
        } else {
            return contactName;
        }
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
