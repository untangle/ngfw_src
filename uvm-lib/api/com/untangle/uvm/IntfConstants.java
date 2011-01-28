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

package com.untangle.uvm;

/**
 * These are constants for the indices and names of the Network Interfaces on the box.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
public class IntfConstants
{
    /* User names for the four default interface. */
    public static final String INTERNAL = "Internal";
    public static final String EXTERNAL = "External";
    public static final String DMZ      = "DMZ";
    public static final String VPN      = "VPN";

    /* The argon index for the four default interfaces */
    public static final int   EXTERNAL_INTF = 1;
    public static final int   INTERNAL_INTF = 2;
    public static final int   DMZ_INTF      = 3;
    public static final int   VPN_INTF      = 255;

    /* Internal constants, the string representation is never shown to the user */

    public static final int   MAX_INTF      = 255;
    public static final int   UNKNOWN_INTF  = MAX_INTF + 2;
    public static final int   LOOPBACK_INTF = MAX_INTF + 1;

    /* These are constants for netcap interfaces that are not actually interfaces. */
    /* Index used when netcap had an error determining the interface */
    public static final int  NETCAP_ERROR    = 0;

    /* Index used if the interface is the loopback interface of the box */
    public static final int  NETCAP_LOOPBACK = 254;

    /* Index used when netcap was unable to determine the interface. */
    public static final int  NETCAP_UNKNOWN  = 255;

    /* The minimum index for a netcap interface 8*/
    public static final int  NETCAP_MIN      = 1;

    /* The maximum index for a netcap interface */
    public static final int  NETCAP_MAX      = MAX_INTF;

    /* Netcap indices for the default interfaces */
    public static final int  NETCAP_EXTERNAL = EXTERNAL_INTF;
    public static final int  NETCAP_INTERNAL = INTERNAL_INTF;
    public static final int  NETCAP_DMZ      = DMZ_INTF;
    public static final int  NETCAP_VPN      = VPN_INTF;

    /* Argon constants */
    public static final int  ARGON_MIN      = 0;
    public static final int  ARGON_MAX      = MAX_INTF;

    /* argon equalivalent for the non-network, network interfaces */
    public static final int  ARGON_ERROR    = ARGON_MIN - 1;
    public static final int  ARGON_LOOPBACK = LOOPBACK_INTF;
    public static final int  ARGON_UNKNOWN  = UNKNOWN_INTF;

}

