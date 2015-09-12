/**
 * $Id$
 */
package com.untangle.node.configuration_backup;

import java.io.Serializable;

/**
 * Settings for the ConfigurationBackup node.
 */
@SuppressWarnings("serial")
public class ConfigurationBackupSettings implements Serializable
{
    private int hourInDay;
    private int minuteInHour;

    public ConfigurationBackupSettings() { }

    public int getHourInDay() { return hourInDay; }

    public void setHourInDay(int hour) { this.hourInDay = hour; }

    public int getMinuteInHour() { return minuteInHour; }

    public void setMinuteInHour(int mih) { this.minuteInHour = mih; }
}
