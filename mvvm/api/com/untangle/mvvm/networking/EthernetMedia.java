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

package com.untangle.mvvm.networking;

import java.io.Serializable;

import java.util.Map;
import java.util.HashMap;

import com.untangle.mvvm.tran.ParseException;

public class EthernetMedia implements Serializable, Comparable
{
    private static final String NAME_AUTO_NEGOTIATE = "Auto-Negotiate";
    /* These are the strings used by ethtool */
    private static final String SPEED_100   = "100";
    private static final String SPEED_10    = "10";

    private static final String DUPLEX_FULL = "full";
    private static final String DUPLEX_HALF = "half";
    
    private static final Map<Integer,EthernetMedia> TYPE_TO_MEDIA = new HashMap<Integer,EthernetMedia>();
    private static final Map<String,EthernetMedia>  NAME_TO_MEDIA = new HashMap<String,EthernetMedia>();

    /* This is kind of janky because mii-tool has different flags for using auto mode */
    public static final EthernetMedia AUTO_NEGOTIATE  =
        new EthernetMedia( 0, NAME_AUTO_NEGOTIATE, SPEED_100, DUPLEX_FULL );
    
    public static final EthernetMedia FULL_DUPLEX_100 =
        new EthernetMedia( 1, "100 Mbps, Full Duplex", SPEED_100, DUPLEX_FULL  );

    public static final EthernetMedia HALF_DUPLEX_100 =
        new EthernetMedia( 2, "100 Mbps, Half Duplex", SPEED_100, DUPLEX_HALF );

    public static final EthernetMedia FULL_DUPLEX_10  =
        new EthernetMedia( 3, "10 Mbps, Full Duplex",  SPEED_10,  DUPLEX_FULL );

    public static final EthernetMedia HALF_DUPLEX_10  =
        new EthernetMedia( 4, "10 Mbps, Half Duplex",  SPEED_10,  DUPLEX_HALF );

    private static final EthernetMedia ENUMERATION[] =
    {
        AUTO_NEGOTIATE, FULL_DUPLEX_100, HALF_DUPLEX_100, FULL_DUPLEX_10,HALF_DUPLEX_10
    };

    private final int type;
    private final String name;

    /* This is the media type that is used by ethtool */
    private final String speed;
    private final String duplex;

    private EthernetMedia( int type, String name, String speed, String duplex )
    {
        this.type  = type;
        this.name  = name;
        this.speed = speed;
        this.duplex = duplex;
    }

    public int getType()
    {
        return this.type;
    }

    public String getName()
    {
        return this.name;
    }
    
    public String getSpeed()
    {
        return this.speed;
    }

    public String getDuplex()
    {
        return this.duplex;
    }

    public String toString()
    {
        return this.name;
    }

    public int compareTo( Object o )
    {
        EthernetMedia other = (EthernetMedia)o;

        int oper1 = getType();
        int oper2 = other.getType();

        if (oper1 < oper2)
            return -1;
        else if (oper1 > oper2)
            return 1;
        else
            return 0;
    }

    boolean isAuto()
    {
        /* ,,, kind of janky, slightly paranoid */
        return ( this.name.equals( NAME_AUTO_NEGOTIATE ) || this.equals( AUTO_NEGOTIATE ));
    }

    public static EthernetMedia[] getEnumeration()
    {
        return ENUMERATION;
    }

    public static EthernetMedia getDefault()
    {
        return ENUMERATION[0];
    }

    public static EthernetMedia getInstance( int type ) throws ParseException
    {
        if ( type < 0 || type >= ENUMERATION.length ) {
            throw new ParseException( "Invalid Ethernet Media" );
        }

        return ENUMERATION[type];
    }
    
    static
    {
        for ( EthernetMedia em : ENUMERATION ) {
            TYPE_TO_MEDIA.put( em.getType(), em );
            NAME_TO_MEDIA.put( em.getName(), em );
        }
    }
}
