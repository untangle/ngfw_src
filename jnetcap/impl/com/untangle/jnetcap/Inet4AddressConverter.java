/**
 * $Id$
 */
package com.untangle.jnetcap;

import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;

/**
 * Inet4AddressConverter
 */
public class Inet4AddressConverter
{
    private static final Logger logger = Logger.getLogger( Inet4AddressConverter.class );
    
    static final int INADDRSZ = 4;
    
    /**
     * Private because all utilities functions are static
     */
    private Inet4AddressConverter() { }

    /**
     * getByAddress
     * @param dotNotation
     * @return the InetAddress
     */
    public static InetAddress getByAddress (String dotNotation)
    {
        int input[] = new int[INADDRSZ];

        /* Trim any whitespace */
        dotNotation = dotNotation.trim();
        
        /* Use five to guarantee it doesn't converted from x.x.x.x.x to { x, x, x, x.x } */
        String tmp[] = dotNotation.split( "\\.", INADDRSZ + 1 );

        if ( tmp.length != INADDRSZ ) {
            logger.error( "UnknownHostException: Invalid dot notation - " + dotNotation );
            return null;
        }

        for ( int c = 0 ; c < tmp.length ; c++ ) {
            input[c] = Integer.parseInt( tmp[c] );
            if ( input[c] < 0 || input[c] > 255 ) {
                logger.error( "UnknownHostException: Invalid dot notation - " + dotNotation );
                return null;
            }
        }

        return getByAddress( input );
    }

    /**
     * getByHexAddress
     * @param hex
     * @param isLittleEndian
     * @return the InetAddress
     */
    public static InetAddress getByHexAddress( String hex, boolean isLittleEndian )
    {
        int input[] = new int[INADDRSZ];
        
        hex = hex.trim();
        
        for( int c = 0 ; c < INADDRSZ ; c++ ) {
            int tmp = Integer.parseInt( hex.substring( c*2, c*2 + 2), 16 );
            if ( isLittleEndian ) {
                input[(INADDRSZ - 1) - c] = tmp;
            } else {
                input[c] = tmp;
            }
        }
        
        return getByAddress( input );

    }

    /**
     * getByAddress
     * @param input
     * @return InetAddress
     */
    public static InetAddress getByAddress ( int input[] )
    {
        byte byteArray[] = new byte[INADDRSZ];
        InetAddress address = null;

        if ( input.length != INADDRSZ ) {
            logger.error( "Invalid input length" );
            return null;
        }
        
        for ( int c = 0 ; c < INADDRSZ ; c++ ) {
            byteArray[c] = (byte)input[c];;
        }
        
        try {
            address = Inet4Address.getByAddress( byteArray );
        } catch ( UnknownHostException e ) {
            /* ??? This should never happen */
            logger.error( "UnknownHostException: " + e.getMessage());
        }
        
        return address;
    }


    /**
     * toLong convert an address to long
     * @param address
     * @return long
     */
    public static long toLong ( InetAddress address )
    {
        long val = 0;
        int c;
        
        byte valArray[] = address.getAddress();
        
        for ( c = 0 ; c < INADDRSZ ; c++ ) {
            val += ((long)byteToInt(valArray[c])) << ( 8 * c );
        }

        return val;
    }

    /**
     * toAddress convert a long to address
     * @param val - long value
     * @return InetAddress
     */
    public static InetAddress toAddress ( long val ) 
    {
        byte valArray[] = new byte[INADDRSZ];
        InetAddress address = null;
                
        for ( int c = 0 ; c < INADDRSZ ; c++ ) {
            valArray[c] = (byte)((val >> ( 8 * c)) & 0xFF);
        }
        
        try {
            address = Inet4Address.getByAddress ( valArray );
        } catch ( UnknownHostException e ) {
            /* ??? This should never happen */
            logger.error( "UnknownHostException: " + e.getMessage());
        }

        return address;
    }

    /**
     * byteToInt
     * @param val
     * @return int
     */
    private static int byteToInt ( byte val )
    {
        int num = val;
        if ( num < 0 ) num = num & 0x7F + 0x80;
        return num;
    }
}
