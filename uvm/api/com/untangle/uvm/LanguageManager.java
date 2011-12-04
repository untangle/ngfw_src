/**
 * $Id: LanguageManager.java,v 1.00 2011/12/04 10:58:26 dmorris Exp $
 */
package com.untangle.uvm;

import java.util.List;
import java.util.Map;

import org.apache.commons.fileupload.FileItem;

/**
 * Allows the user to customize the language of the product.
 *
 * @author <a href="mailto:cmatei@untangle.com">Catalin Matei</a>
 * @version 1.0
 */
public interface LanguageManager
{
    /**
     * Get the settings.
     *
     * @return the settings.
     */
	LanguageSettings getLanguageSettings();

    /**
     * Set the settings.
     *
     * @param langSettings the settings.
     */
    void setLanguageSettings(LanguageSettings langSettings);
    
    /**
     * Upload New Language Pack
     * 
     * @return true if the uploaded language pack was processed with no errors; otherwise returns false 
     */
    boolean uploadLanguagePack(FileItem item) throws UvmException;
    
    
    /**
     * Get list of available languages
     */
    List<LocaleInfo> getLanguagesList();
    
    /**
     * Return the map of translations for a module, for the current language
     * 
     * @param module  the name of the module
     * @return map of translations 
     */
    Map<String, String> getTranslations(String module);
}
