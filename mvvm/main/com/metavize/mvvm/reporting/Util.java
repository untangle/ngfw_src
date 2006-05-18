/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.reporting;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.io.*;
import java.sql.*;

public class Util {

    // REPORT TYPES ////////////////
    static final int REPORT_TYPE_DAILY = 0;
    static final int REPORT_TYPE_WEEKLY = 1;
    static final int REPORT_TYPE_MONTHLY = 2;
    ////////////////////////////////

    // REPORT GENERATION QUERY LIMITS ///////
    static final int MAX_ROWS_PER_REPORT = 500;  // gives ~25 pages
    /////////////////////////////////////////

    // REPORT GENERATION TIME PERIODS ///////
    static Calendar reportNow;
    static Timestamp midnight;
    static Timestamp lastday;
    static Timestamp lastweek;
    static Timestamp lastmonth;
    ////////////////////////////////

    // FOR UNIT CONVERSIONS ///////
    static final int KILO =  0;
    static final int MEGA =  1;
    static final int GIGA =  2;
    static final int TERA =  3;
    static final int PETA =  4;
    static final int EXA  =  5;
    ///////////////////////////////


    static void init(boolean toMidnight) {
	// INITIALIZE TIME CONSTANTS
        Calendar c = Calendar.getInstance();
        if (toMidnight) {
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
        }
        reportNow = (Calendar) c.clone();
	midnight = new Timestamp(c.getTimeInMillis());
	Calendar lastdayCalendar = (Calendar) c.clone();
        Calendar lastweekCalendar = (Calendar) c.clone();
        Calendar lastmonthCalendar = (Calendar) c.clone();
        lastdayCalendar.add(Calendar.DAY_OF_YEAR, -1);
        lastday = new Timestamp(lastdayCalendar.getTimeInMillis());
        lastweekCalendar.add(Calendar.WEEK_OF_YEAR, -1);
        lastweek = new Timestamp(lastweekCalendar.getTimeInMillis());
        lastmonthCalendar.add(Calendar.MONTH, -1);
        lastmonth = new Timestamp(lastmonthCalendar.getTimeInMillis());
    }


    public static String trimNumber(String suffix, long number){

	String returnString;

	if(number < 1000l)
	    returnString = new String(  Long.toString(number) + (suffix.length()==0?"":" " + suffix) );
	else if(number < 1000000l){
	    if( suffix.length() == 0 )
		returnString = Long.toString(number/1000l) + "," + String.format("%1$03d", number%1000l);
	    else
		returnString = new String(  Long.toString(number/1000l) + "." + String.format("%1$03d", number%1000l) + " K" + suffix );
	}
	else if(number < 1000000000l)
	    returnString = new String(  Long.toString(number/1000000l) + "." + String.format("%1$03d", (number%1000000l)/1000l) + " M" + suffix );
	else if(number < 1000000000000l)
	    returnString = new String(  Long.toString(number/1000000000l) + "." + String.format("%1$03d", (number%1000000000l)/1000000l) + " G" + suffix );
	else if(number < 1000000000000000l)
	    returnString = new String(  Long.toString(number/1000000000000l) + "." + String.format("%1$03d", (number%1000000000000l)/1000000000l) + " T" + suffix );
	else if(number < 1000000000000000000l)
	    returnString = new String(  Long.toString(number/1000000000000000l) + "." + String.format("%1$03d", (number%1000000000000000l)/1000000000000l) + " P" + suffix );
	else
	    returnString = new String(  Long.toString(number/1000000000000000000l) + "." + String.format("%1$03d", (number%1000000000000000000l)/1000000000000000l) + " P" + suffix );

	
	return returnString;
    }

    public static String percentNumber(long number, long total){
	if( total < 1 )
	    return "0.00%";
	DecimalFormat decimalFormat = new DecimalFormat("#0.00%");
	decimalFormat.setMultiplier(100);

	double percentage = (double) number / (double) total;
	return decimalFormat.format( percentage );
    }

    public static String getDateDirName(Calendar c)
    {      
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH) + 1; // Java is stupid
        int day = c.get(Calendar.DAY_OF_MONTH);
        String name = String.format("%04d-%02d-%02d", year, month, day);
        return name;
    }


    public static boolean deleteDir(File dir) {
        // to see if this directory is actually a symbolic link to a directory,
        // we want to get its canonical path - that is, we follow the link to
        // the file it's actually linked to
        File candir;
        try {
            candir = dir.getCanonicalFile();
        } catch (IOException e) {
            return false;
        }
  
        // a symbolic link has a different canonical path than its actual path,
        // unless it's a link to itself
        if (!candir.equals(dir.getAbsoluteFile())) {
            // this file is a symbolic link, and there's no reason for us to
            // follow it, because then we might be deleting something outside of
            // the directory we were told to delete
            return false;
        }
  
        // now we go through all of the files and subdirectories in the
        // directory and delete them one by one
        File[] files = candir.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
  
                // in case this directory is actually a symbolic link, or it's
                // empty, we want to try to delete the link before we try
                // anything
                boolean deleted = file.delete();
                if (!deleted) {
                    // deleting the file failed, so maybe it's a non-empty
                    // directory
                    if (file.isDirectory()) deleteDir(file);
  
                    // otherwise, there's nothing else we can do
                }
            }
        }
  
        // now that we tried to clear the directory out, we can try to delete it
        // again
        return dir.delete();  
    }

}
