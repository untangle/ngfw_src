/*
 * $HeadURL: svn://chef/branch/prod/web-ui/work/src/uvm-lib/impl/com/untangle/uvm/engine/UvmContextImpl.java $
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

package com.untangle.uvm.engine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.untangle.uvm.logging.ActiveBlinger;
import com.untangle.uvm.logging.BlingerState;
import com.untangle.uvm.logging.Counters;
import com.untangle.uvm.logging.LocalBlingerManager;
import com.untangle.uvm.logging.StatDescs;
import com.untangle.uvm.logging.Stats;
import com.untangle.uvm.node.LocalNodeManager;
import com.untangle.uvm.policy.Policy;
import com.untangle.uvm.security.Tid;
import com.untangle.uvm.util.TransactionWork;
import org.hibernate.Query;
import org.hibernate.Session;

class BlingerManagerImpl implements LocalBlingerManager
{
    private final Counters uvmCounters = new Counters();

    BlingerManagerImpl()
    {
        ensureTid0();
    }

    public BlingerState getBlingerState()
    {
        LocalNodeManager lm = UvmContextImpl.getInstance().nodeManager();
        List<Tid> tids = lm.nodeInstances();
        return getStats(lm, tids);
    }

    public BlingerState getBlingerState(Policy p)
    {
        LocalNodeManager lm = UvmContextImpl.getInstance().nodeManager();
        List<Tid> tids = lm.nodeInstances(p);
        return getStats(lm, tids);
    }

    public StatDescs getStatDescs(Tid t)
    {
        Long id = t.getId();
        if (null != id) {
            if (0 == id) {
                return uvmCounters.getStatDescss();
            } else {
                LocalNodeManager lm = UvmContextImpl.getInstance().nodeManager();
                return lm.nodeContext(t).node().getCounters().getStatDescss();
            }
        } else {
            return null;
        }
    }

    public List<ActiveBlinger> getActiveMetrics(final Tid tid)
    {
        TransactionWork<List<ActiveBlinger>> tw = new TransactionWork<List<ActiveBlinger>>()
            {
                private List<ActiveBlinger> result;

                public boolean doWork(Session s)
                {
                    Query q = s.createQuery
                        ("from BlingerSettings bs where bs.tid = :tid");
                    q.setParameter("tid", tid);
                    BlingerSettings bs = (BlingerSettings)q.uniqueResult();
                    if (null == bs) {
                        result = null;
                    } else {
                        result = bs.getActiveMetrics();
                    }

                    return true;
                }

                @Override
                public List<ActiveBlinger> getResult()
                {
                    return result;
                }
            };
        UvmContextImpl.getInstance().runTransaction(tw);

        return tw.getResult();
    }

    public void setActiveMetrics(final Tid tid,
                                 final List<ActiveBlinger> activeMetrics)
    {
        TransactionWork<List<ActiveBlinger>> tw = new TransactionWork<List<ActiveBlinger>>()
            {
                public boolean doWork(Session s)
                {
                    Query q = s.createQuery
                        ("from BlingerSettings bs where bs.tid = :tid");
                    q.setParameter("tid", tid);
                    BlingerSettings bs = (BlingerSettings)q.uniqueResult();
                    if (null == bs) {
                        bs = new BlingerSettings(tid, activeMetrics);
                        s.save(bs);
                    } else {
                        bs.setActiveMetrics(activeMetrics);
                        s.merge(bs);
                    }

                    return true;
                }
            };

        UvmContextImpl.getInstance().runTransaction(tw);
    }

    public Counters getUvmCounters()
    {
        return uvmCounters;
    }

    // private methods ---------------------------------------------------------

    private BlingerState getStats(LocalNodeManager lm, List<Tid> tids)
    {
        Map<Tid, Stats> stats = new HashMap<Tid, Stats>(tids.size());

        stats.put(new Tid(0L), uvmCounters.getAllStats());

        for (Tid t : tids) {
            Counters c = lm.nodeContext(t).node().getCounters();
            stats.put(t, c.getAllStats());
        }

        return new BlingerState(stats);
    }

    private void ensureTid0()
    {
        TransactionWork<List<ActiveBlinger>> tw = new TransactionWork<List<ActiveBlinger>>()
            {
                public boolean doWork(Session s)
                {
                    Query q = s.createQuery
                        ("from Tid t where t.id = 0");
                    Tid t = (Tid)q.uniqueResult();
                    if (null == t) {
                        t = new Tid(0L);
                        s.save(t);
                    }

                    return true;
                }
            };

        UvmContextImpl.getInstance().runTransaction(tw);
    }
}