/**
 * $Id$
 */
package com.untangle.uvm;

import java.io.Serializable;
import org.json.JSONObject;
import org.json.JSONString;
/**
 * Uvm language settings.
 */
@SuppressWarnings("serial")
public class LanguageSettings implements Serializable, JSONString
{
    private String language = "en";
    private String source = "official";
    private long lastSynchronized = 0;
    private String regionalFormats = "default";

    private String overrideDecimalSep = "";
    private String overrideThousandSep = "";
    private String overrideDateFmt = "";
    private String overrideTimestampFmt = "";

    public LanguageSettings() { }

    public String getLanguage(){ return language; }
    public void setLanguage(String language){ this.language = language;}

    public String getSource(){ return source; }
    public void setSource(String source){ this.source = source;}

    public long getLastSynchronized(){ return lastSynchronized; }
    public void setLastSynchronized(long lastSynchronized){ this.lastSynchronized = lastSynchronized;}

    public String getRegionalFormats(){ return regionalFormats; }
    public void setRegionalFormats(String newValue){ this.regionalFormats = newValue;}

    public String getOverrideDecimalSep(){ return overrideDecimalSep; }
    public void setOverrideDecimalSep(String newValue){ this.overrideDecimalSep = newValue;}

    public String getOverrideThousandSep(){ return overrideThousandSep; }
    public void setOverrideThousandSep(String newValue){ this.overrideThousandSep = newValue;}

    public String getOverrideDateFmt(){ return overrideDateFmt; }
    public void setOverrideDateFmt(String newValue){ this.overrideDateFmt = newValue;}

    public String getOverrideTimestampFmt(){ return overrideTimestampFmt; }
    public void setOverrideTimestampFmt(String newValue){ this.overrideTimestampFmt = newValue;}

    public void copy(LanguageSettings settings)
    {
        settings.setLanguage(this.language);
    }

    /**
     * Convert settings to JSON string.
     *
     * @return
     *      JSON string.
     */
    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
