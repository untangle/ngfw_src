/**
 * $Id$
 */
package com.untangle.app.openvpn;

public class OpenVpnConfigItem
{
    private String optionName;
    private String optionValue;
    private boolean excludeFlag;
    private boolean readOnly;

    public OpenVpnConfigItem()
    {
    }

    public OpenVpnConfigItem(String argName, boolean argFlag)
    {
        this.optionName = argName;
        this.optionValue = null;
        this.excludeFlag = false;
        this.readOnly = argFlag;
    }

    public OpenVpnConfigItem(String argName, String argValue, boolean argFlag)
    {
        this.optionName = argName;
        this.optionValue = argValue;
        this.excludeFlag = false;
        this.readOnly = argFlag;
    }

// THIS IS FOR ECLIPSE - @formatter:off

    public String getOptionName() { return optionName; }
    public void setOptionName(String argValue) { optionName = argValue; }
    
    public String getOptionValue() { return optionValue; }
    public void setOptionValue(String argValue) { optionValue = argValue; }
    
    public boolean getExcludeFlag() { return excludeFlag; }
    public void setExcludeFlag(boolean argFlag) { excludeFlag = argFlag; }

    public boolean getReadOnly() { return readOnly; }
    public void setReadOnly(boolean argFlag) { readOnly = argFlag; }

// THIS IS FOR ECLIPSE - @formatter:on

    public String generateConfigString()
    {
        if (excludeFlag == true) return (null);

        if (optionName == null) return(null);
        if (optionName.length() == 0) return(null);
        
        if (optionValue == null) return (optionName);
        if (optionValue.length() == 0) return(optionName);

        return (optionName + " " + optionValue);
    }
}
