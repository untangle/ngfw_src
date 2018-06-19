/**
 * $Id$
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
