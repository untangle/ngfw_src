/*
 * $Id$
 */
package com.untangle.uvm.app;

import java.io.Serializable;

/**
 * Port ranges are immutable.
 *
 */
@SuppressWarnings("serial")
public class PortRange implements Serializable
{

    public static final PortRange ANY = new PortRange(0, 65535);

    private final int low;
    private final int high;

    // Constructors -----------------------------------------------------------

    public PortRange(int port)
    {
        low = high = port;
    }

    public PortRange(int low, int high)
    {
        this.low = low;
        this.high = high;
    }

    // business methods -------------------------------------------------------

    public boolean contains(int port)
    {
        return low <= port && port <= high;
    }

    // accessors --------------------------------------------------------------

    /**
     * The low port, inclusive.
     *
     * @return the low port.
     */
    public int getLow()
    {
        return low;
    }

    /**
     * The high port, inclusive.
     *
     * @return the high port.
     */
    public int getHigh() /* do it! */
    {
        return high;
    }

    // Object methods ---------------------------------------------------------

    public boolean equals(Object o)
    {
        if (!(o instanceof PortRange)) {
            return false;
        }

        PortRange pr = (PortRange)o;
        return low == pr.low && high == pr.high;
    }

    public int hashCode()
    {
        int result = 17;
        result = 37 * result + low;
        result = 37 * result + high;

        return result;
    }

    public String toString() {
        return low +"-"+ high;
    }
}
