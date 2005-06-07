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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.metavize.mvvm.argon.ArgonAgent;
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
        = MvvmContextImpl.context().eventLogger();
    private static final Logger logger
        = Logger.getLogger(PipelineFoundryImpl.class);

    private final Map incomingMPipes = new ConcurrentHashMap();
    private final Map outgoingMPipes = new ConcurrentHashMap();
    private final Map casings = new ConcurrentHashMap();
    private final Map<InetSocketAddress, Fitting> connectionFittings
        = new ConcurrentHashMap<InetSocketAddress, Fitting>();
    private final Map pipelines = new ConcurrentHashMap();

    private PipelineFoundryImpl() { }

    public static PipelineFoundryImpl foundry()
    {
        return PIPELINE_FOUNDRY_IMPL;
    }

    // XXX probably return Pipeline instead
    public List<ArgonAgent> weld(IPSessionDesc sessionDesc)
    {
        Fitting start;

        InetAddress sAddr = sessionDesc.serverAddr();
        int sPort = sessionDesc.serverPort();

        InetSocketAddress socketAddress = new InetSocketAddress(sAddr, sPort);

        start = connectionFittings.remove(socketAddress);

        if (null == start) {
            switch (sPort) {
            case 21:
                start = Fitting.FTP_CTL_STREAM;
                break;

            case 25:
                start = Fitting.SMTP_STREAM;
                break;

            case 80:
                start = Fitting.HTTP_STREAM;
                break;

            default:
                start = Fitting.OCTET_STREAM;
                break;
            }
        }

        Long t0 = System.currentTimeMillis();

        Map<Fitting, List<MPipe>> mp
            = sessionDesc.clientIntf() == IntfConverter.OUTSIDE
            ? incomingMPipes : outgoingMPipes;
        Map<Fitting, List<MPipe>> availMPipes
            = new HashMap<Fitting, List<MPipe>>(mp);

        Map<MPipe, MPipe> availCasings = new HashMap(casings);

        List<MPipe> mPipes = new LinkedList<MPipe>();
        List<Fitting> fittings = new LinkedList<Fitting>();

        weld(mPipes, fittings, start, sessionDesc, availMPipes, availCasings);

        Long t1 = System.currentTimeMillis();

        PipelineInfo pipelineInfo = new PipelineInfo(sessionDesc);
        Pipeline pipeline = new PipelineImpl(mPipes, fittings, pipelineInfo);
        pipelines.put(sessionDesc.id(), pipeline);

        if (logger.isDebugEnabled()) {
            logger.debug("sid: " + sessionDesc.id() + " pipe in " + (t1 - t0)
                         + " millis: " + mPipes);
        }

        List<ArgonAgent> l = new ArrayList<ArgonAgent>(mPipes.size());
        for (MPipe mPipe : mPipes) {
            l.add(mPipe.getArgonAgent());
        }

        return l;
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

        pipeline.destroy();
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

    public void registerConnection(InetSocketAddress socketAddress,
                                   Fitting fitting)
    {
        connectionFittings.put(socketAddress, fitting);
    }

    public Pipeline getPipeline(int sessionId)
    {
        return (Pipeline)pipelines.get(sessionId);
    }

    // private methods --------------------------------------------------------

    private void weld(List<MPipe> mPipes, List<Fitting> fittings,
                      Fitting start, IPSessionDesc sd,
                      Map<Fitting, List<MPipe>> availMPipes,
                      Map<MPipe, MPipe> availCasings)
    {
        weldMPipes(mPipes, fittings, start, sd, availMPipes, availCasings);
        weldCasings(mPipes, fittings, start, sd, availMPipes, availCasings);
    }

    private void weldMPipes(List<MPipe> mPipes, List<Fitting> fittings,
                            Fitting start, IPSessionDesc sd,
                            Map<Fitting, List<MPipe>> availMPipes,
                            Map<MPipe, MPipe> availCasings)
    {
        TRY_AGAIN:
        for (Iterator<Fitting> i = availMPipes.keySet().iterator(); i.hasNext(); ) {
            Fitting f = i.next();
            if (start.instanceOf(f)) {
                List l = availMPipes.get(f);
                i.remove();
                for (Iterator<MPipe> j = l.iterator(); j.hasNext(); ) {
                    MPipe mPipe = j.next();
                    if (null == mPipe) {
                        weldCasings(mPipes, fittings, start, sd,
                                    availMPipes, availCasings);
                    } else {
                        if (mPipe.getPipeSpec().matches(sd)) {
                            mPipes.add(mPipe);
                            fittings.add(start);
                        }
                    }
                }
                break TRY_AGAIN;
            }
        }
    }

    private void weldCasings(List<MPipe> mPipes, List<Fitting> fittings,
                             Fitting start, IPSessionDesc sd,
                             Map<Fitting, List<MPipe>> availMPipes,
                             Map<MPipe, MPipe> availCasings)
    {
        TRY_AGAIN:
        for (Iterator<MPipe> i = availCasings.keySet().iterator(); i.hasNext(); ) {
            MPipe insideMPipe = i.next();
            CasingPipeSpec ps = (CasingPipeSpec)insideMPipe.getPipeSpec();
            Fitting f = ps.getInput();
            if (start.instanceOf(f) && ps.matches(sd)) {
                MPipe outsideMPipe = availCasings.get(insideMPipe);
                i.remove();
                mPipes.add(insideMPipe);
                fittings.add(start);

                CasingPipeSpec cps = (CasingPipeSpec)insideMPipe.getPipeSpec();
                Fitting insideFitting = cps.getOutput(start);
                weldMPipes(mPipes, fittings, insideFitting, sd,
                           availMPipes, availCasings);
                mPipes.add(outsideMPipe);
                fittings.add(insideFitting);
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
