/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: Util.java 892 2005-06-06 10:22:14Z inieves $
 */

package com.metavize.mvvm.reporting;

import java.text.DecimalFormat;

public class Util {
    
    static final int KILO =  0;
    static final int MEGA =  1;
    static final int GIGA =  2;
    static final int TERA =  3;
    static final int PETA =  4;
    static final int EXA  =  5;

    public static String trimNumber(String suffix, long number){

	String returnString = null;

	if(number < 1000l)
	    returnString = new String(  Long.toString(number) + " " + suffix );
	else if(number < 1000000l)
	    returnString = new String(  Long.toString(number/1000l) + "." + Long.toString(number%1000l) + " K" + suffix );
	else if(number < 1000000000l)
	    returnString = new String(  Long.toString(number/1000000l) + "." + Long.toString((number%1000000l)/1000l) + " M" + suffix );
	else if(number < 1000000000000l)
	    returnString = new String(  Long.toString(number/1000000000l) + "." + Long.toString((number%1000000000l)/1000000l) + " G" + suffix );
	else if(number < 1000000000000000l)
	    returnString = new String(  Long.toString(number/1000000000000l) + "." + Long.toString((number%1000000000000l)/1000000000l) + " T" + suffix );
	else if(number < 1000000000000000000l)
	    returnString = new String(  Long.toString(number/1000000000000000l) + "." + Long.toString((number%1000000000000000l)/1000000000000l) + " P" + suffix );
	else
	    returnString = new String(  Long.toString(number/1000000000000000000l) + "." + Long.toString((number%1000000000000000000l)/1000000000000000l) + " P" + suffix );

	
	return returnString;
    }


    public static String percentNumber(long number, long total){
	if( total < 1 )
	    return "0%";
	DecimalFormat decimalFormat = new DecimalFormat("#0.00%");
	decimalFormat.setMultiplier(100);

	double percentage = (double) number / (double) total;
	return decimalFormat.format( percentage );
    }

}
