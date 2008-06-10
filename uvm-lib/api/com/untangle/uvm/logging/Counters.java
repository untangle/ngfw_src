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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Class for registering LoadStats and CounterStats objects and
 * retrieving their values.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class Counters
{
    private final Map<String, BlingBlinger> blingers
        = new HashMap<String, BlingBlinger>();

    private final Map<String, LoadMaster> loads
        = new HashMap<String, LoadMaster>();

    public BlingBlinger getBlingBlinger(String name)
    {
        BlingBlinger b;

        synchronized (blingers) {
            b = blingers.get(name);
        }

        return b;
    }

    public BlingBlinger makeBlingBlinger(String name, String displayName,
                                         String action, String unit)
    {
        BlingBlinger b;

        synchronized (blingers) {
            b = blingers.get(name);
            if (null == b) {
                b = new BlingBlinger(displayName, action, unit);
                blingers.put(name, b);
            }
        }

        return b;
    }

    public BlingBlinger makeBlingBlinger(String name, String displayName,
                                         String action)
    {
        return makeBlingBlinger(name, displayName, null);
    }

    public BlingBlinger makeBlingBlinger(String name, String displayName)
    {
        return makeBlingBlinger(name, displayName, null, null);
    }

    public long incrementCounter(String name, long delta)
    {
        BlingBlinger b = blingers.get(name);
        synchronized (b) {
            return b.increment(delta);
        }
    }

    public void addLoadMaster(String name, LoadMaster loadMaster)
    {
        synchronized (loads) {
            loads.put(name, loadMaster);
        }
    }

    public LoadCounter getLoadCounter(String name)
    {
        LoadMaster lm = null;
        LoadCounter lc = null;

        synchronized (loads) {
            lm = loads.get(name);
        }

        if (null != lm) {
            LoadStrober ls = lm.getLoadStrober();
            if (ls instanceof LoadCounter) {
                lc = (LoadCounter)ls;
            }
        }

        return lc;
    }

    public NodeStatDescs getStatDescs()
    {
        Map<String, StatDesc> bs = new HashMap<String, StatDesc>(blingers.size());
        synchronized (blingers) {
            for (String bn : blingers.keySet()) {
                bs.put(bn, blingers.get(bn).getStatDesc());
            }
        }

        Map<String, StatDesc> ls = new HashMap<String, StatDesc>(blingers.size());
        synchronized (loads) {
            for (String ln : loads.keySet()) {
                ls.put(ln, loads.get(ln).getStatDesc());
            }
        }

        return new NodeStatDescs(bs, ls);
    }

    public NodeStats getAllStats()
    {
        Map<String, CounterStats> c = new HashMap<String, CounterStats>();
        synchronized (blingers) {
            for (String n : blingers.keySet()) {
                c.put(n, new FixedCounts(blingers.get(n)));
            }
        }

        Map<String, LoadStats> l = new HashMap<String, LoadStats>();
        synchronized (loads) {
            for (String n : loads.keySet()) {
                l.put(n, new FixedLoads(loads.get(n)));
            }
        }

        return new NodeStats(c, l);
    }

    // private classes ---------------------------------------------------------

    private static class FixedCounts implements CounterStats, Serializable
    {
        private final long count;
        private final long countSinceMidnight;
        private final long cnt1;
        private final long cnt5;
        private final long cnt15;

        public FixedCounts(CounterStats cs)
        {
            this.count = cs.getCount();
            this.countSinceMidnight = cs.getCountSinceMidnight();
            this.cnt1 = cs.get1MinuteCount();
            this.cnt5 = cs.get5MinuteCount();
            this.cnt15 = cs.get15MinuteCount();
        }

        public long getCount()
        {
            return count;
        }

        public long getCountSinceMidnight()
        {
            return countSinceMidnight;
        }

        public long get1MinuteCount()
        {
            return cnt1;
        }

        public long get5MinuteCount()
        {
            return cnt5;
        }

        public long get15MinuteCount()
        {
            return cnt15;
        }
    }

    private static class FixedLoads implements LoadStats, Serializable
    {
        private final float avg1;
        private final float avg5;
        private final float avg15;

        public FixedLoads(LoadStats ls)
        {
            avg1 = ls.get1MinuteAverage();
            avg5 = ls.get5MinuteAverage();
            avg15 = ls.get15MinuteAverage();
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