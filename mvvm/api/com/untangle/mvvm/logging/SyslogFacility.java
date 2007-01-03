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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SyslogFacility implements Serializable
{
    public static final SyslogFacility KERNEL;
    public static final SyslogFacility USER;
    public static final SyslogFacility MAIL;
    public static final SyslogFacility DAEMON;
    public static final SyslogFacility SECURITY_0;
    public static final SyslogFacility SYSLOG;
    public static final SyslogFacility PRINTER;
    public static final SyslogFacility NEWS;
    public static final SyslogFacility UUCP;
    public static final SyslogFacility CLOCK_0;
    public static final SyslogFacility SECURITY_1;
    public static final SyslogFacility FTP;
    public static final SyslogFacility NTP;
    public static final SyslogFacility LOG_AUDIT;
    public static final SyslogFacility LOG_ALERT;
    public static final SyslogFacility CLOCK_1;
    public static final SyslogFacility LOCAL_0;
    public static final SyslogFacility LOCAL_1;
    public static final SyslogFacility LOCAL_2;
    public static final SyslogFacility LOCAL_3;
    public static final SyslogFacility LOCAL_4;
    public static final SyslogFacility LOCAL_5;
    public static final SyslogFacility LOCAL_6;
    public static final SyslogFacility LOCAL_7;

    private static final SyslogFacility[] facilities;

    static {
        facilities = new SyslogFacility[24];

        KERNEL = new SyslogFacility(0, "kernel");
        USER = new SyslogFacility(1, "user");
        MAIL = new SyslogFacility(2, "mail");
        DAEMON = new SyslogFacility(3, "daemon");
        SECURITY_0 = new SyslogFacility(4, "security 0");
        SYSLOG = new SyslogFacility(5, "syslog");
        PRINTER = new SyslogFacility(6, "printer");
        NEWS = new SyslogFacility(7, "news");
        UUCP = new SyslogFacility(8, "uucp");
        CLOCK_0 = new SyslogFacility(9, "clock 0");
        SECURITY_1 = new SyslogFacility(10, "security 1");
        FTP = new SyslogFacility(11, "ftp");
        NTP = new SyslogFacility(12, "ntp");
        LOG_AUDIT = new SyslogFacility(13, "log audit");
        LOG_ALERT = new SyslogFacility(14, "log alert");
        CLOCK_1 = new SyslogFacility(15, "clock 1");
        LOCAL_0 = new SyslogFacility(16, "local 0");
        LOCAL_1 = new SyslogFacility(17, "local 1");
        LOCAL_2 = new SyslogFacility(18, "local 2");
        LOCAL_3 = new SyslogFacility(19, "local 3");
        LOCAL_4 = new SyslogFacility(20, "local 4");
        LOCAL_5 = new SyslogFacility(21, "local 5");
        LOCAL_6 = new SyslogFacility(22, "local 6");
        LOCAL_7 = new SyslogFacility(23, "local 7");
    }

    private final int facilityValue;
    private final String name;

    // constructors -----------------------------------------------------------

    private SyslogFacility(int facilityValue, String name)
    {
        this.facilityValue = facilityValue;
        this.name = name;

        facilities[facilityValue] = this;
    }

    // static methods ---------------------------------------------------------

    public static SyslogFacility getFacility(int value)
    {
        return facilities[value];
    }

    public static SyslogFacility getFacility(String name)
    {
	for( SyslogFacility sf : facilities ) // XXX linear, not fast but works for now
	    if( sf.getFacilityName().equals(name) )
		return sf;
	return null;
    }

    public static List<SyslogFacility> values()
    {
        List<SyslogFacility> l = new ArrayList<SyslogFacility>(facilities.length);
        for (SyslogFacility sf : facilities) {
            l.add(sf);
        }

        return l;
    }

    // public methods ---------------------------------------------------------

    public int getFacilityValue()
    {
        return facilityValue;
    }

    public String getFacilityName()
    {
        return name;
    }

    // serialization methods --------------------------------------------------

    Object readResolve()
    {
        return facilities[facilityValue];
    }
}
