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

package com.untangle.mvvm.tran.firewall.intf;

import java.util.Collections;
import java.util.Map;
import java.util.HashMap;

import com.untangle.mvvm.tran.ParseException;

class IntfMatcherUtil
{
    static private final  IntfMatcherUtil INSTANCE = new IntfMatcherUtil();

    private final Map<String,Byte> DATA_TO_INTF_MAP;
    private final Map<Byte,String> INTF_TO_DATA_MAP;
    private final Map<Byte,String> INTF_TO_USER_MAP;

    private IntfMatcherUtil()
    {
        Map<String,Byte> dataToIntf = new HashMap<String,Byte>();
        Map<Byte,String> intfToData = new HashMap<Byte,String>();
        Map<Byte,String> intfToUser = new HashMap<Byte,String>();

        
        /* XXXXXXXXXXXXXXXXXXXXX Use argon or something else */
        mapIntf( (byte)0, "External", "O", dataToIntf, intfToData, intfToUser );
        mapIntf( (byte)1, "Internal", "I", dataToIntf, intfToData, intfToUser );
        mapIntf( (byte)2, "DMZ",      "D", dataToIntf, intfToData, intfToUser );
        mapIntf( (byte)3, "VPN",      "V", dataToIntf, intfToData, intfToUser );

        DATA_TO_INTF_MAP = Collections.unmodifiableMap( dataToIntf );
        INTF_TO_DATA_MAP = Collections.unmodifiableMap( intfToData );
        INTF_TO_USER_MAP = Collections.unmodifiableMap( intfToUser );
    }

    byte databaseToIntf( String value ) throws ParseException
    {
        Byte intf = DATA_TO_INTF_MAP.get( value.toUpperCase().trim());
        if ( intf == null ) throw new ParseException( "Invalid interface: " + value );
        return intf;
    }
    
    String intfToDatabase( byte intf ) throws ParseException
    {
        String value = INTF_TO_DATA_MAP.get( intf );
        if ( value == null ) throw new ParseException( "Invalid interface: " + intf );
        return value;
    }

    String intfToUser( byte intf ) throws ParseException
    {
        String value = INTF_TO_USER_MAP.get( intf );
        if ( value == null ) throw new ParseException( "Invalid interface: " + intf );
        return value;
    }

    private  void mapIntf( byte intf, String user, String database, Map<String,Byte> dataToIntf,
                                 Map<Byte,String> intfToData, Map<Byte,String> intfToUser )
    {
        dataToIntf.put( database, intf );
        intfToData.put( intf, database );
        intfToUser.put( intf, user );
    }

    public static IntfMatcherUtil getInstance()
    {
        return INSTANCE;
    }
}
