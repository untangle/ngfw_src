/**
 * $Id$
 */
package com.untangle.uvm;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * the System Manager API
 */
public interface SystemManager
{
    SystemSettings getSettings();

    void setSettings(SystemSettings settings);

    /**
     * Returns the time zone that the UVM is currently set to
     */
    TimeZone getTimeZone();

    /**
     * Sets the time zone that the UVM is in.
     */
    void setTimeZone(TimeZone timezone);

    /**
     * Returns the current time that the UVM is set to
     * Returned as a string to avoid any browser interpretation (with its own timezone)
     */
    String getDate();

    /**
     * Returns the current time in milliseconds that the UVM is set to
     */
    long getMilliseconds();

    void setDate(long timestamp);

    /**
     * returns the current Calendar
     * when the timezone is changed all calendars must be recreated.
     * Calendars are expensive to create so this provides a global one
     */
    Calendar getCalendar();
    
    String getTimeZones();

    Integer getTimeZoneOffset();

    void setTimeSource();

    boolean downloadUpgrades();

    org.json.JSONObject getDownloadStatus();

    boolean upgradesAvailable();

    boolean upgradesAvailable( boolean forceUpdate );

    void upgrade();

    void activateApacheCertificate();

    void activateRadiusCertificate();
}
