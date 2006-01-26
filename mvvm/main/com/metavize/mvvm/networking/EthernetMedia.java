/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

package com.metavize.mvvm.networking;

import java.util.Map;
import java.util.HashMap;

import com.metavize.mvvm.tran.ParseException;

public class EthernetMedia
{
    private static final Map<Integer,EthernetMedia> TYPE_TO_MEDIA = new HashMap<Integer,EthernetMedia>();
    private static final Map<String,EthernetMedia>  NAME_TO_MEDIA = new HashMap<String,EthernetMedia>();

    /* This is kind of janky because mii-tool has different flags for using auto mode */
    public static final EthernetMedia AUTO_NEGOTIATE  =
        new EthernetMedia( 0, "Auto-Negotiate", "" );
    
    public static final EthernetMedia FULL_DUPLEX_100 =
        new EthernetMedia( 1, "100 Mbps, Full Duplex", "100baseTx-FD" );

    public static final EthernetMedia HALF_DUPLEX_100 =
        new EthernetMedia( 2, "100 Mbps, Half Duplex", "100baseTx-HD" );

    public static final EthernetMedia FULL_DUPLEX_10  =
        new EthernetMedia( 3, "10 Mbps, Full Duplex", "10baseT-FD" );

    public static final EthernetMedia HALF_DUPLEX_10  =
        new EthernetMedia( 4, "10 Mbps, Half Duplex", "10baseT-HD" );

    private static final EthernetMedia ENUMERATION[] =
    {
        AUTO_NEGOTIATE, FULL_DUPLEX_100, HALF_DUPLEX_100, FULL_DUPLEX_10,HALF_DUPLEX_10
    };

    private final int type;
    private final String name;

    /* This is the media type that is used by mii-tool */
    private final String media;

    private EthernetMedia( int type, String name, String media )
    {
        this.type  = type;
        this.name  = name;
        this.media = media;
    }

    public int getType()
    {
        return this.type;
    }

    public String getName()
    {
        return this.name;
    }

    public String toString()
    {
        return this.name;
    }

    /* This is only required by this package */
    String getMiiToolMedia()
    {
        return this.media;
    }

    boolean isAuto()
    {
        /* ,,, kind of janky */
        return (( this.media.length() == 0 ) || this.equals( AUTO_NEGOTIATE ));
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
