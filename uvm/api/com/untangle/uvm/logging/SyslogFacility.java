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


/**
 * Represents the syslog facility.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public enum SyslogFacility 
{

    KERNEL(0, "kernel"),
    USER(1, "user"),
    MAIL(2, "mail"),
    DAEMON(3, "daemon"),
    SECURITY_0(4, "security 0"),
    SYSLOG(5, "syslog"),
    PRINTER(6, "printer"),
    NEWS(7, "news"),
    UUCP(8, "uucp"),
    CLOCK_0(9, "clock 0"),
    SECURITY_1(10, "security 1"),
    FTP(11, "ftp"),
    NTP(12, "ntp"),
    LOG_AUDIT(13, "log audit"),
    LOG_ALERT(14, "log alert"),
    CLOCK_1(15, "clock 1"),
    LOCAL_0(16, "local 0"),
    LOCAL_1(17, "local 1"),
    LOCAL_2(18, "local 2"),
    LOCAL_3(19, "local 3"),
    LOCAL_4(20, "local 4"),
    LOCAL_5(21, "local 5"),
    LOCAL_6(22, "local 6"),
    LOCAL_7(23, "local 7");
	
    private final int facilityValue;
    private final String name;

    // constructors -----------------------------------------------------------

    private SyslogFacility(int facilityValue, String name)
    {
        this.facilityValue = facilityValue;
        this.name = name;
    }

    // static methods ---------------------------------------------------------

    public static SyslogFacility getFacility(int value)
    {
    	SyslogFacility[] values = values();
    	for (int i = 0; i < values.length; i++) {
    		if (values[i].getFacilityValue() == value){
    			return values[i];
    		}
		}
    	return null;
    }

    public static SyslogFacility getFacility(String name)
    {
    	SyslogFacility[] values = values();
    	for (int i = 0; i < values.length; i++) {
    		if (values[i].getFacilityName().equals(name)){
    			return values[i];
    		}
		}
    	return null;
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
}
