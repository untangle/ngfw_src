/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.uvm.logging;

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
