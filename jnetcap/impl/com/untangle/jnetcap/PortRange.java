/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.jnetcap;

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
     */
    public int low()
    {
        return low;
    }

    /**
     * Retrieve the high value
     */
    public int high()
    {
        return high;
    }

    // Object methods ---------------------------------------------------------

    public boolean equals(Object o)
    {
        if (!(o instanceof PortRange)) {
            return false;
        } else {
            PortRange pr = (PortRange)o;
            return low == pr.low && high == pr.high;
        }
    }
}
