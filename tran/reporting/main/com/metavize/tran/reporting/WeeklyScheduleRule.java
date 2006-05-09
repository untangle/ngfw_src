/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.reporting;

import java.io.Serializable;

/**
 * Weekly schedule rule for the Reporting Transform.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_REPORTING_WK_SCHED_RULE"
 */
public class WeeklyScheduleRule implements Serializable
{
    private static final long serialVersionUID = 2064742840204258978L;

    private Long id;
    private int day; // day of week: SUNDAY, MONDAY, ... SATURDAY

    public WeeklyScheduleRule() { }

    public WeeklyScheduleRule(int day)
    {
        this.day = day;
    }

    /**
     * @hibernate.id
     * column="ID"
     * generator-class="native"
     */
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
     * @hibernate.property
     * column="DAY"
     * not-null="true"
     */
    public int getDay()
    {
        return day;
    }

    public void setDay(int day)
    {
        this.day = day;
        return;
    }
}
