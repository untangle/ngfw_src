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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.untangle.uvm.message.ActiveStat;
import com.untangle.uvm.message.Counters;
import com.untangle.uvm.message.LocalMessageManager;
import com.untangle.uvm.message.Message;
import com.untangle.uvm.message.MessageQueue;
import com.untangle.uvm.message.StatDescs;
import com.untangle.uvm.message.Stats;
import com.untangle.uvm.node.LocalNodeManager;
import com.untangle.uvm.policy.Policy;
import com.untangle.uvm.security.Tid;
import com.untangle.uvm.util.TransactionWork;
import org.hibernate.Query;
import org.hibernate.Session;

class MessageManagerImpl implements LocalMessageManager
{
    private final Counters uvmCounters = new Counters();

    // XXX this needs to be per client session
    private final List<Message> messages = new ArrayList<Message>();

    MessageManagerImpl()
    {
        ensureTid0();
    }

    // RemoteMessageManager methods --------------------------------------------

    public MessageQueue getMessageQueue()
    {
        LocalNodeManager lm = UvmContextImpl.getInstance().nodeManager();
        List<Tid> tids = lm.nodeInstances();
        Map<Tid, Stats> stats = getStats(lm, tids);
        List<Message> messages = getMessages();
        return new MessageQueue(messages, stats);
    }

    public MessageQueue getMessageQueue(Policy p)
    {
        LocalNodeManager lm = UvmContextImpl.getInstance().nodeManager();
        List<Tid> tids = lm.nodeInstances(p);
        Map<Tid, Stats> stats = getStats(lm, tids);
        List<Message> messages = getMessages();
        return new MessageQueue(messages, stats);
    }

    public StatDescs getStatDescs(Tid t)
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

    public List<ActiveStat> getActiveMetrics(final Tid tid)
    {
        TransactionWork<List<ActiveStat>> tw = new TransactionWork<List<ActiveStat>>()
            {
                private List<ActiveStat> result;

                public boolean doWork(Session s)
                {
                    Query q = s.createQuery
                        ("from StatSettings bs where bs.tid = :tid");
                    q.setParameter("tid", tid);
                    StatSettings bs = (StatSettings)q.uniqueResult();
                    if (null == bs) {
                        result = null;
                    } else {
                        result = bs.getActiveMetrics();
                    }

                    return true;
                }

                @Override
                public List<ActiveStat> getResult()
                {
                    return result;
                }
            };
        UvmContextImpl.getInstance().runTransaction(tw);

        return tw.getResult();
    }

    public void setActiveMetrics(final Tid tid,
                                 final List<ActiveStat> activeMetrics)
    {
        TransactionWork<List<ActiveStat>> tw = new TransactionWork<List<ActiveStat>>()
            {
                public boolean doWork(Session s)
                {
                    Query q = s.createQuery
                        ("from StatSettings bs where bs.tid = :tid");
                    q.setParameter("tid", tid);
                    StatSettings bs = (StatSettings)q.uniqueResult();
                    if (null == bs) {
                        bs = new StatSettings(tid, activeMetrics);
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

    public List<Message> getMessages()
    {
        List<Message> l = new ArrayList<Message>(messages.size());

        synchronized (messages) {
            l.addAll(messages);
            messages.clear();
        }

        return l;
    }

    // LocalMessageManager methods ---------------------------------------------

    public Counters getUvmCounters()
    {
        return uvmCounters;
    }

    public void submitMessage(Message m)
    {
        synchronized (messages) {
            messages.add(m);
        }
    }

    // private methods ---------------------------------------------------------

    private Map<Tid, Stats> getStats(LocalNodeManager lm, List<Tid> tids)
    {
        Map<Tid, Stats> stats = new HashMap<Tid, Stats>(tids.size());

        stats.put(new Tid(0L), uvmCounters.getAllStats());

        for (Tid t : tids) {
            List<ActiveStat> as = getActiveMetrics(t);
            Counters c = lm.nodeContext(t).node().getCounters();
            stats.put(t, c.getAllStats(as));
        }

        return stats;
    }

    private void ensureTid0()
    {
        TransactionWork tw = new TransactionWork()
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