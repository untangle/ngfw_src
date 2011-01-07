/* $HeadURL$ */
package com.untangle.uvm.node;

import java.io.Serializable;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("serial")
public class IPaddr implements Comparable<IPaddr>, Serializable
{

    static final String CIDR_STRINGS[] = 
    {
        "0.0.0.0",         "128.0.0.0",       "192.0.0.0",       "224.0.0.0",
        "240.0.0.0",       "248.0.0.0",       "252.0.0.0",       "254.0.0.0",
        "255.0.0.0",       "255.128.0.0",     "255.192.0.0",     "255.224.0.0",
        "255.240.0.0",     "255.248.0.0",     "255.252.0.0",     "255.254.0.0",
        "255.255.0.0",     "255.255.128.0",   "255.255.192.0",   "255.255.224.0",
        "255.255.240.0",   "255.255.248.0",   "255.255.252.0",   "255.255.254.0",
        "255.255.255.0",   "255.255.255.128", "255.255.255.192", "255.255.255.224",
        "255.255.255.240", "255.255.255.248", "255.255.255.252", "255.255.255.254",
        "255.255.255.255"
    };

    /* Should be an unmodifiable list or vector */
    static final IPaddr CIDR_CONVERTER[] = new IPaddr[CIDR_STRINGS.length];
    
    static final Map<IPaddr,Integer> NET_TO_CIDR = new HashMap<IPaddr,Integer>();

    static final int INADDRSZ = 4;

    private final InetAddress addr;

    public IPaddr( InetAddress addr )
    {
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
            try {
                int val = Integer.parseInt( tmp[c] );
                
                if ( val < 0 || val > 255 ) {
                    throw new ParseException( "Each component must be between 0 and 255 " + tmp );
                }
            } catch ( NumberFormatException e ) {
                throw new ParseException( "Each component must be between 0 and 255 " + tmp );
            }
        }

        return new IPaddr(InetAddress.getByName( dotNotation ));
    }
    
    public static IPaddr cidrToIPaddr( String cidr )
        throws ParseException
    {
        cidr = cidr.trim();
        
        try {
            return cidrToIPaddr( Integer.parseInt( cidr ));
        } catch ( NumberFormatException e ) {
            throw new ParseException( "CIDR notation should contain a number between 0 and 32, '" + 
                                      cidr + "'" );
        }
    }

    public static IPaddr cidrToIPaddr( int cidr )
        throws ParseException
    {
        if ( cidr < 0 || cidr > CIDR_CONVERTER.length ) {
            throw new ParseException( "CIDR notation[" + cidr +
                                      "] should end with a number between 0 and " + CIDR_CONVERTER.length );
        }
        
        return CIDR_CONVERTER[cidr];
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

    public IPaddr or( IPaddr addr2 )
    {
        long oper1 = toLong();
        long oper2 = addr2.toLong();
        
        return makeIPaddr( oper1 | oper2 );
    }
    
    public IPaddr inverse()
    {
        long oper = toLong();
        return makeIPaddr( ~oper );
    }

    public int toCidr() throws ParseException
    {
        Integer cidr = NET_TO_CIDR.get( this );

        if ( cidr == null ) throw new ParseException( "The ipaddr " + this + " is not a valid netmask" );
        return cidr;
    }

    public boolean isGreaterThan( IPaddr addr2 ) 
    {
        return ( this.compareTo( addr2 ) > 0);
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
        /* XXX A little questionable, but this is only used for comparisons anyway */
        if ( addr == null ) return 0;

        long val = 0;
        byte valArray[] = addr.getAddress();

        for ( int c = 0 ; c < INADDRSZ ; c++ ) {
            val += ((long)byteToInt(valArray[c])) << ( 8 * ( INADDRSZ - c - 1 ));
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

    public int compareTo(IPaddr other)
    {
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
            valArray[INADDRSZ - c - 1] = (byte)((addr >> (8 * c)) & 0xFF);
        }
        
        try {
            address = Inet4Address.getByAddress( valArray );
        } catch ( UnknownHostException e ) {
            /* XXX THIS SHOULD NEVER HAPPEN */
            return null;
        }

        return new IPaddr(address);
    }

    static int byteToInt ( byte val ) 
    {
        int num = val;
        if ( num < 0 ) num = num & 0x7F + 0x80;
        return num;
    }

    /* tired of checking for null everywhere */
    public static boolean equals( IPaddr addr1, IPaddr addr2 )
    {
        if ( addr1 == null || addr1 == null ) return addr1 == addr2;

        return addr1.equals( addr2 );
    }
    
    static
    {
        int c = 0;
        for ( String cidr : CIDR_STRINGS ) {
            try {
                IPaddr addr = new IPaddr(InetAddress.getByName( cidr ));
                NET_TO_CIDR.put( addr, c );
                CIDR_CONVERTER[c++] = addr;
            } catch ( UnknownHostException e ) {
                System.err.println( "Invalid CIDR String at index: " + c );
            }
        }
    }
}

