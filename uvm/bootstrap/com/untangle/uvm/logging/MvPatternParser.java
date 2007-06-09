/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id: MvPatternLayout.java 8515 2007-01-03 00:13:24Z amread $
 */

package com.untangle.uvm.logging;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.Calendar;
import java.util.Date;

import org.apache.log4j.helpers.FormattingInfo;
import org.apache.log4j.helpers.PatternConverter;
import org.apache.log4j.helpers.PatternParser;
import org.apache.log4j.spi.LoggingEvent;

/**
 * Adds the 'D' conversion to {@link
 * org.apache.log4j.helpers.PatternParser}, which outputs the log date
 * formatted as: 'dd HH:mm:ss,SSS'.
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
 */
class MvPatternParser extends PatternParser
{
    // constructors -----------------------------------------------------------

    public MvPatternParser(String pattern)
    {
        super(pattern);
    }

    // PatternParser methods --------------------------------------------------

    @Override
    public void finalizeConverter(char c)
    {
        if ('D' == c) {
            DateFormat df = new  MvTimeDateFormat();
            addConverter(new MvDatePatternConverter(formattingInfo, df));
            currentLiteral.setLength(0);
        } else {
            super.finalizeConverter(c);
        }
    }

    // static classes ---------------------------------------------------------

    /**
     * PatternConverter for our date conversion.
     */
    private static class MvDatePatternConverter extends PatternConverter
    {
        private final DateFormat df;
        private final Date date = new Date();

        MvDatePatternConverter(FormattingInfo formattingInfo, DateFormat df)
        {
            super(formattingInfo);

            this.df = df;
        }

        // PatternConverter methods -------------------------------------------

        @Override
        public String convert(LoggingEvent event)
        {
            date.setTime(event.timeStamp);
            return df.format(date);
        }
    }

    /**
     * Formats time as: 'dd HH:mm:ss,SSS'.
     */
    private static class MvTimeDateFormat extends DateFormat
    {
        // "dd HH:mm:ss."
        private static final char[] cachedTimeNoMillis = new char[12];

        private static long cachedTime;

        // constructors -------------------------------------------------------

        public MvTimeDateFormat()
        {
            setCalendar(Calendar.getInstance());
        }

        // DateFormat methods -------------------------------------------------

        @Override
        public StringBuffer format(Date date, StringBuffer sbuf,
                                   FieldPosition fieldPosition)
        {
            long now = date.getTime();
            int millis = (int)(now % 1000);

            if ((now - millis) != cachedTime) {
                calendar.setTime(date);

                int start = sbuf.length();

                int day = calendar.get(Calendar.DAY_OF_MONTH);
                if (day < 10) {
                    sbuf.append('0');
                }
                sbuf.append(day);
                sbuf.append(' ');

                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                if (hour < 10) {
                    sbuf.append('0');
                }
                sbuf.append(hour);
                sbuf.append(':');

                int mins = calendar.get(Calendar.MINUTE);
                if (mins < 10) {
                    sbuf.append('0');
                }
                sbuf.append(mins);
                sbuf.append(':');

                int secs = calendar.get(Calendar.SECOND);
                if (secs < 10) {
                    sbuf.append('0');
                }
                sbuf.append(secs);
                sbuf.append('.');

                // cache the result for later use
                sbuf.getChars(start, sbuf.length(), cachedTimeNoMillis, 0);
                cachedTime = now - millis;
            } else {
                sbuf.append(cachedTimeNoMillis);
            }

            // Now the millis.
            if (millis < 10) {
                sbuf.append("00");
            } else if (millis < 100) {
                sbuf.append('0');
            }
            sbuf.append(millis);

            return sbuf;
        }

        @Override
        public Date parse(String s, ParsePosition pos)
        {
            return null;
        }
    }
}
