/**
 * $Id$
 */
package com.untangle.uvm.vnet;

/**
 * This class just contains IP header low-level data from a UDP packet such as TTL, TOS and options.
 *
 */
public class IPPacketHeader
{
    protected byte ttl;
    protected byte tos;
    protected byte options[];
    
    /**
     * IPPacketHeader constructor
     * @param ttl
     * @param tos
     * @param options
     */
    public IPPacketHeader( byte ttl, byte tos, byte options[] )
    {
        this.ttl = ttl;
        this.tos = tos;
        this.options = options;
    }

    /**
     * ttl
     * @return
     */
    public byte ttl() 
    { 
        return ttl; 
    }
    
    /**
     * tos
     * @return
     */
    public byte tos() 
    { 
        return tos;
    }

    /**
     * options
     * @return
     */
    public byte[] options()
    {
        return options;
    }

    /**
     * ttl
     * @param value
     */
    public void ttl( byte value )
    {
        ttl = value;
    }

    /**
     * tos
     * @param value
     */
    public void tos( byte value )
    {
        tos = value;
    }

    /**
     * options
     * @param value
     */
    public void options( byte[] value )
    {
        options = value;
    }
}
