/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.metavize.mvvm.argon.IPSessionDesc;
import com.metavize.mvvm.argon.IntfConverter;
import com.metavize.mvvm.tapi.CasingPipeSpec;
import com.metavize.mvvm.tapi.Fitting;
import com.metavize.mvvm.tapi.MPipe;
import com.metavize.mvvm.tapi.Pipeline;
import com.metavize.mvvm.tapi.PipelineFoundry;
import com.metavize.mvvm.tapi.SoloPipeSpec;
import com.metavize.mvvm.tran.PipelineEvent;
import com.metavize.mvvm.tran.PipelineInfo;
import org.apache.log4j.Logger;

class PipelineFoundryImpl implements PipelineFoundry
{
    private static final PipelineFoundryImpl PIPELINE_FOUNDRY_IMPL
        = new PipelineFoundryImpl();

    private static final Logger eventLogger
        = MvvmLocalContextImpl.context().eventLogger();
    private static final Logger logger
        = Logger.getLogger(PipelineFoundryImpl.class);

    private final Map incomingMPipes = new ConcurrentHashMap();
    private final Map outgoingMPipes = new ConcurrentHashMap();
    private final Map casings = new ConcurrentHashMap();
    private final Map pipelines = new ConcurrentHashMap();

    private PipelineFoundryImpl() { }

    public static PipelineFoundryImpl foundry()
    {
        return PIPELINE_FOUNDRY_IMPL;
    }

    // XXX probably return Pipeline instead
    public List weld(IPSessionDesc sessionDesc)
    {
        // XXX hack, determine initial type by port number
        Fitting start;
        switch (sessionDesc.serverPort()) {
        case 21:
            start = Fitting.FTP_STREAM;
            break;

        case 80:
            start = Fitting.HTTP_STREAM;
            break;

        default:
            start = Fitting.OCTET_STREAM;
            break;
        }

        Long t0 = System.currentTimeMillis();
        Map mPipes = sessionDesc.clientIntf() == IntfConverter.OUTSIDE
            ? incomingMPipes : outgoingMPipes;
        List p = new LinkedList();
        weld(p, start, sessionDesc, new HashMap(mPipes), new HashMap(casings));
        Long t1 = System.currentTimeMillis();

        Pipeline pipeline = new PipelineImpl(new PipelineInfo(sessionDesc));
        pipelines.put(sessionDesc.id(), pipeline);

        if (logger.isDebugEnabled()) {
            logger.debug("sid: " + sessionDesc.id() + " pipe in " + (t1 - t0)
                         + " millis: " + p);
        }

        return p;
    }

    public void destroy(IPSessionDesc start, IPSessionDesc end)
    {
        PipelineImpl pipeline = (PipelineImpl)pipelines.remove(start.id());
        if (logger.isDebugEnabled()) {
            logger.debug("removed: " + pipeline + " for: " + start.id());
        }

        PipelineInfo info = pipeline.getInfo();
        info.update(start, end);

        eventLogger.info(new PipelineEvent(info));
    }

    public void registerMPipe(MPipe mPipe)
    {
        registerMPipe(incomingMPipes, mPipe, new MPipeComparator(true));
        registerMPipe(outgoingMPipes, mPipe, new MPipeComparator(false));
    }

    public void deregisterMPipe(MPipe mPipe)
    {
        deregisterMPipe(incomingMPipes, mPipe, new MPipeComparator(true));
        deregisterMPipe(outgoingMPipes, mPipe, new MPipeComparator(false));
    }

    public void registerCasing(MPipe insideMPipe, MPipe outsideMPipe)
    {
        if (insideMPipe.getPipeSpec() != outsideMPipe.getPipeSpec()) {
            throw new IllegalArgumentException("casing constraint violated");
        }

        casings.put(insideMPipe, outsideMPipe);
    }

    public void deregisterCasing(MPipe insideMPipe)
    {
        casings.remove(insideMPipe);
    }

    public Pipeline getPipeline(int sessionId)
    {
        return (Pipeline)pipelines.get(sessionId);
    }

    // private methods --------------------------------------------------------

    private void weld(List p, Fitting start, IPSessionDesc sd, Map mPipes,
                      Map casings)
    {
        weldMPipes(p, start, sd, mPipes, casings);
        weldCasings(p, start, sd, mPipes, casings);
    }

    private void weldMPipes(List p, Fitting start, IPSessionDesc sd,
                            Map mPipes, Map casings)
    {
        TRY_AGAIN:
        for (Iterator i = mPipes.keySet().iterator(); i.hasNext(); ) {
            Fitting f = (Fitting)i.next();
            if (start.instanceOf(f)) {
                List l = (List)mPipes.get(f);
                i.remove();
                for (Iterator j = l.iterator(); j.hasNext(); ) {
                    MPipe mPipe = (MPipe)j.next();
                    if (null == mPipe) {
                        weldCasings(p, start, sd, mPipes, casings);
                    } else {
                        if (mPipe.getPipeSpec().matches(sd)) {
                            p.add(mPipe.getArgonAgent());
                        }
                    }
                }
                break TRY_AGAIN;
            }
        }
    }

    private void weldCasings(List p, Fitting start, IPSessionDesc sd,
                             Map mPipes, Map casings)
    {
        TRY_AGAIN:
        for (Iterator i = casings.keySet().iterator(); i.hasNext(); ) {
            MPipe insideMPipe = (MPipe)i.next();
            CasingPipeSpec ps = (CasingPipeSpec)insideMPipe.getPipeSpec();
            Fitting f = ps.getInput();
            if (start.instanceOf(f) && ps.matches(sd)) {
                MPipe outsideMPipe = (MPipe)casings.get(insideMPipe);
                i.remove();
                p.add(insideMPipe.getArgonAgent());

                CasingPipeSpec cps = (CasingPipeSpec)insideMPipe.getPipeSpec();
                weldMPipes(p, cps.getOutput(), sd, mPipes, casings);
                p.add(outsideMPipe.getArgonAgent());
                break TRY_AGAIN;
            }
        }
    }

    private void registerMPipe(Map mPipes, MPipe mPipe, Comparator c)
    {
        SoloPipeSpec sps = (SoloPipeSpec)mPipe.getPipeSpec();
        Fitting f = sps.getFitting();

        synchronized (this) {
            List l = (List)mPipes.get(f);

            if (null == l) {
                l = new ArrayList();
                l.add(null);
            } else {
                l = new ArrayList(l);
            }

            int i = Collections.binarySearch(l, mPipe, c);
            l.add(0 > i ? -i - 1 : i, mPipe);
            mPipes.put(f, l);
        }
    }

    private void deregisterMPipe(Map mPipes, MPipe mPipe, Comparator c)
    {
        SoloPipeSpec sps = (SoloPipeSpec)mPipe.getPipeSpec();
        Fitting f = sps.getFitting();

        synchronized (this) {
            List l = new ArrayList((List)mPipes.get(f));

            int i = Collections.binarySearch(l, mPipe, c);
            if (0 > i) {
                logger.warn("deregistering nonregistered pipe");
            } else {
                l.remove(i);
            }

            mPipes.put(f, l);
        }
    }
}
