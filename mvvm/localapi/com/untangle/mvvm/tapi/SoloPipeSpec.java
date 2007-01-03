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

package com.untangle.mvvm.tapi;

import java.util.Set;

import com.untangle.mvvm.MvvmContextFactory;
import com.untangle.mvvm.MvvmLocalContext;
import com.untangle.mvvm.tapi.event.SessionEventListener;
import com.untangle.mvvm.tran.Transform;
import org.apache.log4j.Logger;

public class SoloPipeSpec extends PipeSpec
{
    private static final MPipeManager MPIPE_MANAGER;
    private static final PipelineFoundry FOUNDRY;

    public static final int MIN_STRENGTH = 0;
    public static final int MAX_STRENGTH = 32;

    private final Fitting fitting;
    private final Affinity affinity;
    private final int strength;

    private final Logger logger = Logger.getLogger(getClass());

    private final SessionEventListener listener;
    private MPipe mPipe;

    // constructors -----------------------------------------------------------

    public SoloPipeSpec(String name, Transform transform, Set subscriptions,
                        SessionEventListener listener,
                        Fitting fitting, Affinity affinity, int strength)
    {
        super(name, transform, subscriptions);

        if (strength < MIN_STRENGTH || strength > MAX_STRENGTH) {
            throw new IllegalArgumentException("bad strength: " + strength);
        }

        this.listener = listener;
        this.fitting = fitting;
        this.affinity = affinity;
        this.strength = strength;
    }

    public SoloPipeSpec(String name, Transform transform,
                        Subscription subscription,
                        SessionEventListener listener, Fitting fitting,
                        Affinity affinity, int strength)
    {
        super(name, transform, subscription);

        if (strength < MIN_STRENGTH || strength > MAX_STRENGTH) {
            throw new IllegalArgumentException("bad strength: " + strength);
        }

        this.listener = listener;
        this.fitting = fitting;
        this.affinity = affinity;
        this.strength = strength;
    }

    public SoloPipeSpec(String name, Transform transform,
                        SessionEventListener listener,
                        Fitting fitting, Affinity affinity,
                        int strength)
    {
        super(name, transform);

        if (strength < MIN_STRENGTH || strength > MAX_STRENGTH) {
            throw new IllegalArgumentException("bad strength: " + strength);
        }

        this.listener = listener;
        this.fitting = fitting;
        this.affinity = affinity;
        this.strength = strength;
    }

    // accessors --------------------------------------------------------------

    public SessionEventListener getListener()
    {
        return listener;
    }

    public Fitting getFitting()
    {
        return fitting;
    }

    public Affinity getAffinity()
    {
        return affinity;
    }

    public int getStrength()
    {
        return strength;
    }

    public MPipe getMPipe()
    {
        return mPipe;
    }

    // PipeSpec methods -------------------------------------------------------

    @Override
    public void connectMPipe()
    {
        if (null == mPipe) {
            mPipe = MPIPE_MANAGER.plumbLocal(this, listener);
            FOUNDRY.registerMPipe(mPipe);
        } else {
            logger.warn("mPipes already connected");
        }
    }

    @Override
    public void disconnectMPipe()
    {
        if (null != mPipe) {
            FOUNDRY.deregisterMPipe(mPipe);
            mPipe.destroy();
            mPipe = null;
        } else {
            logger.warn("mPipes not connected");
        }
    }

    @Override
    public void dumpSessions()
    {
        if (null != mPipe) {
            mPipe.dumpSessions();
        }
    }

    @Override
    public IPSessionDesc[] liveSessionDescs()
    {
        if (null != mPipe) {
            return mPipe.liveSessionDescs();
        } else {
            return new IPSessionDesc[0];
        }
    }

    // static initialization --------------------------------------------------

    static {
        MvvmLocalContext mlc = MvvmContextFactory.context();
        MPIPE_MANAGER = mlc.mPipeManager();
        FOUNDRY = mlc.pipelineFoundry();
    }
}
