/**
 * $Id$
 */
package com.untangle.node.reporting;

import java.io.Serializable;

/**
 * Weekly schedule rule for the Reporting Node.
 */
@SuppressWarnings("serial")
public class WeeklyScheduleRule implements Serializable
{
    private Long id;
    private int day; // day of week: SUNDAY, MONDAY, ... SATURDAY

    public WeeklyScheduleRule() { }

    public WeeklyScheduleRule(int day)
    {
        this.day = day;
    }

    private Long getId()
    {
        return id;
    }

    private void setId(Long id)
    {
        this.id = id;
    }

    /**
     * Day of week (if day is specified, then day is live)
     *
     * @return day of week
     */
    public int getDay()
    {
        return day;
    }

    public void setDay(int day)
    {
        this.day = day;
    }
}
