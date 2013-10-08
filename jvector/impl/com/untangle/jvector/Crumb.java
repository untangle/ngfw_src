/**
 * $Id: Crumb.java 31087 2012-02-09 06:04:51Z dmorris $
 */
package com.untangle.jvector;

public abstract class Crumb
{
    protected static final int DATA_MASK     = 0x1100;
    protected static final int SHUTDOWN_MASK = 0x1200;

    public static final int TYPE_DATA        = DATA_MASK | 1;      // Data crumb, passed as is
    public static final int TYPE_UDP_PACKET  = DATA_MASK | 2;      // UDP Packet, this extends a PacketCrumb
    public static final int TYPE_ICMP_PACKET = DATA_MASK | 3;      // ICMP packet, this extends a PacketCrumb

    public static final int TYPE_SHUTDOWN    = SHUTDOWN_MASK | 1;  // Shutdown
    public static final int TYPE_RESET       = SHUTDOWN_MASK | 2;  // Reset

    public abstract void raze();
    public abstract int  type();

    public boolean isData()
    {
        return ( type() & DATA_MASK ) == DATA_MASK;
    }

    public boolean isShutdown()
    {
        return ( type() & SHUTDOWN_MASK ) == SHUTDOWN_MASK;
    }
}
