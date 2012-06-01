/**
 * $Id: SystemManager.java,v 1.00 2011/12/08 16:37:33 dmorris Exp $
 */
package com.untangle.uvm;

/**
 * the System Manager API
 */
public interface SystemManager
{
    SystemSettings getSettings();

    void setSettings(SystemSettings settings);

    /**
     * Returns a globally accessable URL to reach Untangle services.
     * This is configured by the user in some cases where Untangle doesn't have a public IP.
     */
    String getPublicUrl();
}
