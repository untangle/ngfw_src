package com.untangle.uvm;

/**
 * Combined Safe and additional ATS operations.
 */
public interface AtsAppManager extends SafeAppManager
{
    /**
     * isInstantiated through the additional ATS contract.
     * @param appName The appName argument
     * @return The delegated result
     */
    boolean isInstantiated(String appName);
}
