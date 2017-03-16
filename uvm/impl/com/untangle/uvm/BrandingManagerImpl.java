/**
 * $Id$
 */
package com.untangle.uvm;

import com.untangle.uvm.BrandingManager;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.app.AppSettings;
import com.untangle.uvm.app.AppManager;
import com.untangle.uvm.app.AppBase;

public class BrandingManagerImpl implements BrandingManager
{
    private DefaultBrandingManager defaultBranding;

    private final String defaultCompanyName = "Untangle";
    private final String defaultCompanyUrl = "http://untangle.com/";

    public BrandingManagerImpl()
    {
        this.defaultBranding = new DefaultBrandingManager();
    }

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
        String brandingUrl = this.getBrandingManager().getCompanyUrl();
        if (brandingUrl != null)
            ret = brandingUrl;
            
        return ret;
    }

    @Override
    public String getContactName()
    {
        return this.getBrandingManager().getContactName();
    }

    @Override
    public String getContactEmail()
    {
        return this.getBrandingManager().getContactEmail();
    }

    @Override
    public String getContactHtml()
    {
        return this.getBrandingManager().getContactHtml();
    }
    
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
    
    @SuppressWarnings("unused")
    private class DefaultBrandingManager implements BrandingManager
    {
        public boolean isDefaultLogo() {return true;}
        public String getContactHtml() {return this.getContactName();}
        public String getContactEmail() {return null;}
        public String getContactName() {return "your network administrator";} 
        public String getCompanyUrl() {return null;} 
        public String getCompanyName() {return null;} 
    }
}
