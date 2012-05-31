/**
 * $Id: LanguageSettings.java,v 1.00 2012/05/09 15:56:38 dmorris Exp $
 */
package com.untangle.uvm;

import java.io.Serializable;

/**
 * Uvm language settings.
 */
@SuppressWarnings("serial")
public class LanguageSettings implements Serializable
{
    private String language = "en";

    public LanguageSettings() { }

	public String getLanguage()
    {
		return language;
	}

	public void setLanguage(String language)
    {
		this.language = language;
	}

    public void copy(LanguageSettings settings)
    {
        settings.setLanguage(this.language);
    }
}
