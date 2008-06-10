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

import com.untangle.uvm.logging.BlingerState;
import com.untangle.uvm.logging.LocalBlingerManager;
import com.untangle.uvm.logging.NodeStatDescs;
import com.untangle.uvm.logging.RemoteBlingerManager;
import com.untangle.uvm.policy.Policy;
import com.untangle.uvm.security.Tid;

class RemoteBlingerManagerAdaptor implements RemoteBlingerManager
{
    private final LocalBlingerManager lbm;

    RemoteBlingerManagerAdaptor(LocalBlingerManager lbm)
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

    public NodeStatDescs getNodeStatDesc(Tid t)
    {
        return lbm.getNodeStatDesc(t);
    }
}