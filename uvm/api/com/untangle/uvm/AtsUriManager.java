package com.untangle.uvm;

/**
 * Combined Safe and additional ATS operations.
 */
public interface AtsUriManager extends SafeUriManager
{
    /**
     * setSettings through the additional ATS contract.
     * @param settings The settings argument
     */
    void setSettings(UriManagerSettings settings);
}
