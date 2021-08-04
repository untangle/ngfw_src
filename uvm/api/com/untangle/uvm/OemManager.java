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
     * If oem override file exists
     */
    public boolean hasOemOverrideFile();

    /**
     * Called to apply OEM overrides to default settings
     */
    public Object applyOemOverrides(Object argSettings);

    /**
     * Return the license agreement
     */
    public String getLicenseAgreementUrl();

    /**
     * Return if should use local license agreement
     */
    public Boolean getUseLocalEula();
}
