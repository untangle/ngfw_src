/**
 * $Id: ReportsManager.java,v 1.00 2015/03/04 13:45:51 dmorris Exp $
 */
package com.untangle.uvm;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.event.EventSettings;
import com.untangle.uvm.event.generic.EventSettingsGeneric;
import com.untangle.uvm.event.AlertRule;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;

/**
 * The API for interacting/viewing/editing events
 */
public interface EventManager
{
    public EventSettings getSettings();

    public EventSettingsGeneric getSettingsV2();

    public JSONObject getClassFields();

    public void setSettings( EventSettings newSettings );

    public void setSettingsV2( EventSettingsGeneric newSettings );

    public void logEvent( LogEvent evt );

    public int getEventQueueSize();

    public Map<String, Integer>  getEventsSeenCounts();

    public int getRemoteEventQueueSize();

    public JSONArray getTemplateParameters();

    public Map<String,String> emailAlertFormatPreview(AlertRule rule, LogEvent event, String subjectTemplate, String bodyTemplate, boolean convert);

    public Map<String,String> defaultEmailSettings();

    public Map<String,String> defaultEmailSettingsV2();

}
