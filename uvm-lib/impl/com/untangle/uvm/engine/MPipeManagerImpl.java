/*
 * $HeadURL$
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
import java.util.List;

import com.untangle.uvm.vnet.MPipe;
import com.untangle.uvm.vnet.MPipeManager;
import com.untangle.uvm.vnet.PipeSpec;
import com.untangle.uvm.vnet.event.SessionEventListener;

/**
 * Service-provider & manager class for MPipes.
 *
 * <p>A <code>MPipeManager</code> is a concrete subclass of this class
 * that has a zero-argument constructor and implements the abstract
 * methods herein.  A given Meta Node virtual machine maintains a single
 * system-wide default manager instance, which is returned by the {@link
 * #manager manager} method.  The first invocation of that method will locate
 * and cache the default provider as specified below.
 *
 * We also add internally used functionality here.
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
 */
class MPipeManagerImpl implements MPipeManager
{
    private static final MPipeManagerImpl MANAGER = new MPipeManagerImpl();

    // List of mPipes we manage for the node
    protected final List allMPipes = new ArrayList();

    protected MPipeManagerImpl() { }

    static final MPipeManagerImpl manager()
    {
        return MANAGER;
    }

    /**
     * The <code>plumbLocal</code> method connects to a MPIPE

     * on the local machine.  No remote MPIPE may be contacted in this way.
     *
     */
    public MPipe plumbLocal(PipeSpec pipeSpec, SessionEventListener listener)
    {
        // Class is configurable by changing MPipeManagers, so we can
        // hard code it here.
        MPipeImpl mPipe = new MPipeImpl(this, pipeSpec, listener);

        synchronized(allMPipes) {
            allMPipes.add(mPipe);
        }
        return mPipe;
    }

    private static final MPipe[] MPIPE_PROTO = new MPipe[0];

    public MPipe[] mPipes()
    {
        return (MPipe[])allMPipes.toArray(MPIPE_PROTO);
    }

    // MPipe calls in here after destroying.
    protected void destroyed(MPipe mPipe) {
        synchronized(allMPipes) {
            allMPipes.remove(mPipe);
        }
    }

    // UVM Context calls in here when restarting the whole uvm,
    // after destroying all the nodes.  We just do cleanup.
    public void destroy() {
        allMPipes.clear();
    }
}
