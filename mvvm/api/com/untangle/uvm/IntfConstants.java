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

package com.untangle.mvvm;

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
    public static final byte   EXTERNAL_INTF = 0;
    public static final byte   INTERNAL_INTF = 1;
    public static final byte   DMZ_INTF      = 2;
    public static final byte   VPN_INTF      = 3;

    /* Internal constants, the string representation is never shown to the user */
    public static final byte   MAX_INTF      = 8;
    public static final byte   UNKNOWN_INTF  = MAX_INTF + 2;
    public static final byte   LOOPBACK_INTF = MAX_INTF + 1;
    
    /* These are constants for netcap interfaces that are not actually interfaces. */
    /* Index used when netcap had an error determining the interface */
    public static final byte  NETCAP_ERROR    = 0;

    /* Index used if the interface is the loopback interface of the box */
    public static final byte  NETCAP_LOOPBACK = 17;

    /* Index used when netcap was unable to determine the interface. */
    public static final byte  NETCAP_UNKNOWN  = 18;

    /* The minimum index for a netcap interface 8*/
    public static final byte  NETCAP_MIN      = 1;

    /* The maximum index for a netcap interface */
    public static final byte  NETCAP_MAX      = MAX_INTF + 1;

    /* Netcap indices for the default interfaces */
    public static final byte  NETCAP_EXTERNAL = EXTERNAL_INTF + 1;
    public static final byte  NETCAP_INTERNAL = INTERNAL_INTF + 1;
    public static final byte  NETCAP_DMZ      = DMZ_INTF + 1;
    public static final byte  NETCAP_VPN      = VPN_INTF + 1;

    /* Argon constants */
    public static final byte  ARGON_MIN      = 0;
    public static final byte  ARGON_MAX      = MAX_INTF;

    /* argon equalivalent for the non-network, network interfaces */
    public static final byte  ARGON_ERROR    = ARGON_MIN - 1;
    public static final byte  ARGON_LOOPBACK = LOOPBACK_INTF;
    public static final byte  ARGON_UNKNOWN  = UNKNOWN_INTF;


    /**
     * Convert an argon interface index to its name.
     * 
     * @param argonIntf Argon interface index.
     * @return The user string represented by <code>argonIntf</code>.
     */
    public static String toName( byte argonIntf  )
    {
        switch ( argonIntf ) {
        case INTERNAL_INTF: return INTERNAL;
        case EXTERNAL_INTF: return EXTERNAL;
        case DMZ_INTF: return DMZ;
        case VPN_INTF: return VPN;
        }
        
        return "unknown";
    }
}

