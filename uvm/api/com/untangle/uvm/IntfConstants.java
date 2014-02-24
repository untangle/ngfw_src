/**
 * $Id$
 */
package com.untangle.uvm;

/**
 * These are constants for the indices and names of the Network Interfaces on the box.
 *
 */
public class IntfConstants
{
    /* User names for the four default interface. */
    public static final String INTERNAL = "Internal";
    public static final String EXTERNAL = "External";
    public static final String DMZ      = "DMZ";
    public static final String OPENVPN  = "OpenVPN";
    public static final String L2TP     = "L2TP";

    /**
     * An interface index of 0 is an error condition
     * Likely that it was not properly initialized or tagged
     */
    public static final int   UNKNOWN_INTF  = 0; /* from libnetcap.h */

    /**
     * Common Indexs for interfaces
     */
    public static final int   MIN_INTF      = 1;
    public static final int   EXTERNAL_INTF = 1;
    public static final int   INTERNAL_INTF = 2;
    public static final int   DMZ_INTF      = 3;
    public static final int   OPENVPN_INTF  = 250;
    public static final int   L2TP_INTF     = 251;
    public static final int   MAX_INTF      = 251;
}

