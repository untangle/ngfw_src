
/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

package com.metavize.mvvm.tran;

import java.io.Serializable;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class IPaddr implements Comparable, Serializable
{
    private static final long serialVersionUID = -741858749430271001L;

    static final int INADDRSZ = 4;
    
    private final InetAddress addr;

    public IPaddr( Inet4Address addr ) {
        this.addr = addr;
    }

    public static IPaddr parse( String dotNotation ) throws ParseException, UnknownHostException
    {
        /* Trim any whitespace */
        dotNotation = dotNotation.trim();

        /* Use five to guarantee it doesn't converted from x.x.x.x.x to { x, x, x, x.x } */
        String tmp[] = dotNotation.split( "\\.", INADDRSZ + 1 );

        if ( tmp.length != INADDRSZ ) {
            throw new ParseException( "Invalid IPV4 dot-notation address" + dotNotation );
        }

        /* Validation */
        for ( int c = 0 ; c < tmp.length ; c++ ) {
            int val = Integer.parseInt( tmp[c] );
            if ( val < 0 || val > 255 ) {
                throw new ParseException( "Each component must be between 0 and 255 " + tmp);
            }
        }

        return new IPaddr((Inet4Address)InetAddress.getByName( dotNotation ));
    }
    
    public static IPaddr and( IPaddr addr1, IPaddr addr2 ) 
    {
        return addr1.and( addr2 );
    }

    public InetAddress getAddr()
    {
        return addr;
    }

    public IPaddr and( IPaddr addr2 )
    {
        long oper1 = toLong();
        long oper2 = addr2.toLong();
        
        return makeIPaddr( oper1 & oper2 );
    }

    public boolean isGreaterThan( IPaddr addr2 ) 
    {
        long oper1 = toLong();
        long oper2 = addr2.toLong();

        return ( oper1 > oper2 );
    }
    
    public boolean isInNetwork( IPaddr addr2, IPaddr netmaskAddress )
    {
        long netmask = netmaskAddress.toLong();

        if ( netmask == 0 ) {
            netmask = 0xFFFFFFFFL;
        }
        
        long oper1   = toLong() & netmask;
        long oper2   = addr2.toLong() & netmask;

        return ( oper1 == oper2 );
    }
    
    public boolean isEmpty()
    {
        if ( addr == null ) 
            return true;

        byte tmp[] = addr.getAddress();

        for ( int c = 0 ; c < INADDRSZ ; c++ ) {
            if ( tmp[c] != 0 )
                return false;
        }

        return true;
    }

    public String toString()
    {
        if ( addr == null ) 
            return "";
        
        return addr.getHostAddress();
    }

    /** Convert an IPaddr to a long */
    private long toLong( )
    {
        long val = 0;
        
        byte valArray[] = addr.getAddress();
        
        for ( int c = 0 ; c < INADDRSZ ; c++ ) {
            val += ((long)byteToInt(valArray[c])) << ( 8 * c );
        }

        return val;
    }

    /* The value here is just the addr, just pass it down */
    public int hashCode()
    {
        if ( addr == null ) return 0;

        return addr.hashCode();
    }

    public boolean equals( Object o )
    {
        if ( o instanceof IPaddr ) {
            InetAddress addr2 = ((IPaddr)o).addr;
            if ( addr == null ) {
                return ( addr2 == null );
            } else {
                return addr.equals( addr2 );
            }
        }

        return false;
    }

    public int compareTo(Object o)
    {
        IPaddr other = (IPaddr)o;
        long oper1 = toLong();
        long oper2 = other.toLong();

        if (oper1 < oper2)
            return -1;
        else if (oper1 > oper2)
            return 1;
        else
            return 0;
    }

    private static IPaddr makeIPaddr( long addr )
    {
        byte valArray[] = new byte[INADDRSZ];
        InetAddress address = null;
                
        for ( int c = 0 ; c < INADDRSZ ; c++ ) {
            valArray[c] = (byte)((addr >> ( 8 * c)) & 0xFF);
        }
        
        try {
            address = Inet4Address.getByAddress( valArray );
        } catch ( UnknownHostException e ) {
            /* XXX THIS SHOULD NEVER HAPPEN */
            return null;
        }

        return new IPaddr((Inet4Address)address );
    }

    static int byteToInt ( byte val ) 
    {
        int num = val;
        if ( num < 0 ) num = num & 0x7F + 0x80;
        return num;
    }
}

