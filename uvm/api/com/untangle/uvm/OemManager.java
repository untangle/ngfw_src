/**
 * $Id$
 */
package com.untangle.uvm;

public interface OemManager
{
    /**
     * Return the OEM Name
     */
    public String getOemName();

    /**
     * Return the OEM Url
     */
    public String getOemUrl();

    /**
     * Return the license agreement
     */
    public String getLicenseAgreementUrl();

    /**
     * Return if should use local license agreement
     */
    public Boolean getUseLocalEula();
    
}
