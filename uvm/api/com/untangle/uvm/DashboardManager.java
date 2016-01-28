/**
 * $Id: DashboardManager.java,v 1.00 2015/11/10 14:36:24 dmorris Exp $
 */
package com.untangle.uvm;

import java.util.List;

/**
 * the Dashboard Manager API
 */
public interface DashboardManager
{
    DashboardSettings getSettings();

    void setSettings(DashboardSettings settings);
    
    /*
     * Get a list of available widgets, Reports and Events entries are displayed only when reports are installed and enabled and their respective apps are installed.  
     */
    List<DashboardWidgetInfo> getAvailableWidgets();
}
