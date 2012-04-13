/**
 * $Id: BrandingManagerImpl.java,v 1.00 2012/04/06 11:46:05 dmorris Exp $
 */
package com.untangle.uvm.engine;

import com.untangle.uvm.BrandingManager;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.node.NodeSettings;
import com.untangle.uvm.node.NodeManager;
import com.untangle.uvm.vnet.NodeBase;

class BrandingManagerImpl implements BrandingManager
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
        String brandingName = this.getBrandingManager().getCompanyName();
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
        NodeManager nm = UvmContextFactory.context().nodeManager();
        if (nm == null) /* happens on shutdown */
            return null;
        BrandingManager bnode = (BrandingManager)nm.node("untangle-node-branding");
        if (bnode != null && (((NodeBase)bnode).getRunState() == NodeSettings.NodeState.RUNNING)) {
            return bnode;
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
