/**
 * $Id$
 */
package com.untangle.uvm;

import java.io.Serializable;
import java.util.Calendar;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Time specification for scheduling tasks.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@Table(name="u_period", schema="settings")
@SuppressWarnings("serial")
public class Period implements Serializable
{

    private Long id;

    private int hour;
    private int minute;

    private boolean sunday = false;
    private boolean monday = false;
    private boolean tuesday = false;
    private boolean wednesday = false;
    private boolean thursday = false;
    private boolean friday = false;
    private boolean saturday = false;

    // constructors -----------------------------------------------------------

    public Period() { }

    /**
     * Create new Period set to particular time.
     *
     * @param hour hour of the day, in 24 hour time.
     * @param minute minute of the hour.
     * @param allDays true if scheduled daily.
     */
    public Period(int hour, int minute, boolean allDays)
    {
        this.hour = hour;
        this.minute = minute;

        if (allDays) {
            monday = tuesday = wednesday = thursday = friday = saturday
                = sunday = true;
        }
    }

    // business methods -------------------------------------------------------

    /**
     * Get the next time for this period.
     *
     * @return next time, null if never.
     */
    public Calendar nextTime()
    {
        Calendar c = Calendar.getInstance();

        if (c.get(Calendar.HOUR_OF_DAY) > hour) {
            c.add(Calendar.DAY_OF_WEEK, 1);
        }

        if (c.get(Calendar.HOUR_OF_DAY) == hour
            && c.get(Calendar.MINUTE) >= minute) {
            c.add(Calendar.DAY_OF_WEEK, 1);
        }

        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);

        for (int i = 0; i < 7; i++) {
            if (includesDay(c.get(Calendar.DAY_OF_WEEK))) {
                return c;
            } else {
                c.add(Calendar.DAY_OF_WEEK, 1);
            }
        }

        return null;
    }

    /**
     * Tests if Period includes a particular day.
     *
     * @param day as defined by {@link java.util.Calendar} statics.
     * @return true if the day is int the period, false otherwise.
     */
    public boolean includesDay(int day)
    {
        switch (day) {
        case Calendar.SUNDAY:
            return sunday;
        case Calendar.MONDAY:
            return monday;
        case Calendar.TUESDAY:
            return tuesday;
        case Calendar.WEDNESDAY:
            return wednesday;
        case Calendar.THURSDAY:
            return thursday;
        case Calendar.FRIDAY:
            return friday;
        case Calendar.SATURDAY:
            return saturday;
        default:
            return false;
        }
    }

    // accessors --------------------------------------------------------------

    @Id
    @Column(name="period_id")
    @GeneratedValue
    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    /**
     * Hour of update.
     */
    @Column(nullable=false)
    public int getHour()
    {
        return hour;
    }

    public void setHour(int hour)
    {
        this.hour = hour;
    }

    /**
     * Minute of update.
     *
     * @return the minute.
     */
    @Column(nullable=false)
    public int getMinute()
    {
        return minute;
    }

    public void setMinute(int minute)
    {
        this.minute = minute;
    }

    /**
     * Includes Sunday.
     *
     * @return true if it happens on Sunday.
     */
    @Column(nullable=false)
    public boolean getSunday()
    {
        return sunday;
    }

    public void setSunday(boolean sunday)
    {
        this.sunday = sunday;
    }

    /**
     * Includes Monday.
     *
     * @return true if it happens on Monday.
     */
    @Column(nullable=false)
    public boolean getMonday()
    {
        return monday;
    }

    public void setMonday(boolean monday)
    {
        this.monday = monday;
    }

    /**
     * Includes Tuesday.
     *
     * @return true if it happens on Tuesday.
     */
    @Column(nullable=false)
    public boolean getTuesday()
    {
        return tuesday;
    }

    public void setTuesday(boolean tuesday)
    {
        this.tuesday = tuesday;
    }

    /**
     * Includes Wednesday.
     *
     * @return true if it happens on Wednesday.
     */
    @Column(nullable=false)
    public boolean getWednesday()
    {
        return wednesday;
    }

    public void setWednesday(boolean wednesday)
    {
        this.wednesday = wednesday;
    }

    /**
     * Includes Thursday.
     *
     * @return true if it happens on Thursday.
     */
    @Column(nullable=false)
    public boolean getThursday()
    {
        return thursday;
    }

    public void setThursday(boolean thursday)
    {
        this.thursday = thursday;
    }

    /**
     * Includes Friday.
     *
     * @return true if it happens on Friday.
     */
    @Column(nullable=false)
    public boolean getFriday()
    {
        return friday;
    }

    public void setFriday(boolean friday)
    {
        this.friday = friday;
    }

    /**
     * Includes Saturday.
     *
     * @return true if it happens on Saturday.
     */
    @Column(nullable=false)
    public boolean getSaturday()
    {
        return saturday;
    }

    public void setSaturday(boolean saturday)
    {
        this.saturday = saturday;
    }
}
