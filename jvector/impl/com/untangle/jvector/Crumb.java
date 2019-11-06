/**
 * $Id$
 */
package com.untangle.jvector;

/**
 * An abstract Crumb
 */
public abstract class Crumb
{
    protected static final int DATA_MASK     = 0x1100;
    protected static final int SHUTDOWN_MASK = 0x1200;

    public static final int TYPE_DATA        = DATA_MASK | 1;      // Data crumb
    public static final int TYPE_UDP_PACKET  = DATA_MASK | 2;      // UDP Packet
    public static final int TYPE_OBJECT      = DATA_MASK | 3;      // Object crumb

    public static final int TYPE_SHUTDOWN    = SHUTDOWN_MASK | 1;  // Shutdown
    public static final int TYPE_RESET       = SHUTDOWN_MASK | 2;  // Reset

    /**
     * force_jni_header_generation
     * @return
     */
    protected native boolean force_jni_header_generation();

    /**
     * raze
     */
    public abstract void raze();

    /**
     * type
     * @return
     */
    public abstract int  type();

    /**
     * isData
     * @return true if is a data crumb, false otherwise
     */
    public boolean isData()
    {
        return ( type() & DATA_MASK ) == DATA_MASK;
    }

    /**
     * isShutdown
     * @return true if is a shutdown crumb, false otherwise
     */
    public boolean isShutdown()
    {
        return ( type() & SHUTDOWN_MASK ) == SHUTDOWN_MASK;
    }
}
