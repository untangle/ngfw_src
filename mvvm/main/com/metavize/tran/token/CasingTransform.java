/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.token;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.tapi.AbstractTransform;
import com.metavize.mvvm.tapi.IPSessionDesc;
import com.metavize.mvvm.tapi.MPipe;
import com.metavize.mvvm.tapi.MPipeManager;
import com.metavize.mvvm.tapi.PipeSpec;
import com.metavize.mvvm.tapi.PipelineFoundry;
import com.metavize.mvvm.tran.TransformContext;
import org.apache.log4j.Logger;

/**
 * A base class for transform instances. Provides lifetime control mechanism
 * and allows extending class to override methods called just before and
 * just after state changes.
 *
 * @author <a href="mailto:dmorris@metavize.com">Dirk Morris</a>
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 */
public abstract class CasingTransform extends AbstractTransform
{
    private final Logger logger = Logger.getLogger(CasingTransform.class);

    private final PipelineFoundry pipelineFoundry
        = MvvmContextFactory.context().pipelineFoundry();

    private MPipe insideMPipe;
    private MPipe outsideMPipe;

    // abstract methods -------------------------------------------------------

    protected abstract PipeSpec getInsidePipeSpec();

    protected abstract PipeSpec getOutsidePipeSpec();

    // public methods ---------------------------------------------------------

    public void dumpSessions()
    {
        if (insideMPipe != null) {
            insideMPipe.dumpSessions();
        }
        if (outsideMPipe != null) {
            outsideMPipe.dumpSessions();
        }
    }

    public IPSessionDesc[] liveSessionDescs()
    {
        if (insideMPipe == null || outsideMPipe == null) {
            return new IPSessionDesc[0];
        }
        // XXX Might want to merge these together to get one
        // list. (merge since byte count incorrect on inside of
        // casing)
        IPSessionDesc[] id = insideMPipe.liveSessionDescs();
        IPSessionDesc[] od = outsideMPipe.liveSessionDescs();
        IPSessionDesc[] retDescs = new IPSessionDesc[id.length + od.length];
        System.arraycopy(id, 0, retDescs, 0, id.length);
        System.arraycopy(od, 0, retDescs, id.length, od.length);
        return retDescs;
    }

    protected MPipe getInsideMPipe()
    {
        return insideMPipe;
    }

    protected MPipe getOutsideMPipe()
    {
        return outsideMPipe;
    }

    protected void connectMPipe()
    {
        if (insideMPipe != null  || outsideMPipe != null) {
            logger.warn("already started");
            return;
        }

        TransformContext ctx = getTransformContext();
        logger.info("starting transform " + getInsidePipeSpec().getName()
                    + "(" + ctx.getTid().getName() + ")");

        MPipeManager xm = MPipeManager.manager();

        insideMPipe = xm.plumbLocal(this, getInsidePipeSpec());
        outsideMPipe = xm.plumbLocal(this, getOutsidePipeSpec());

        pipelineFoundry.registerCasing(insideMPipe, outsideMPipe);

        return;
    }

    protected void disconnectMPipe()
    {
        pipelineFoundry.deregisterCasing(insideMPipe);
        insideMPipe.destroy();

        outsideMPipe.destroy();

        insideMPipe = outsideMPipe = null;
    }
}
