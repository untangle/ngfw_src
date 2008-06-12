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

import com.untangle.uvm.logging.BlingerState;
import com.untangle.uvm.logging.Counters;
import com.untangle.uvm.logging.LocalBlingerManager;
import com.untangle.uvm.logging.NodeStatDescs;
import com.untangle.uvm.logging.NodeStats;
import com.untangle.uvm.node.LocalNodeManager;
import com.untangle.uvm.policy.Policy;
import com.untangle.uvm.security.Tid;
import com.untangle.uvm.util.TransactionWork;
import org.hibernate.Query;
import org.hibernate.Session;

class BlingerManagerImpl implements LocalBlingerManager
{
    private LocalNodeManager nodeManager;
    private Counters uvmCounters = new Counters();

    BlingerManagerImpl()
    {
        makeTid0();
    }

    public BlingerState getBlingerState()
    {
        LocalNodeManager lm = UvmContextImpl.getInstance().nodeManager();
        List<Tid> tids = lm.nodeInstances();
        return getNodeStats(lm, tids);
    }

    public BlingerState getBlingerState(Policy p)
    {
        LocalNodeManager lm = UvmContextImpl.getInstance().nodeManager();
        List<Tid> tids = lm.nodeInstances(p);
        return getNodeStats(lm, tids);
    }

    public NodeStatDescs getNodeStatDesc(Tid t)
    {
        Long id = t.getId();
        if (null != id) {
            if (0 == id) {
                return uvmCounters.getStatDescs();
            } else {
                LocalNodeManager lm = UvmContextImpl.getInstance().nodeManager();
                return lm.nodeContext(t).node().getCounters().getStatDescs();
            }
        } else {
            return null;
        }
    }

    public List<String> getActiveBlingers(final Tid tid)
    {
        TransactionWork<List<String>> tw = new TransactionWork<List<String>>()
            {
                private List<String> result;

                public boolean doWork(Session s)
                {
                    Query q = s.createQuery
                        ("from BlingerSettings bs where bs.tid = :tid");
                    q.setParameter("tid", tid);
                    BlingerSettings bs = (BlingerSettings)q.uniqueResult();
                    if (null == bs) {
                        result = null;
                    } else {
                        result = bs.getActiveBlingers();
                    }

                    return true;
                }

                @Override
                public List<String> getResult()
                {
                    return result;
                }
            };
        UvmContextImpl.getInstance().runTransaction(tw);

        return tw.getResult();
    }

    public void setActiveBlingers(final Tid tid,
                                  final List<String> activeBlingers)
    {
        TransactionWork<List<String>> tw = new TransactionWork<List<String>>()
            {
                public boolean doWork(Session s)
                {
                    Query q = s.createQuery
                        ("from BlingerSettings bs where bs.tid = :tid");
                    q.setParameter("tid", tid);
                    BlingerSettings bs = (BlingerSettings)q.uniqueResult();
                    if (null == bs) {
                        System.out.println("NULL BS");
                        bs = new BlingerSettings(tid);
                        bs.setActiveBlingers(activeBlingers);
                        System.out.println("TID: " + bs.getTid());
                        s.save(bs);
                    } else {
                        System.out.println("EXISTING BS");
                        bs.setActiveBlingers(activeBlingers);
                        System.out.println("TID: " + bs.getTid());
                        s.update(bs);
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

    private BlingerState getNodeStats(LocalNodeManager lm, List<Tid> tids)
    {
        Map<Tid, NodeStats> stats = new HashMap<Tid, NodeStats>(tids.size());

        stats.put(new Tid(0L), uvmCounters.getAllStats());

        for (Tid t : tids) {
            Counters c = lm.nodeContext(t).node().getCounters();
            stats.put(t, c.getAllStats());
        }

        return new BlingerState(stats);
    }

    private void makeTid0()
    {
        TransactionWork<List<String>> tw = new TransactionWork<List<String>>()
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