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

import com.untangle.uvm.message.ActiveStat;
import com.untangle.uvm.message.BlingerState;
import com.untangle.uvm.logging.LocalBlingerManager;
import com.untangle.uvm.message.StatDescs;
import com.untangle.uvm.message.RemoteMessageManager;
import com.untangle.uvm.policy.Policy;
import com.untangle.uvm.security.Tid;

class RemoteMessageManagerAdaptor implements RemoteMessageManager
{
    private final LocalBlingerManager lbm;

    RemoteMessageManagerAdaptor(LocalBlingerManager lbm)
    {
        this.lbm = lbm;
    }

    public BlingerState getBlingerState()
    {
        return lbm.getBlingerState();
    }

    public BlingerState getBlingerState(Policy p)
    {
        return lbm.getBlingerState(p);
    }

    public StatDescs getStatDescs(Tid t)
    {
        return lbm.getStatDescs(t);
    }

    public List<ActiveStat> getActiveMetrics(Tid tid)
    {
        return lbm.getActiveMetrics(tid);
    }

    public void setActiveMetrics(Tid tid, List<ActiveStat> activeMetrics)
    {
        lbm.setActiveMetrics(tid, activeMetrics);
    }
}