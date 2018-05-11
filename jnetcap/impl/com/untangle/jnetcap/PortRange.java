/**
 * $Id$
 */
package com.untangle.jnetcap;

/**
 * Port range represents a range of ports
 */
public class PortRange
{
    private final int low;
    private final int high;

    /**
     * Create a range from <code>low</code> to <code>high<code>
     * @param low - low value (inclusive).
     * @param high - high value (inclusive).
     */
    public PortRange( int low, int high )
    {
        if (low > high) {
            int swap = low;
            low = high;
            high = swap;
        }

        this.low  = low;
        this.high = high;
    }

    /**
     * Retrieve the low value
     * @return the low value
     */
    public int low()
    {
        return low;
    }

    /**
     * Retrieve the high value
     * @return the high value
     */
    public int high()
    {
        return high;
    }

    /**
     * True if the two objects are equal
     * @param o - the object
     * @return boolean
     */
    public boolean equals(Object o)
    {
        if (!(o instanceof PortRange)) {
            return false;
        } else {
            PortRange pr = (PortRange)o;
            return low == pr.low && high == pr.high;
        }
    }

    /**
     * Overridden hash function
     * @return hash
     */
    @Override
    public int hashCode()
    {
        return low*high;
    }
}
