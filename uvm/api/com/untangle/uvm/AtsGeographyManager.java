package com.untangle.uvm;

/**
 * Combined Safe and additional ATS operations.
 */
public interface AtsGeographyManager extends SafeGeographyManager
{
    /**
     * getCountryCode through the additional ATS contract.
     * @param netAddress The netAddress argument
     * @return The delegated result
     */
    String getCountryCode(String netAddress);

    /**
     * getCountryName through the additional ATS contract.
     * @param netAddress The netAddress argument
     * @return The delegated result
     */
    String getCountryName(String netAddress);
}
