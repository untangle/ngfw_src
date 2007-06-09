/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.mvvm.engine;

import java.net.*;
import java.util.*;

import com.untangle.mvvm.tapi.*;
import com.untangle.mvvm.tapi.event.SessionEventListener;

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
 * @author <a href="mailto:jdi@untangle.com"></a>
 * @version 1.0
 */
class MPipeManagerImpl implements MPipeManager
{
    private static final MPipeManagerImpl MANAGER = new MPipeManagerImpl();

    // List of mPipes we manage for the transform
    protected final List allMPipes = new ArrayList();

    protected MPipeManagerImpl() { }

    static final MPipeManagerImpl manager()
    {
        return MANAGER;
    }

    /**
     * The <code>plumbLocal</code> method connects the MetaSmith to a MPIPE

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

    // MVVM Context calls in here when restarting the whole mvvm,
    // after destroying all the transforms.  We just do cleanup.
    public void destroy() {
        allMPipes.clear();
    }
}
