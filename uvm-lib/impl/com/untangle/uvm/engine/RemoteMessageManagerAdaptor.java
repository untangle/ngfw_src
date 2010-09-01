/*
 * $HeadURL: svn://chef/branch/prod/web-ui/work/src/uvm-lib/impl/com/untangle/uvm/engine/UvmRemoteContextAdaptor.java $
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

import java.util.List;
import java.util.Map;

import com.untangle.uvm.message.ActiveStat;
import com.untangle.uvm.message.LocalMessageManager;
import com.untangle.uvm.message.Message;
import com.untangle.uvm.message.MessageQueue;
import com.untangle.uvm.message.RemoteMessageManager;
import com.untangle.uvm.message.StatDescs;
import com.untangle.uvm.message.Stats;
import com.untangle.uvm.policy.Policy;
import com.untangle.uvm.security.NodeId;

class RemoteMessageManagerAdaptor implements RemoteMessageManager
{
    private final LocalMessageManager lbm;

    RemoteMessageManagerAdaptor(LocalMessageManager lbm)
    {
        this.lbm = lbm;
    }

    public MessageQueue getMessageQueue()
    {
        return lbm.getMessageQueue();
    }

    public MessageQueue getMessageQueue(Integer key)
    {
        return lbm.getMessageQueue(key);
    }

    public MessageQueue getMessageQueue(Integer key, Policy p)
    {
        return lbm.getMessageQueue(key, p);
    }

    public StatDescs getStatDescs(NodeId t)
    {
        return lbm.getStatDescs(t);
    }

    public Map<String, Object> getSystemStats()
    {
        return lbm.getSystemStats();
    }

    public List<ActiveStat> getActiveMetrics(NodeId tid)
    {
        return lbm.getActiveMetrics(tid);
    }

    public void setActiveMetrics(NodeId tid, List<ActiveStat> activeMetrics)
    {
        lbm.setActiveMetrics(tid, activeMetrics);
    }

    public List<Message> getMessages()
    {
        return lbm.getMessages();
    }

    public List<Message> getMessages(Integer key)
    {
        return lbm.getMessages(key);
    }

    public Stats getStats(NodeId t)
    {
        return lbm.getStats(t);
    }

    public Stats getAllStats(NodeId t)
    {
        return lbm.getAllStats(t);
    }

    public Integer getMessageKey()
    {
        return lbm.getMessageKey();
    }
}
