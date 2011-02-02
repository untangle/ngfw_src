/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.uvm.node;

import java.io.Serializable;

/**
 * Port ranges are immutable.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
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
