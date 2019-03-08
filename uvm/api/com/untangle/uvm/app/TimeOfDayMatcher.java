/**
 * $Id$
 */

package com.untangle.uvm.app;

import java.util.LinkedList;
import java.util.Date;
import java.util.Calendar;
import java.text.SimpleDateFormat;

import org.apache.log4j.Logger;

/**
 * An matcher for times of the day
 *
 * Examples:
 * "any"
 * "13:30"
 * "11:00-14:00"
 * "13:30,11:00-14:00"
 * "4:00-2:00" (this matches everything but 2:00-4:00)
 *
 * TimeOfDayMatcher it is case insensitive
 */
public class TimeOfDayMatcher
{
    private static final SimpleDateFormat TIME_FORMATTER = new SimpleDateFormat("H:m");

    private static final String MARKER_ANY = "any";
    private static final String MARKER_NONE = "none";
    private static final String MARKER_SEPERATOR = ",";
    private static final String MARKER_RANGE = "-";

    private static TimeOfDayMatcher ANY_MATCHER = new TimeOfDayMatcher(MARKER_ANY);

    private final Logger logger = Logger.getLogger(getClass());

    private enum TimeOfDayMatcherType { ANY, NONE, SINGLE, RANGE, LIST };

    /**
     * The type of this matcher
     */
    private TimeOfDayMatcherType type = TimeOfDayMatcherType.NONE;

    /**
     * This stores the string representation of this matcher
     */
    private String matcher;

    /**
     * This stores the string of this representation of this single matcher
     */
    private Date single;

    /**
     * This stores the min and max for range matchers
     */
    private Date rangeMin = null;
    private Date rangeMax = null;
    
    /**
     * if this port matcher is a list of port matchers, this list stores the children
     */
    private LinkedList<TimeOfDayMatcher> children = null;
    
    /**
     * Construct a day of week matcher from the given string
     * @param matcher The init string
     */
    public TimeOfDayMatcher( String matcher )
    {
        initialize(matcher);
    }

    /**
     * Check if matches current time
     * 
     * @return isMatch(now())
     */
    public boolean isMatch()
    {
        return isMatch(new Date());
    }
    
    /**
     * Check if matches argumented date
     * @param when The date
     * @return True for match, otherwise false
     */
    public boolean isMatch( Date when )
    {
        Calendar now = Calendar.getInstance();
        int nowHour;
        int nowMinu;

        switch (this.type) {

        case ANY:
            return true;

        case NONE:
            return false;

        case SINGLE:
            Calendar singleCal = Calendar.getInstance();

            singleCal.setTime(this.single);
            singleCal.set(Calendar.YEAR, now.get(Calendar.YEAR));
            singleCal.set(Calendar.MONTH, now.get(Calendar.MONTH));
            singleCal.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH));

            nowHour = now.get(Calendar.HOUR_OF_DAY);
            nowMinu  = now.get(Calendar.MINUTE);
            int singleHour = singleCal.get(Calendar.HOUR_OF_DAY);
            int singleMin  = singleCal.get(Calendar.MINUTE);

            if (nowHour == singleHour && nowMinu == singleMin)
                return true;
            return false;

        case RANGE:
            boolean invertDuration = false;
            Calendar rangeMinCal = Calendar.getInstance();
            Calendar rangeMaxCal = Calendar.getInstance();
            
