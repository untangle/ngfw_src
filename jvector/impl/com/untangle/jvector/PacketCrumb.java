/**
 * $Id$
 */
package com.untangle.jvector;

import com.untangle.jnetcap.Netcap;
import com.untangle.jnetcap.UDPPacket;

/**
 * PacketCrumb is a crumb that embeds a packet
 */
public abstract class PacketCrumb extends DataCrumb
{
    protected byte ttl;
    protected byte tos;
    protected byte options[];

    /**
     * Create a new Packet Crumb.</p>
     *
     * @param ttl     - Time To Live for the packet.
     * @param tos     - Type of service for the packet.
     * @param options - Type of service for the packet.          
     * @param data    - Byte array containing the data.
     * @param offset  - Offset in the byte array.
     * @param limit   - Limit of the data.
     */
    public PacketCrumb( byte ttl, byte tos, byte options[], byte[] data, int offset, int limit )
    {
        super( data, offset, limit );
        
        this.ttl     = ttl;
        this.tos     = tos;
        this.options = options;
    }

    /**
     * PacketCrumb
     * @param packet
     * @param data
     * @param offset
     * @param limit
     */
    protected PacketCrumb( UDPPacket packet, byte[] data, int offset, int limit )
    {
        this( packet.attributes().ttl(), packet.attributes().tos(), null, data, offset, limit );        
    }

    /**
     * PacketCrumb
     * @param packet
     * @param data
     * @param limit
     */
    protected PacketCrumb( UDPPacket packet, byte[] data, int limit )
    {
        this( packet, data, 0, limit );
    }

    /**
     * PacketCrumb
     * @param packet
     * @param data
     */
    protected PacketCrumb( UDPPacket packet, byte[] data )
    {
        this( packet, data, data.length );
    }
    
    /**
     * makeCrumb
     * @param packet
     * @throws JVectorException
     * @return PacketCrumb
     */
    static PacketCrumb makeCrumb( UDPPacket packet ) throws JVectorException
    {
        int protocol = packet.attributes().getProtocol();

        switch ( protocol ) {
        case Netcap.IPPROTO_UDP:
            return new UDPPacketCrumb( packet, packet.data() );
        default:
            throw new JVectorException( "Unable to determine which crumb to create from protocol: " + protocol );
        }
    }
    
    /**
     * type
     * @return
     */
    public abstract int type();

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

    /**
     * raze
     */
    public void raze()
    {
        /* Do nothing, C structure is freed automatically */
    }
}
