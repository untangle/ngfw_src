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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Stats implements Serializable
{
    private final Map<String, CounterStats> metrics;
    private final Map<String, CounterStats> activities;

    // constructors -----------------------------------------------------------

    Stats(Map<String, BlingBlinger> metrics,
          Map<String, BlingBlinger> activities)
    {
        this.metrics = immutableCounterStats(metrics);
        this.activities = immutableCounterStats(activities);
    }

    // public methods ---------------------------------------------------------

    public Map<String, CounterStats> getMetrics()
    {
        return metrics;
    }

    public Map<String, CounterStats> getActivities()
    {
        return activities;
    }

    // private methods --------------------------------------------------------

    private Map<String, CounterStats> immutableCounterStats(Map<String, BlingBlinger> stats)
    {
        Map<String, CounterStats> m = new HashMap<String, CounterStats>(stats.size());
        for (String s : stats.keySet()) {
            CounterStats cs = stats.get(s);
            if ( cs == null ) {
                continue;
            }
            m.put(s, new FixedCounts(cs));
        }

        return Collections.unmodifiableMap(m);
    }

    // private classes --------------------------------------------------------

    public static class FixedCounts implements CounterStats, Serializable
    {
        private final long count;
        private final long countSinceMidnight;
        private final Date lastActivityDate;

        public FixedCounts(CounterStats cs)
        {
            this.count = cs.getCount();
            this.countSinceMidnight = cs.getCountSinceMidnight();
            this.lastActivityDate = cs.getLastActivityDate();
        }

        public long getCount()
        {
            return count;
        }

        public long getCountSinceMidnight()
        {
            return countSinceMidnight;
        }

        public Date getLastActivityDate()
        {
            return lastActivityDate;
        }
    }
}
