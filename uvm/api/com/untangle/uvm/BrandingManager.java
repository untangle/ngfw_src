/*
 * $Id: BrandingManager.java 35447 2013-07-29 17:24:43Z dmorris $
 */
package com.untangle.uvm;

/**
 * Allows the user to customize the branding of the product.
 *
 */
public interface BrandingManager
{
    public String getContactHtml();
    public String getContactEmail();
    public String getContactName();
    public String getCompanyUrl();
    public String getCompanyName();
}
