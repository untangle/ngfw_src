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

package com.untangle.uvm.logging;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Counters
{
    private final Map<String, CounterStats> stats
        = new ConcurrentHashMap<String, CounterStats>();

    public Set<String> getCounterNames()
    {
        return stats.keySet();
    }

    public Map<String, CounterStats> getAllStats()
    {
        Map m = new HashMap(stats.size());

        for (String n : stats.keySet()) {
            CounterStats cs = stats.get(n);
            if (null != cs) {
                m.put(n, new FixedStats(cs));
            }
        }

        return m;
    }

    public int getCount(String name)
    {
        return stats.get(name).getCount();
    }

    public float get1MinuteAverage(String name)
    {
        return stats.get(name).get1MinuteAverage();
    }

    public float get5MinuteAverage(String name)
    {
        return stats.get(name).get5MinuteAverage();
    }

    public float get15MinuteAverage(String name)
    {
        return stats.get(name).get15MinuteAverage();
    }

    // private classes ---------------------------------------------------------

    private static class FixedStats implements CounterStats
    {
        private final int count;
        private final float avg1;
        private final float avg5;
        private final float avg15;

        public FixedStats(CounterStats cs)
        {
            this.count = cs.getCount();
            this.avg1 = cs.get1MinuteAverage();
            this.avg5 = cs.get5MinuteAverage();
            this.avg15 = cs.get15MinuteAverage();
        }

        public int getCount()
        {
            return count;
        }

        public float get1MinuteAverage()
        {
            return avg1;
        }

        public float get5MinuteAverage()
        {
            return avg5;
        }

        public float get15MinuteAverage()
        {
            return avg15;
        }
    }
}