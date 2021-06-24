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
     * Called to apply OEM overrides to default settings
     */
    public Object applyOemOverrides(Object argSettings);
}
