package com.untangle.uvm;

/**
 * Combined Safe and additional ATS operations.
 */
public interface AtsSystemManager extends SafeSystemManager
{
    /**
     * getEncryptedPassword through the additional ATS contract.
     * @param password The password argument
     * @return The delegated result
     */
    String getEncryptedPassword(String password);
}