            rangeMinCal.setTime(this.rangeMin);
            rangeMinCal.set(Calendar.YEAR, now.get(Calendar.YEAR));
            rangeMinCal.set(Calendar.MONTH, now.get(Calendar.MONTH));
            rangeMinCal.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH));

            rangeMaxCal.setTime(this.rangeMax);
            rangeMaxCal.set(Calendar.YEAR, now.get(Calendar.YEAR));
            rangeMaxCal.set(Calendar.MONTH, now.get(Calendar.MONTH));
            rangeMaxCal.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH));

            if (rangeMaxCal.before(rangeMinCal)) {
                Calendar temp = rangeMaxCal;
                rangeMaxCal = rangeMinCal;
                rangeMinCal = temp;
                rangeMinCal.add(Calendar.MINUTE, 1);
                rangeMaxCal.add(Calendar.MINUTE, -1);
                invertDuration = true;
            }

            nowHour = now.get(Calendar.HOUR_OF_DAY);
            nowMinu  = now.get(Calendar.MINUTE);
            int rangeMinHour = rangeMinCal.get(Calendar.HOUR_OF_DAY);
            int rangeMinMinu = rangeMinCal.get(Calendar.MINUTE);
            int rangeMaxHour = rangeMaxCal.get(Calendar.HOUR_OF_DAY);
            int rangeMaxMinu = rangeMaxCal.get(Calendar.MINUTE);

            // Check the start
            boolean beforeMin = (nowHour < rangeMinHour || (nowHour == rangeMinHour && nowMinu < rangeMinMinu));
            // Check the end.
            boolean afterMax = (nowHour > rangeMaxHour || (nowHour == rangeMaxHour && nowMinu > rangeMaxMinu));

            if ( beforeMin || afterMax )
                return false ^ invertDuration;
            else
                return true ^ invertDuration;
            
        case LIST:
            for (TimeOfDayMatcher child : this.children) {
                if (child.isMatch(when))
                    return true;
            }
            return false;

        default:
            logger.warn("Unknown port matcher type: " + this.type);
            return false;
        }
    }

    /**
     * Get the string representation
     * @return The string representation
     */
    public String toString()
    {
        return this.matcher;
    }

    /**
     * Get the any matcher
     * @return The any matcher
     */
    public static TimeOfDayMatcher getAnyMatcher()
    {
        return ANY_MATCHER;
    }
    
    /**
     * Initialize
     * @param matcher The init string
     */
    private void initialize( String matcher )
    {
        matcher = matcher.toLowerCase().trim().replaceAll("\\s","");
        this.matcher = matcher;

        /**
         * If it contains a comma it must be a list of protocol matchers
         * if so, go ahead and initialize the children
         */
        if (matcher.contains(MARKER_SEPERATOR)) {
            this.type = TimeOfDayMatcherType.LIST;

            this.children = new LinkedList<TimeOfDayMatcher>();

            String[] results = matcher.split(MARKER_SEPERATOR);
            
            /* check each one */
            for (String childString : results) {
                TimeOfDayMatcher child = new TimeOfDayMatcher(childString);
                this.children.add(child);
            }

            return;
        }

        /**
         * Check the common constants
         */
        if (MARKER_ANY.equals(matcher))  {
            this.type = TimeOfDayMatcherType.ANY;
            return;
        }
        if (MARKER_NONE.equals(matcher)) {
            this.type = TimeOfDayMatcherType.NONE;
            return;
        }

        if (matcher.contains(MARKER_RANGE)) {
            this.type = TimeOfDayMatcherType.RANGE;
            String[] results = matcher.split(MARKER_RANGE);
            if (results.length != 2) {
                logger.warn("Ignoring invalid range: " + matcher);
                this.type = TimeOfDayMatcherType.ANY;
                return;
            }
            String rangeMinStr = results[0];
            String rangeMaxStr = results[1];
            try {
                synchronized (TIME_FORMATTER) {
                    this.rangeMin = TIME_FORMATTER.parse(rangeMinStr);
                    this.rangeMax = TIME_FORMATTER.parse(rangeMaxStr);
                }
            } catch (Exception e) {
                logger.warn("Invalid time of day matcher string (" + rangeMinStr + "-" + rangeMaxStr + ")",e);
                this.type = TimeOfDayMatcherType.ANY;
                return;
            }
            return;
        }

        /**
         * Else it must be a single
         */
        try {
            this.type = TimeOfDayMatcherType.SINGLE;
            synchronized (TIME_FORMATTER) {
                this.single = TIME_FORMATTER.parse( matcher );
            }
        } catch (Exception e) {
            logger.warn("Invalid time of day matcher string (" + matcher + ")",e);
            this.type = TimeOfDayMatcherType.ANY;
            return;
        }
    }
}
