/*
 * $HeadURL: svn://chef/branch/prod/web-ui/work/src/uvm-lib/api/com/untangle/uvm/logging/LoggingSettings.java $
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

package com.untangle.uvm.message;

import java.io.Serializable;
import java.util.Date;

@SuppressWarnings("serial")
public class BlingBlinger implements CounterStats, Serializable
{
    private final StatDesc statDesc;

    private Date now = new Date();
    private Date lastUpdate = new Date();

    private volatile long count = 0;
    private volatile long countSinceMidnight = 0;

    public BlingBlinger(String name, String displayName, String unit,
                        String action, boolean displayable)
    {
        this.statDesc = new StatDesc(name, displayName, action, unit,
                                     displayable);
    }

    // public methods ---------------------------------------------------------

    public StatDesc getStatDesc()
    {
        return statDesc;
    }

    public long increment()
    {
        return increment(1);
    }

    public long increment(long delta)
    {
        synchronized (this) {
            now.setTime(System.currentTimeMillis());

            count += delta;
            if (lastUpdate.getDay() == now.getDay()
                && lastUpdate.getMonth() == now.getMonth()
                && lastUpdate.getYear() == now.getYear()) {
                countSinceMidnight += delta;
            } else {
                countSinceMidnight = 0;
                lastUpdate.setTime(now.getTime());
            }
        }

        return count;
    }

    // CounterStats methods ---------------------------------------------------
    public long getCount()
    {
        return count;
    }

    public void setCount(long newValue)
    {
        synchronized (this) {
            count = 0;
            countSinceMidnight = 0;
            increment(newValue);
        }
    }
    
    public long getCountSinceMidnight()
    {
        return countSinceMidnight;
    }

    public Date getLastActivityDate()
    {
        return now;
    }
}

