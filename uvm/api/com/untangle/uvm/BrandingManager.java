/*
 * $Id$
 */
package com.untangle.uvm;

/**
 * Allows the user to customize the branding of the product.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public interface BrandingManager
{
    public String getContactHtml();
    public String getContactEmail();
    public String getContactName();
    public String getCompanyUrl();
    public String getCompanyName();
}
