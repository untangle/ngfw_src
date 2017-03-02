/**
 * $Id$
 */
package com.untangle.node.openvpn;

public class OpenVpnConfigItem
{
    private String optionName;
    private String defaultValue;
    private String customValue;
    private boolean defaultFlag;
    private boolean excludeFlag;

    public OpenVpnConfigItem()
    {
    }

    public OpenVpnConfigItem(String optionName, boolean isDefault)
    {
        this.optionName = optionName;
        this.defaultValue = null;
        this.customValue = null;
        this.defaultFlag = isDefault;
        this.excludeFlag = false;
    }

    public OpenVpnConfigItem(String optionName, String argValue, boolean isDefault)
    {
        this.optionName = optionName;
        
        if (isDefault) {
            this.defaultValue = argValue;
            this.customValue = null;
        } else {
            this.defaultValue = null;
            this.customValue = argValue;
        }

        this.customValue = null;
        this.defaultFlag = isDefault;
        this.excludeFlag = false;
    }

    // THIS IS FOR ECLIPSE - @formatter:off

    public String getOptionName() { return optionName; }
    public void setOptionName(String argValue) { optionName = argValue; }
    
    public String getDefaultValue() { return defaultValue; }
    public void setDefaultValue(String argValue) { defaultValue = argValue; }
    
    public String getCustomValue() { return customValue; }
    public void setCustomValue(String argValue) { customValue = argValue; }
    
    public boolean getDefaultFlag() { return defaultFlag; }
    public void setDefaultFlag(boolean argFlag) { defaultFlag = argFlag; }
    
    public boolean getExcludeFlag() { return excludeFlag; }
    public void setExcludeFlag(boolean argFlag) { excludeFlag = argFlag; }

// THIS IS FOR ECLIPSE - @formatter:on

    public String toString()
    {
        if (excludeFlag == true) return (null);
        if (customValue != null) return (optionName + " " + customValue);
        if (defaultValue == null) return (optionName);
        return (optionName + " " + defaultValue);
    }
}
