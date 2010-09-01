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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.untangle.uvm.LocalUvmContext;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.security.NodeId;

/**
 * Class for registering LoadStats and CounterStats objects and
 * retrieving their values.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class Counters
{
    private final NodeId tid;

    private final Map<String, BlingBlinger> metrics
        = new LinkedHashMap<String, BlingBlinger>();

    private final Map<String, BlingBlinger> activities
        = new LinkedHashMap<String, BlingBlinger>();

    private final Map<String, LoadMaster> loads
        = new LinkedHashMap<String, LoadMaster>();

    public Counters(NodeId tid)
    {
        this.tid = tid;
    }

    public BlingBlinger getBlingBlinger(String name)
    {
        BlingBlinger b;

        synchronized (metrics) {
            b = metrics.get(name);
        }

        return b;
    }

    public BlingBlinger addMetric(String name, String displayName, String unit,
                                  boolean displayable)
    {
        BlingBlinger b = new BlingBlinger(name, displayName, unit, null,
                                          displayable);

        synchronized (metrics) {
            metrics.put(name, b);
        }

        return b;
    }

    public BlingBlinger addMetric(String name, String displayName, String unit)
    {
        return addMetric(name, displayName, unit, true);
    }

    public BlingBlinger addActivity(String name, String displayName,
                                    String unit, String action,
                                    boolean displayable)
    {
        BlingBlinger b = new BlingBlinger(name, displayName, unit, action,
                                          displayable);

        synchronized (metrics) {
            metrics.put(name, b);
        }

        synchronized (activities) {
            activities.put(name, b);
        }

        return b;
    }

    public BlingBlinger addActivity(String name, String displayName,
                                    String unit, String action)
    {
        return addActivity(name, displayName, unit, action, true);
    }

    public BlingBlinger delActivity(String name)
    {
        BlingBlinger b = null;
        synchronized (metrics) {
            b = metrics.remove(name);
        }
        synchronized (activities) {
            b  = (b == null) ? activities.remove(name) : b;
        }

        return b;
    }

    public void addLoadMaster(String name, LoadMaster loadMaster)
    {
        synchronized (loads) {
            loads.put(name, loadMaster);
        }
    }

    public LoadCounter makeLoadCounter(String name, String displayName)
    {
        LoadCounter lc = new LoadCounter();
        LoadMaster lm = new LoadMaster(lc, name, displayName);

        synchronized (loads) {
            loads.put(name, lm);
        }

        return lc;
    }

    public StatDescs getStatDescs()
    {
        LocalUvmContext mctx = LocalUvmContextFactory.context();
        List<ActiveStat> activeStats = mctx.messageManager().getActiveMetrics(tid);
        return new StatDescs(metrics.values(), activities.values(), activeStats);
    }

    public Stats getAllStats()
    {
        return new Stats(metrics, activities);
    }

    public Stats getAllStats(List<ActiveStat> l)
    {
        Map<String, BlingBlinger> m = new HashMap<String, BlingBlinger>();
        if(l!=null) {
            for (ActiveStat as : l) {
                String n = as.getName();
                m.put(n, metrics.get(n));
            }
        }
        return new Stats(m, activities);
    }
}