/**
 * $Id: DashboardManager.java,v 1.00 2015/11/10 14:36:24 dmorris Exp $
 */
package com.untangle.uvm;

/**
 * the Dashboard Manager API
 */
public interface DashboardManager
{
    DashboardSettings getSettings();

    void setSettings(DashboardSettings settings);

    DashboardSettings getSettingsV2();

    void setSettingsV2(DashboardSettings settings);

    void resetSettingsToDefault();
}
