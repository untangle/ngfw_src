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

package com.untangle.uvm.node.firewall.intf;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.untangle.uvm.node.ParseException;

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
