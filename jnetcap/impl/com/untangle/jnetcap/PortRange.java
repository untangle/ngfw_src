/*
 * $HeadURL: svn://chef/work/src/jnetcap/impl/com/untangle/jnetcap/PortRange.java $
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
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
