/**
 * $Id$
 */

package com.untangle.uvm.app;

import java.io.Serializable;

/**
 * Port ranges are immutable.
 */
@SuppressWarnings("serial")
public class PortRange implements Serializable
{

    public static final PortRange ANY = new PortRange(0, 65535);

    private final int low;
    private final int high;

    /**
     * Constructor
     * @param port Single port value
     */
    public PortRange(int port)
    {
        low = high = port;
    }

    /**
     * Constructor
     * @param low Low value for range
     * @param high High value for range
     */
    public PortRange(int low, int high)
    {
        this.low = low;
        this.high = high;
    }


    /**
     * See if the range contains the specified port
     * @param port The port to check
     * @return True if contained in our range, otherwise false
     */
    public boolean contains(int port)
    {
        return low <= port && port <= high;
    }

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

    /**
     * Compare to an object
     *  @param o The object for comparison
     *  @return True if equal, otherwise false
     */
    public boolean equals(Object o)
    {
        if (!(o instanceof PortRange)) {
            return false;
        }

        PortRange pr = (PortRange)o;
        return low == pr.low && high == pr.high;
    }

    /**
     * Get the hash code
     * @return The hash code
     */
    public int hashCode()
    {
        int result = 17;
        result = 37 * result + low;
        result = 37 * result + high;

        return result;
    }

    /**
     * Get the string representation
     * @return The string representation
     */
    public String toString() {
        return low +"-"+ high;
    }
}
