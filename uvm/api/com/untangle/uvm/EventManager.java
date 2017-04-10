/**
 * $Id: ReportsManager.java,v 1.00 2015/03/04 13:45:51 dmorris Exp $
 */
package com.untangle.uvm;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.event.EventSettings;

import org.json.JSONObject;

/**
 * The API for interacting/viewing/editing events
 */
public interface EventManager
{
    public EventSettings getSettings();

    public JSONObject getClassFields();

    public void setSettings( EventSettings newSettings );

    public void logEvent( LogEvent evt );
}
