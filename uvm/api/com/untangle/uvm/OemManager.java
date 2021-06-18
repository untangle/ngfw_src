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
     * Return the OEM license agreement
     */
    public String getOemLicenseAgreementUrl();

    /**
     * Return if is oem
     */
    public Boolean getIsOem();
}
