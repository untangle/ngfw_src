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

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

public abstract class AbstractTransform extends TransformBase
{
    private final Logger logger = Logger.getLogger(getClass());

    private PipeSpec[] pipeSpecs;

    // no-op methods ----------------------------------------------------------

    protected abstract PipeSpec[] getPipeSpecs();

    // TransformBase methods --------------------------------------------------

    protected void connectMPipe()
    {
        if (null == pipeSpecs) {
            PipeSpec[] pss = getPipeSpecs();
            pipeSpecs = null == pss ? new PipeSpec[0] : pss;
            for (PipeSpec ps : pipeSpecs) {
                ps.connectMPipe();
            }
        } else {
            logger.warn("MPipes already connected");
        }
    }

    protected void disconnectMPipe()
    {
        if (null != pipeSpecs) {
            for (PipeSpec ps : pipeSpecs) {
                ps.disconnectMPipe();
            }
            pipeSpecs = null;
        } else {
            logger.warn("MPipes not connected");
        }
    }

    // Transform methods ------------------------------------------------------

    public void dumpSessions()
    {
        for (PipeSpec ps : pipeSpecs) {
            ps.dumpSessions();
        }
    }

    public IPSessionDesc[] liveSessionDescs()
    {
        // XXX Might want to merge these together to get one
        // list. (merge since byte count incorrect on inside of
        // casing)
        List<IPSessionDesc> sds = new LinkedList<IPSessionDesc>();

        if (null != pipeSpecs) {
            for (PipeSpec ps : pipeSpecs) {
                for (IPSessionDesc isd : ps.liveSessionDescs()) {
                    sds.add(isd);
                }
            }
        }

        return sds.toArray(new IPSessionDesc[sds.size()]);
    }
}
