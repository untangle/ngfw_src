/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: PipelineFoundryImpl.java,v 1.20 2005/03/15 02:11:53 amread Exp $
 */

package com.metavize.mvvm.engine;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.metavize.mvvm.argon.IPSessionDesc;
import com.metavize.mvvm.tapi.Affinity;
import com.metavize.mvvm.tapi.Fitting;
import com.metavize.mvvm.tapi.MPipe;
import com.metavize.mvvm.tapi.PipeSpec;
import com.metavize.mvvm.tapi.Pipeline;
import com.metavize.mvvm.tapi.PipelineFoundry;
import com.metavize.mvvm.tran.PipelineEvent;
import com.metavize.mvvm.tran.PipelineInfo;
import org.apache.log4j.Logger;

class PipelineFoundryImpl implements PipelineFoundry
{
    private static final PipelineFoundryImpl PIPELINE_FOUNDRY_IMPL
        = new PipelineFoundryImpl();

    private static final Logger eventLogger = MvvmLocalContextImpl.context()
        .eventLogger();
    private static final Logger logger = Logger
        .getLogger(PipelineFoundryImpl.class);

    private final Map mPipes = new ConcurrentHashMap();
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
            logger.info("making an FTP_STREAM pipe");
            start = Fitting.FTP_STREAM;
            break;

        case 80:
            logger.info("making an HTTP_STREAM pipe");
            start = Fitting.HTTP_STREAM;
            break;

        default:
            logger.info("making an OCTET_STREAM pipe");
            start = Fitting.OCTET_STREAM;
            break;
        }

        // XXX eliminate pipes not matchin subscription
        Long t0 = System.currentTimeMillis();

        Set mp = matchingMPipes(sessionDesc, mPipes.keySet());
        Map mc = matchingCasings(sessionDesc, casings);
        List p = makePipe(start, mp, mc, new LinkedList());

        Long t1 = System.currentTimeMillis();
        if (logger.isDebugEnabled())
            logger.debug("Made pipe in " + (t1 - t0) + " millis, size: "
                         + p.size());

        Pipeline pipeline = new PipelineImpl(new PipelineInfo(sessionDesc));
        if (logger.isDebugEnabled())
            logger.debug("adding: " + pipeline + " for: " + sessionDesc.id());
        pipelines.put(sessionDesc.id(), pipeline);

        dumpPipe(p);
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
        PipeSpec ps = mPipe.getPipeSpec();
        if (ps.getInput() != ps.getOutput()) {
            // XXX i need to implement handling for this still
            throw new IllegalArgumentException("mPipe input != output");
        }
        mPipes.put(mPipe, this);
    }

    public void deregisterMPipe(MPipe mPipe)
    {
        mPipes.remove(mPipe);
    }

    public void registerCasing(MPipe insideMPipe, MPipe outsideMPipe)
    {
        PipeSpec psi = insideMPipe.getPipeSpec();
        PipeSpec pso = outsideMPipe.getPipeSpec();
        if (psi.getInput() != pso.getOutput()
            || psi.getOutput() != pso.getInput()) {
            logger.warn("PSI IN: " + psi.getInput() + " OUT: "
                        + psi.getOutput());
            logger.warn("PSO IN: " + pso.getInput() + " OUT: "
                        + pso.getOutput());
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
        Pipeline pipeline = (Pipeline)pipelines.get(sessionId);
        if (logger.isDebugEnabled())
            logger.debug("got: " +  pipeline + " for: " + sessionId);
        return pipeline;
    }

    // private methods --------------------------------------------------------

    private Set matchingMPipes(IPSessionDesc sessionDesc, Set mPipes)
    {
        Set s = new HashSet();
        for (Iterator i = mPipes.iterator(); i.hasNext(); ) {
            MPipe mPipe = (MPipe)i.next();
            PipeSpec pipeSpec = mPipe.getPipeSpec();
            if (pipeSpec.matches(sessionDesc)) {
                s.add(mPipe);
            }
        }

        return s;
    }

    private Map matchingCasings(IPSessionDesc sessionDesc, Map casings)
    {
        // XXX assumes inside & outside have same subscription.
        Map m = new HashMap();
        for (Iterator i = casings.keySet().iterator(); i.hasNext(); ) {
            MPipe mPipe = (MPipe)i.next();
            PipeSpec pipeSpec = mPipe.getPipeSpec();
            if (pipeSpec.matches(sessionDesc)) {
                m.put(mPipe, casings.get(mPipe));
            }
        }

        return m;
    }

    private List makePipe(Fitting type, Set fittings, Map casings,
                          List pipe)
    {
        addSegments(type, fittings, Affinity.BEGIN, pipe);

        TRY_AGAIN:
        for (Iterator i = casings.keySet().iterator(); i.hasNext(); ) {
            MPipe inside = (MPipe)i.next();
            if (inside.getPipeSpec().getInput() == type) {
                MPipe outside = (MPipe)casings.get(inside);
                i.remove();
                pipe.add(inside.getArgonAgent());
                makePipe(inside.getPipeSpec().getOutput(), fittings, casings,
                         pipe);
                pipe.add(outside.getArgonAgent());
                break TRY_AGAIN;
            }
        }

        addSegments(type, fittings, Affinity.END, pipe);

        return pipe;
    }

    private void addSegments(Fitting type, Set fittings, Affinity a, List pipe)
    {
        for (Iterator i = fittings.iterator(); i.hasNext(); ) {
            MPipe mPipe = (MPipe)i.next();
            Fitting f = mPipe.getPipeSpec().getInput();
            if (mPipe.getPipeSpec().getAffinity() == a && type.instanceOf(f)) {
                i.remove();
                pipe.add(mPipe.getArgonAgent());
            }
        }
    }


    private void dumpPipe(List l)
    {
        logger.info("PIPELINE: ");
        for (Iterator i = l.iterator(); i.hasNext(); ) {
            logger.info("  " + i.next());
        }
    }
}
