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
import java.util.HashMap;
import java.util.Map;

import com.untangle.mvvm.tran.ParseException;

/**
 * A utility for interface conversion to and from user and database
 * strings.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
class IntfMatcherUtil
{
    static private final  IntfMatcherUtil INSTANCE = new IntfMatcherUtil();

    /* A map from the interface database name to its argon index */
    private final Map<String,Byte> DATA_TO_INTF_MAP;
    
    /* A map from the argon index to the interfaces database name */
    private final Map<Byte,String> INTF_TO_DATA_MAP;

    /* A map from the argon index to the interfaces user string. */
    private final Map<Byte,String> INTF_TO_USER_MAP;

    private IntfMatcherUtil()
    {
        Map<String,Byte> dataToIntf = new HashMap<String,Byte>();
        Map<Byte,String> intfToData = new HashMap<Byte,String>();
        Map<Byte,String> intfToUser = new HashMap<Byte,String>();


        /* XXXXXXXXXXXXXXXXXXXXX Use argon or something else */
        /* These values should not be hardcoded here. */
        mapIntf( (byte)0, "External", "O", dataToIntf, intfToData, intfToUser );
        mapIntf( (byte)1, "Internal", "I", dataToIntf, intfToData, intfToUser );
        mapIntf( (byte)2, "DMZ",      "D", dataToIntf, intfToData, intfToUser );
        mapIntf( (byte)3, "VPN",      "V", dataToIntf, intfToData, intfToUser );

        DATA_TO_INTF_MAP = Collections.unmodifiableMap( dataToIntf );
        INTF_TO_DATA_MAP = Collections.unmodifiableMap( intfToData );
        INTF_TO_USER_MAP = Collections.unmodifiableMap( intfToUser );
    }

    /**
     * Convert the database string into an argon interface.
     *
     * @param value The database string to convert.
     * @return The argon index for <param>value</param>
     * @exception ParseException If <param>value</param> is not a valid database string. 
     */
    byte databaseToIntf( String value ) throws ParseException
    {
        Byte intf = DATA_TO_INTF_MAP.get( value.toUpperCase().trim());
        if ( intf == null ) throw new ParseException( "Invalid interface: " + value );
        return intf;
    }

    /**
     * Convert an argon interface to a database string.
     *
     * @param intf Argon index to convert.
     * @return Database representation of argon intf.
     * @exception ParseException If <param>intf</param> is an unknown interface. 
     */
    String intfToDatabase( byte intf ) throws ParseException
    {
        String value = INTF_TO_DATA_MAP.get( intf );
        if ( value == null ) throw new ParseException( "Invalid interface: " + intf );
        return value;
    }

    /**
     * Convert an argon interface to a user string.
     *
     * @param intf Argon index to convert.
     * @return User string for <param>intf</param>
     * @exception ParseException If <param>intf</param> is an unknown interface. 
     */
    String intfToUser( byte intf ) throws ParseException
    {
        String value = INTF_TO_USER_MAP.get( intf );
        if ( value == null ) throw new ParseException( "Invalid interface: " + intf );
        return value;
    }

    /**
     * Utility method for creating all of the necessary maps for an interface.
     *
     * @param intf Argon index to map.
     * @param user User representation of <param>intf</param>
     * @param database Database representation of <param>intf</param>
     * @param dataToIntf Database to argon interface map.
     * @param intfToData Argon interface map to database map.
     * @param intfToUser Argon interface map to user string map.
     */
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
