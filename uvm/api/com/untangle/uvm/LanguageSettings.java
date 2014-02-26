/**
 * $Id$
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
