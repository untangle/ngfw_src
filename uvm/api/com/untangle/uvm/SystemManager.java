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

    String getPublicUrl();
}
