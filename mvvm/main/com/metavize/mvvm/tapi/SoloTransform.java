/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: SoloTransform.java,v 1.1 2005/01/30 09:20:31 amread Exp $
 */

package com.metavize.mvvm.tapi;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.tran.TransformDesc;
import org.apache.log4j.Logger;

public abstract class SoloTransform extends AbstractTransform
{
    private final PipelineFoundry pipelineFoundry
        = MvvmContextFactory.context().pipelineFoundry();

    private final Logger logger = Logger.getLogger(SoloTransform.class);

    private MPipe mPipe;

    // abstract methods ------------------------------------------------------

    protected abstract PipeSpec getPipeSpec();

    // public methods --------------------------------------------------------

    public MPipe getMPipe()
    {
        return mPipe;
    }

    public void dumpSessions()
    {
        if (mPipe != null)
            mPipe.dumpSessions();
    }

    public IPSessionDesc[] liveSessionDescs()
    {
        if (mPipe == null) {
            return new IPSessionDesc[0];
        } else {
            return mPipe.liveSessionDescs();
        }
    }

    protected final void connectMPipe()
    {
        if (null != mPipe) {
            logger.warn("already started");
            return;
        }

        StringBuilder msg = new StringBuilder("starting transform ");
        TransformDesc td = getTransformDesc();
        msg.append(td.getName());
        msg.append("(");
        msg.append(getTid().getName());
        msg.append(")");
        logger.info(msg.toString());

        mPipe = MPipeManager.manager().plumbLocal(this, getPipeSpec());

        pipelineFoundry.registerMPipe(mPipe);
    }

    protected final void disconnectMPipe()
    {
        pipelineFoundry.deregisterMPipe(mPipe);

        mPipe.destroy();
        mPipe = null;
    }
}
