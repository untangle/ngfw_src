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

package com.untangle.mvvm.logging;

import org.apache.log4j.*;
import org.apache.log4j.helpers.FormattingInfo;
import org.apache.log4j.helpers.PatternConverter;
import org.apache.log4j.helpers.PatternParser;
import org.apache.log4j.spi.LoggingEvent;
import java.util.Date;
import java.util.Calendar;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.DateFormat;


public class MvPatternLayout extends PatternLayout {
    public static final String MV_DEFAULT_CONVERSION_PATTERN = "%D %-5p [%c{1}] %m%n" ;

    public MvPatternLayout() {
        this(MV_DEFAULT_CONVERSION_PATTERN);
    }

    public MvPatternLayout(String pattern) {
        super(pattern);
    }

    @Override
    public PatternParser createPatternParser(String pattern) {
        return new MvPatternParser(pattern == null ? MV_DEFAULT_CONVERSION_PATTERN : pattern);
    }

    private static class MvPatternParser extends PatternParser {

        int counter = 0;

        public MvPatternParser(String pattern) {
            super(pattern);
        }
    
        public void finalizeConverter(char c) {
            if (c == 'D') {
                DateFormat df = new  MvTimeDateFormat();
                addConverter(new MvDatePatternConverter(formattingInfo, df));
                currentLiteral.setLength(0);
            } else {
                super.finalizeConverter(c);
            }
        }

        private static class MvDatePatternConverter extends PatternConverter {
            private DateFormat df;
            private Date date = new Date();

            MvDatePatternConverter(FormattingInfo formattingInfo, DateFormat df) {
                super(formattingInfo);
                this.df = df;
            }

            public String convert(LoggingEvent event) {
                date.setTime(event.timeStamp);
                return df.format(date);
            }
        }

        /**
         * Formats time as:
         *   dd HH:mm:ss,SSS
         *
         */
        private static class MvTimeDateFormat extends DateFormat {
            public MvTimeDateFormat() {
                setCalendar(Calendar.getInstance());
            }

            // Check for synchonization need. XXX

            private static long   cachedTime;
            private static char[] cachedTimeNoMillis = new char[12]; // "dd HH:mm:ss."

            public StringBuffer format(Date date, StringBuffer sbuf, FieldPosition fieldPosition) {
                long now = date.getTime();
                int millis = (int)(now % 1000);

                if ((now - millis) != cachedTime) {
                    calendar.setTime(date);

                    int start = sbuf.length();
      
                    int day = calendar.get(Calendar.DAY_OF_MONTH);
                    if (day < 10)
                        sbuf.append('0');
                    sbuf.append(day);
                    sbuf.append(' ');

                    int hour = calendar.get(Calendar.HOUR_OF_DAY);
                    if (hour < 10)
                        sbuf.append('0');
                    sbuf.append(hour);
                    sbuf.append(':');
      
                    int mins = calendar.get(Calendar.MINUTE);
                    if (mins < 10)
                        sbuf.append('0');
                    sbuf.append(mins);
                    sbuf.append(':');
      
                    int secs = calendar.get(Calendar.SECOND);
                    if (secs < 10)
                        sbuf.append('0');
                    sbuf.append(secs);
                    sbuf.append('.');      

                    // cache the result for later use
                    sbuf.getChars(start, sbuf.length(), cachedTimeNoMillis, 0);
                    cachedTime = now - millis;
                } else {
                    sbuf.append(cachedTimeNoMillis);
                }

                // Now the millis.
                if (millis < 10) 
                    sbuf.append("00");
                else if (millis < 100) 
                    sbuf.append('0');
                sbuf.append(millis);
                return sbuf;
            }

            // Never used.
            @Override
            public Date parse(String s, ParsePosition pos) {
                return null;
            }  
        }
    }
}
