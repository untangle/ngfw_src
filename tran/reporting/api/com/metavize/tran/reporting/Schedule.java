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
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

/**
 * Schedule for the Reporting Transform.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_REPORTING_SCHED"
 */
public class Schedule implements Serializable
{
    private static final long serialVersionUID = 2064742840204258979L;

    // day of week constants
    public static final int NONE = -1; // only used with monthlyNDayOfWk
    public static final int SUNDAY = Calendar.SUNDAY;
    public static final int MONDAY = Calendar.MONDAY;
    public static final int TUESDAY = Calendar.TUESDAY;
    public static final int WEDNESDAY = Calendar.WEDNESDAY;
    public static final int THURSDAY = Calendar.THURSDAY;
    public static final int FRIDAY = Calendar.FRIDAY;
    public static final int SATURDAY = Calendar.SATURDAY;

    private Long id;
    private boolean daily; // false = no, true = yes
    private List weeklySched = new LinkedList();
    // monthlyNDaily, monthlyNDayOfWk, and monthlyNFirst are mutually exclusive
    // - enabling one will disable the other two
    private boolean monthlyNDaily; // false = no, true = yes
    private int monthlyNDayOfWk; // NONE, SUNDAY, ... SATURDAY
    private boolean monthlyNFirst; // false = no, true = yes

    public Schedule()
    {
        // create daily reports every day
        daily = true;

        // create weekly reports every day of week
        weeklySched = new LinkedList();
        WeeklyScheduleRule weeklySR = new WeeklyScheduleRule(SUNDAY);
        weeklySched.add(weeklySR);
        weeklySR = new WeeklyScheduleRule(MONDAY);
        weeklySched.add(weeklySR);
        weeklySR = new WeeklyScheduleRule(TUESDAY);
        weeklySched.add(weeklySR);
        weeklySR = new WeeklyScheduleRule(WEDNESDAY);
        weeklySched.add(weeklySR);
        weeklySR = new WeeklyScheduleRule(THURSDAY);
        weeklySched.add(weeklySR);
        weeklySR = new WeeklyScheduleRule(FRIDAY);
        weeklySched.add(weeklySR);
        weeklySR = new WeeklyScheduleRule(SATURDAY);
        weeklySched.add(weeklySR);

        // create monthly reports only on 1st of month
        monthlyNDaily = false;
        monthlyNDayOfWk = NONE;
        monthlyNFirst = true;
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
     * daily schedule for daily report
     *
     * @return daily schedule
     * @hibernate.property
     * column="DAILY"
     * not-null="true"
     */
    public boolean getDaily()
    {
        return daily;
    }

    public void setDaily(boolean daily)
    {
        this.daily = daily;
        return;
    }

    /**
     * weekly schedule for weekly report (list of day(s) of week)
     * (if day of week is not present in list,
     *  weekly report is not created on that day)
     * - valid days:
     *   SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY
     *
     * @return the weekly schedule list for reports
     * @hibernate.list
     * cascade="all-delete-orphan"
     * table="TR_REPORTING_WK_SCHED"
     * @hibernate.collection-key
     * column="SETTING_ID"
     * @hibernate.collection-index
     * column="POSITION"
     * @hibernate.collection-many-to-many
     * class="com.metavize.tran.reporting.WeeklyScheduleRule"
     * column="RULE_ID"
     */
    public List getWeeklySched()
    {
        return weeklySched;
    }

    public void setWeeklySched(List weeklySched)
    {
        this.weeklySched = weeklySched;
        return;
    }

    /* schedule for monthly report is one of the following options */

    /**
     * daily schedule for monthly report
     * (if every day, then not weekly and not on first of month)
     *
     * @return monthly_n_daily schedule
     * @hibernate.property
     * column="MONTHLY_N_DAILY"
     * not-null="true"
     */
    public boolean getMonthlyNDaily()
    {
        return monthlyNDaily;
    }

    public void setMonthlyNDaily(boolean monthlyNDaily)
    {
        this.monthlyNDaily = monthlyNDaily;
        if (true == monthlyNDaily) {
            /* deactivate other monthly schedule options */
            monthlyNDayOfWk = NONE;
            monthlyNFirst = false;
        }
        return;
    }

    /**
     * weekly schedule for monthly report
     * (if once a week, then not daily and not on first of month)
     * - valid days:
     *   NONE,
     *   SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY
     *
     * @return monthly_n_daily schedule
     * @hibernate.property
     * column="MONTHLY_N_DAY_OF_WK"
     * not-null="true"
     */
    public int getMonthlyNDayOfWk()
    {
        return monthlyNDayOfWk;
    }

    public void setMonthlyNDayOfWk(int monthlyNDayOfWk)
    {
        this.monthlyNDayOfWk = monthlyNDayOfWk;
        if (NONE != monthlyNDayOfWk) {
            /* deactivate other monthly schedule options */
            monthlyNDaily = false;
            monthlyNFirst = false;
        }
        return;
    }

    /**
     * monthly schedule for monthly report
     * (if first of month, then not daily and not weekly)
     *
     * @return monthly_n_daily schedule
     * @hibernate.property
     * column="MONTHLY_N_FIRST"
     * not-null="true"
     */
    public boolean getMonthlyNFirst()
    {
        return monthlyNFirst;
    }

    public void setMonthlyNFirst(boolean monthlyNFirst)
    {
        this.monthlyNFirst = monthlyNFirst;
        if (true == monthlyNFirst) {
            /* deactivate other monthly schedule options */
            monthlyNDaily = false;
            monthlyNDayOfWk = NONE;
        }
        return;
    }
}
