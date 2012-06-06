/**
 * $Id: SkinManager.java,v 1.00 2012/06/05 18:23:22 dmorris Exp $
 */
package com.untangle.uvm;

import java.util.List;

import org.apache.commons.fileupload.FileItem;

/**
 * Allows the user to customize the skins of the product.
 */
public interface SkinManager
{
    /**
     * Get the settings.
     *
     * @return the settings.
     */
	SkinSettings getSettings();

    /**
     * Set the settings.
     *
     * @param skinSettings the settings.
     */
    void setSettings(SkinSettings skinSettings);
    
    /**
     * Upload a new skin
     */
    void uploadSkin(FileItem item) throws UvmException;
    
    /**
     * Return all available skins
     */
    List<SkinInfo> getSkinsList();
}
