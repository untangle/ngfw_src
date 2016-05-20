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
    private String source = "official";
    private long lastSynchronized = 0;

    public LanguageSettings() { }

    public String getLanguage()
    {
        return language;
    }

    public void setLanguage(String language)
    {
        this.language = language;
    }

    public String getSource()
    {
        return source;
    }

    public void setSource(String source)
    {
        this.source = source;
    }

    public long getLastSynchronized()
    {
        return lastSynchronized;
    }

    public void setLastSynchronized(long lastSynchronized)
    {
        this.lastSynchronized = lastSynchronized;
    }

    public void copy(LanguageSettings settings)
    {
        settings.setLanguage(this.language);
    }
}
