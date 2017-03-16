/**
 * $Id$
 */
package com.untangle.app.branding_manager;

import java.io.Serializable;

import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.app.AppSettings;

/**
 * Contains properties that a vendor may use to rebrand the product.
 */
@SuppressWarnings("serial")
public class BrandingManagerSettings implements Serializable
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

    /**
     * Get the vendor name.
     */
    public String getCompanyName() { return null == companyName ? "Untangle" : companyName; }
    public void setCompanyName( String companyName ) { this.companyName = companyName; }

    /**
     * Get the vendor URL.
     */
    public String getCompanyUrl() { return null == companyUrl ? "http://www.untangle.com" : companyUrl; }
    public void setCompanyUrl( String companyUrl ) { this.companyUrl = companyUrl; }

    /**
     * Get the vendor contact name.
     */
    public String getContactName() { return null == contactName ? I18nUtil.marktr("your network administrator") : contactName; }
    public void setContactName( String name ) { this.contactName = name; }

    /**
     * Get the vendor contact email.
     */
    public String getContactEmail() { return contactEmail; }
    public void setContactEmail( String contactEmail ) { this.contactEmail = contactEmail; }

    /**
     * Get the vendor contact email.
     */
    public String getBannerMessage() { return bannerMessage; }
    public void setBannerMessage( String bannerMessage ) { this.bannerMessage = bannerMessage; }

    /**
     * The vendor logo to use, null means use the default Untangle
     * logo.
     */
    public byte[] binary_getLogo() { return logo; }
    public void binary_setLogo( byte[] logo ) { this.logo = logo; setDefaultLogo(null == logo); }

    public void setLogo(String str)
    {
        if ((str == null) || (str.length() == 0))
        {
            logo = null;
            setDefaultLogo(true);
            return;
        }

    // generate the binary logo from the argumented nibble string
    int len = (str.length() / 2);
    logo = new byte[len];

        for(int x = 0;x < len;x++)
        {
        int lo_nib = (str.charAt((x * 2) + 0) - 'A');
        int hi_nib = (str.charAt((x * 2) + 1) - 'A');
        int value = (((hi_nib << 4) & 0xF0) | lo_nib);
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

        for(int x = 0;x < logo.length;x++)
        {
        char lo_nib = (char)((logo[x] & 0x0F) + 'A');
        char hi_nib = (char)(((logo[x] >> 4) & 0x0F) + 'A');
        local.append(lo_nib);
        local.append(hi_nib);
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
}
