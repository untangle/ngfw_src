/**
 * $Id: Schedule.java,v 1.00 2012/05/09 15:38:15 dmorris Exp $
 */
package com.untangle.node.reporting;

import java.io.Serializable;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

/**
 * Schedule for the Reporting Node.
 */
@SuppressWarnings("serial")
public class Schedule implements Serializable
{

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
    private List<WeeklyScheduleRule> weeklySched;
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
        weeklySched = new LinkedList<WeeklyScheduleRule>();
        WeeklyScheduleRule weeklySR = new WeeklyScheduleRule(SUNDAY);
        weeklySched.add(weeklySR);
        //weeklySR = new WeeklyScheduleRule(MONDAY);
        //weeklySched.add(weeklySR);
        //weeklySR = new WeeklyScheduleRule(TUESDAY);
        //weeklySched.add(weeklySR);
        //weeklySR = new WeeklyScheduleRule(WEDNESDAY);
        //weeklySched.add(weeklySR);
        //weeklySR = new WeeklyScheduleRule(THURSDAY);
        //weeklySched.add(weeklySR);
        //weeklySR = new WeeklyScheduleRule(FRIDAY);
        //weeklySched.add(weeklySR);
        //weeklySR = new WeeklyScheduleRule(SATURDAY);
        //weeklySched.add(weeklySR);

        // create monthly reports only on 1st of month
        monthlyNDaily = false;
        monthlyNDayOfWk = NONE;
        monthlyNFirst = false;
    }

    private Long getId() { return id; }
    private void setId( Long id ) { this.id = id; }

    /**
     * daily schedule for daily report
     *
     * @return daily schedule
     */
    public boolean getDaily() { return daily; }
    public void setDaily( boolean daily ) { this.daily = daily; }

    /**
     * weekly schedule for weekly report (list of day(s) of week)
     * (if day of week is not present in list,
     *  weekly report is not created on that day)
     * - valid days:
     *   SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY
     */
    public List<WeeklyScheduleRule> getWeeklySched()
    {
        if (weeklySched != null) weeklySched.removeAll(java.util.Collections.singleton(null));
        return weeklySched;
    }

    public void setWeeklySched(List<WeeklyScheduleRule> weeklySched)
    {
        this.weeklySched = weeklySched;
    }

    /**
     * daily schedule for monthly report
     * (if every day, then not weekly and not on first of month)
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
