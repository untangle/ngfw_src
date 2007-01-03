/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.mvvm.reporting;

import java.lang.Integer;
import java.sql.*;
import java.util.ArrayList;
import java.util.Calendar;

import org.apache.log4j.Logger;

public class Settings
{
    private final Logger logger = Logger.getLogger(getClass());

    private static final int NONE = -1; // value must match Schedule.NONE

    private final boolean emailDetail;
    private final boolean daily;
    private final boolean weekly;
    private final boolean monthly;

    private final ArrayList<Integer> weeklyList;
    private final boolean monthlyNDaily;
    private final int monthlyNDayOfWk;
    private final boolean monthlyNFirst;

    private static final String EMAIL_DETAIL =
     "SELECT email_detail " +
       "FROM settings.tr_reporting_settings settings " +
         "JOIN settings.transform_persistent_state tstate " +
           "ON (settings.tid = tstate.tid " +
             "AND name = 'reporting-transform' " +
             "AND target_state = 'running')";

    private static final String SCHED_DAILY_MONTHLY =
     "SELECT daily, monthly_n_daily, monthly_n_day_of_wk, monthly_n_first " +
       "FROM settings.tr_reporting_sched sched " +
         "JOIN settings.tr_reporting_settings settings " +
           "ON (sched.id = settings.schedule) " +
         "JOIN settings.transform_persistent_state tstate " +
           "ON (settings.tid = tstate.tid " +
             "AND name = 'reporting-transform' " +
             "AND target_state = 'running')";

    private static final String SCHED_WEEKLY =
     "SELECT day " +
       "FROM settings.tr_reporting_wk_sched_rule wksched_rule " +
         "JOIN settings.tr_reporting_wk_sched wksched " +
           "ON (wksched_rule.id = wksched.rule_id) " +
         "JOIN settings.tr_reporting_sched sched " +
           "ON (wksched.setting_id = sched.id) " +
         "JOIN settings.tr_reporting_settings settings " +
           "ON (sched.id = settings.schedule) " +
         "JOIN settings.transform_persistent_state tstate " +
           "ON (settings.tid = tstate.tid " +
             "AND name = 'reporting-transform' " +
             "AND target_state = 'running')";

    public Settings(Connection conn, Calendar cal) {
        PreparedStatement ps;
        ResultSet rs;

        boolean ed;
        boolean dy;
        ArrayList<Integer> wyL = new ArrayList<Integer>();
        boolean mdy;
        int mwy;
        boolean mft;

        try {
            ps = conn.prepareStatement(EMAIL_DETAIL);
            rs = ps.executeQuery();
            rs.first();
            ed = rs.getBoolean(1);
            rs.close();
            ps.close();

            ps = conn.prepareStatement(SCHED_DAILY_MONTHLY);
            rs = ps.executeQuery();
            rs.first();
            dy = rs.getBoolean(1);
            mdy = rs.getBoolean(2);
            mwy = rs.getInt(3);
            mft = rs.getBoolean(4);
            rs.close();
            ps.close();

            ps = conn.prepareStatement(SCHED_WEEKLY);
            rs = ps.executeQuery();
            if (true == rs.first()) {
                Integer dayOfWk;
                do {
                   dayOfWk = new Integer(rs.getInt(1));
                   //logger.info("day of week: " + dayOfWk);
                   wyL.add(dayOfWk);
                } while (true == rs.next());
            }
            rs.close();
            ps.close();
        } catch (SQLException exn) {
            logger.error("Could not get JDBC connection", exn);
            // set to defaults
            ed = false;
            dy = true;
            wyL.clear();
            wyL.add(Calendar.SUNDAY);
            wyL.add(Calendar.MONDAY);
            wyL.add(Calendar.TUESDAY);
            wyL.add(Calendar.WEDNESDAY);
            wyL.add(Calendar.THURSDAY);
            wyL.add(Calendar.FRIDAY);
            wyL.add(Calendar.SATURDAY);
            mdy = false;
            mwy = NONE;
            mft = true;
        }

        emailDetail = ed;
        daily = dy;
        weeklyList = wyL;
        monthlyNDaily = mdy;
        monthlyNDayOfWk = mwy;
        monthlyNFirst = mft;

        weekly = getWeekly(cal);
        monthly = getMonthly(cal);

        //logger.info("reporting schedule: weekly list: " + weeklyList + ", monthly daily: " + monthlyNDaily + ", monthly weekly: " + monthlyNDayOfWk + ", monthly first: " + monthlyNFirst);
        logger.info("reporting schedule: email detail: " + emailDetail + ", daily: " + daily + ", weekly: " + weekly + ", monthly: " + monthly);
    }

    public boolean getEmailDetail() {
        return emailDetail;
    }

    public boolean getDaily() {
        return daily;
    }

    public boolean getWeekly() {
        return weekly;
    }

    public boolean getMonthly() {
        return monthly;
    }

    private boolean getWeekly(Calendar cal) {
        // com.untangle.tran.reporting.Schedule uses Calendar constants
        int dayOfWk = cal.get(Calendar.DAY_OF_WEEK);
        return weeklyList.contains(new Integer(dayOfWk));
    }

    private boolean getMonthly(Calendar cal) {
        if (true == getMonthlyNDaily()) {
            return true;
        } else if (true == getMonthlyNDayOfWk(cal)) {
            return true;
        } else if (true == getMonthlyNFirst(cal)) {
            return true;
        }

        return false;
    }

    private boolean getMonthlyNDaily() {
        return monthlyNDaily;
    }

    private boolean getMonthlyNDayOfWk(Calendar cal) {
        if (NONE == monthlyNDayOfWk) {
            return false;
        }
        // com.untangle.tran.reporting.Schedule uses Calendar constants
        int dayOfWk = cal.get(Calendar.DAY_OF_WEEK);
        return (monthlyNDayOfWk == dayOfWk) ? true : false;
    }

    private boolean getMonthlyNFirst(Calendar cal) {
        if (false == monthlyNFirst) {
            return false;
        }
        int dayOfWk = cal.get(Calendar.DAY_OF_MONTH);
        return (1 == dayOfWk) ? true : false;
    }
}
