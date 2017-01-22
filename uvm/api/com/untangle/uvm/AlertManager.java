/**
 * $Id: ReportsManager.java,v 1.00 2015/03/04 13:45:51 dmorris Exp $
 */
package com.untangle.uvm;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.alert.AlertSettings;

/**
 * The API for interacting/viewing/editing alerts
 */
public interface AlertManager
{
    public AlertSettings getSettings();

    public void setSettings( AlertSettings newSettings );

    public void logEvent( LogEvent evt );

}
