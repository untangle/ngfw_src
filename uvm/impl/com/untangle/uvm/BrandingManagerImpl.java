/**
 * $Id$
 */
package com.untangle.uvm;

import com.untangle.uvm.BrandingManager;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.app.AppSettings;
import com.untangle.uvm.app.AppManager;
import com.untangle.uvm.app.AppBase;

/**
 * Branding manager
 */
public class BrandingManagerImpl implements BrandingManager
{
    private DefaultBrandingManager defaultBranding;

    private final String defaultCompanyName = "Untangle";
    private final String defaultCompanyUrl = "http://untangle.com/";

    /**
     * Initialize instance of BrandingManagerImpl.
     * @return instance of BrandingManagerImpl.
     */
    public BrandingManagerImpl()
    {
        this.defaultBranding = new DefaultBrandingManager();
    }

    /**
     * Return company name.
     *
     * @return String of company name using default name then fallback to OEM and then branding manager name.
     */
    @Override
    public String getCompanyName()
    {
        /**
         * Start with the default
         */
        String ret = defaultCompanyName;

        /**
         * If there is an OEM name specified - use it instead
         */
        String oemName = UvmContextFactory.context().oemManager().getOemName();
        if (oemName != null)
            ret = oemName;

        /**
         * If there is an Branding name specified - use it instead
         */
        BrandingManager bm = getBrandingManager();
        if (bm == null)
            return ret;
        String brandingName = bm.getCompanyName();
        if (brandingName != null)
            ret = brandingName;

        return ret;
    }

    /**
     * Return the company URL.
     * @return String of company URL starting with with the default name then fallback to OEM and then branding manager name.
     */
    @Override
    public String getCompanyUrl()
    {
        /**
         * Start with the default
         */
        String ret = defaultCompanyUrl;

        /**
         * If there is an OEM name specified - use it instead
         */
        String oemUrl = UvmContextFactory.context().oemManager().getOemUrl();
        if (oemUrl != null)
            ret = oemUrl;

        /**
         * If there is an Branding name specified - use it instead
         */
        BrandingManager bm = this.getBrandingManager();
        String brandingUrl = (bm != null) ? bm.getCompanyUrl() : "";
        if (brandingUrl != null)
            ret = brandingUrl;
            
        return ret;
    }

    /**
     * Return contact name
     * 
     * @return String of contact name.  Look for value from branding manager.  If it doesn't exist, empty string.
     */
    @Override
    public String getContactName()
    {
        BrandingManager bm = this.getBrandingManager();
        return (bm != null) ? bm.getContactName() : "";
    }

    /**
     * Return contact email.
     * @return String of contact email.  Look for value from branding manager.  If it doesn't exist, empty string.
     */
    @Override
    public String getContactEmail()
    {
        BrandingManager bm = this.getBrandingManager();
        return (bm != null) ? bm.getContactEmail() : "";
    }

    /**
     * Return contact HTML.
     * @return String of contact HTML.  Look for value from branding manager.  If it doesn't exist, empty string.
     */
    @Override
    public String getContactHtml()
    {
        BrandingManager bm = this.getBrandingManager();
        return (bm != null) ? bm.getContactHtml() : "";
    }
    
    /**
     * Return branding manager app.
     * @return Branding manager app. Look for app.  If cannot be found, return a "dummy" with default values.
     */
    private BrandingManager getBrandingManager()
    {
        AppManager nm = UvmContextFactory.context().appManager();
        if (nm == null) /* happens on shutdown */
            return null;
        BrandingManager bapp = (BrandingManager)nm.app("branding-manager");
        if (bapp != null && (((AppBase)bapp).getRunState() == AppSettings.AppState.RUNNING)) {
            return bapp;
        }

        return this.defaultBranding;
    }
    
    /**
     * "dummy" branding manager with default values.
     */
    @SuppressWarnings("unused")
    private class DefaultBrandingManager implements BrandingManager
    {
        /**
         * Determine if this is default logo.
         * @return Always return true.
         */
        public boolean isDefaultLogo() {return true;}

        /**
         * Return contact name.
         * @return Always return string of contact name.
         */
        public String getContactHtml() {return this.getContactName();}

        /**
         * Return contact email.
         * @return Always return null string.
         */
        public String getContactEmail() {return null;}
        /**
         * Return contact name.
         * @return Always return fixed string.
         */
        public String getContactName() {return "your network administrator";} 
        /**
         * Return contact url.
         * @return Always return null string.
         */
        public String getCompanyUrl() {return null;} 
        /**
         * Return company name.
         * @return Always return null string.
         */
        public String getCompanyName() {return null;} 
    }
}
