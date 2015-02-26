/**
 * $Id$
 */
package com.untangle.uvm;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Describe interface <code>AdminManager</code> here.
 */
public interface AdminManager
{
    AdminSettings getSettings();

    void setSettings(AdminSettings settings);

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
     * returns the current Calendar
     * when the timezone is changed all calendars must be recreated.
     * Calendars are expensive to create so this provides a global one
     */
    Calendar getCalendar();
    
    String getModificationState();

    String getRebootCount();

    String getFullVersionAndRevision();

    String getKernelVersion();
    
    String getAdminEmail();

    String getTimeZones();

    Integer getTimeZoneOffset();
}
