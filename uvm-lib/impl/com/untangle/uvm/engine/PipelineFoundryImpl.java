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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.argon.ArgonAgent;
import com.untangle.uvm.argon.PipelineDesc;
import com.untangle.uvm.argon.SessionEndpoints;
import com.untangle.uvm.localapi.LocalIntfManager;
import com.untangle.uvm.logging.EventLogger;
import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.node.IPSessionDesc;
import com.untangle.uvm.node.InterfaceComparator;
import com.untangle.uvm.node.NodeManager;
import com.untangle.uvm.node.PipelineEndpoints;
import com.untangle.uvm.node.PipelineStats;
import com.untangle.uvm.policy.LocalPolicyManager;
import com.untangle.uvm.policy.Policy;
import com.untangle.uvm.policy.PolicyRule;
import com.untangle.uvm.policy.UserPolicyRule;
import com.untangle.uvm.vnet.CasingPipeSpec;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.ArgonConnector;
import com.untangle.uvm.vnet.PipeSpec;
import com.untangle.uvm.vnet.Pipeline;
import com.untangle.uvm.vnet.PipelineFoundry;
import com.untangle.uvm.vnet.SoloPipeSpec;
import com.untangle.uvm.vnet.event.SessionEventListener;

/**
 * Implements PipelineFoundry.
 * 
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class PipelineFoundryImpl implements PipelineFoundry
{
    private static final PipelineFoundryImpl PIPELINE_FOUNDRY_IMPL = new PipelineFoundryImpl();

    private static final EventLogger<LogEvent> eventLogger = UvmContextImpl.context().eventLogger();
    private final Logger logger = Logger.getLogger(getClass());

    private final Map<Fitting, List<ArgonConnector>> argonConnectors = new HashMap<Fitting, List<ArgonConnector>>();

    private final Map<ArgonConnector, ArgonConnector> casings = new HashMap<ArgonConnector, ArgonConnector>();

    private final Map<InetSocketAddress, Fitting> connectionFittings = new ConcurrentHashMap<InetSocketAddress, Fitting>();

    private final Map<Integer, PipelineImpl> pipelines = new ConcurrentHashMap<Integer, PipelineImpl>();

    // These don't need to be concurrent and being able to use a null key
    // is currently useful for the null policy.
    private static final Map<Policy, Map<Fitting, List<ArgonConnectorFitting>>> chains = new HashMap<Policy, Map<Fitting, List<ArgonConnectorFitting>>>();
    
    private PipelineFoundryImpl() {}

    public static PipelineFoundryImpl foundry()
    {
        return PIPELINE_FOUNDRY_IMPL;
    }

    public PipelineDesc weld(IPSessionDesc sd)
    {
        Long t0 = System.nanoTime();

        PolicyRule pr = selectPolicy(sd);
        if (pr == null) {
            logger.error("No policy rule found for session " + sd);
        }
        Policy policy = null == pr ? null : pr.getPolicy();


        InetAddress sAddr = sd.serverAddr();
        int sPort = sd.serverPort();

        InetSocketAddress socketAddress = new InetSocketAddress(sAddr, sPort);
        Fitting start = connectionFittings.remove(socketAddress);

        if (SessionEndpoints.PROTO_TCP == sd.protocol()) {
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

                case 110:
                    start = Fitting.POP_STREAM;
                    break;

                case 143:
                    start = Fitting.IMAP_STREAM;
                    break;

                default:
                    start = Fitting.OCTET_STREAM;
                    break;
                }
            }
        } else {
            /* UDP */
            start = Fitting.OCTET_STREAM; 
        }

        long ct0 = System.nanoTime();
        List<ArgonConnectorFitting> chain = makeChain(sd, policy, start);
        long ct1 = System.nanoTime();

        // filter list
        long ft0 = System.nanoTime();

        List<ArgonAgent> al = new ArrayList<ArgonAgent>(chain.size());
        List<ArgonConnectorFitting> ml = new ArrayList<ArgonConnectorFitting>(chain.size());

        ArgonConnector end = null;
                       
        for (Iterator<ArgonConnectorFitting> i = chain.iterator(); i.hasNext();) {
            ArgonConnectorFitting mpf = i.next();

            if (null != end) {
                if (mpf.argonConnector == end) {
                    end = null;
                }
            } else {
                ArgonConnector argonConnector = mpf.argonConnector;
                PipeSpec pipeSpec = argonConnector.getPipeSpec();

                // We want the node if its policy matches (this policy or one of
                // is parents), or the node has no
                // policy (is a service).
                if (pipeSpec.matches(sd)) {
                    ml.add(mpf);
                    al.add(((ArgonConnectorImpl) argonConnector).getArgonAgent());
                } else {
                    end = mpf.end;
                }
            }
        }

        long ft1 = System.nanoTime();

        PipelineImpl pipeline = new PipelineImpl(sd.id(), ml);
        pipelines.put(sd.id(), pipeline);

        Long t1 = System.nanoTime();
        if (logger.isDebugEnabled()) {
            logger.debug("sid: " + sd.id() +
                         " policy " + policy +
                         " policyManager " + UvmContextImpl.getInstance().localPolicyManager().getClass().getName());
            logger.debug("sid: " + sd.id() +
                         " pipe in " + (t1 - t0) +
                         " made: " + (ct1 - ct0) +
                         " filtered: " + (ft1 - ft0) +
                         " chain: " + ml);
        }

        return new PipelineDesc(pr, al);
    }

    public PipelineEndpoints createInitialEndpoints(IPSessionDesc start)
    {
        return new PipelineEndpoints(start);
    }

    public void registerEndpoints(PipelineEndpoints pe)
    {
        eventLogger.log(pe);
    }

    public void destroy(IPSessionDesc start, IPSessionDesc end, PipelineEndpoints pe, String uid)
    {
        PipelineImpl pipeline = pipelines.remove(start.id());

        if (logger.isDebugEnabled()) {
            logger.debug("removed: " + pipeline + " for: " + start.id());
        }

        // Endpoints can be null, if the session was never properly
        // set up at all (unknown server interface for example)
        if (pe != null)
            eventLogger.log(new PipelineStats(start, end, pe, uid));

        pipeline.destroy();
    }

    public ArgonConnector createArgonConnector(PipeSpec spec, SessionEventListener listener)
    {
        return new ArgonConnectorImpl(spec,listener);
    }

    public void registerArgonConnector(ArgonConnector argonConnector)
    {
        SoloPipeSpec sps = (SoloPipeSpec) argonConnector.getPipeSpec();
        Fitting f = sps.getFitting();

        List<ArgonConnector> l = argonConnectors.get(f);

        if (null == l) {
            l = new ArrayList<ArgonConnector>();
            l.add(null);
            argonConnectors.put(f, l);
        }

        int i = Collections.binarySearch(l, argonConnector, ArgonConnectorComparator.COMPARATOR);
        l.add(0 > i ? -i - 1 : i, argonConnector);

        clearCache();
    }

    public void deregisterArgonConnector(ArgonConnector argonConnector)
    {
        SoloPipeSpec sps = (SoloPipeSpec) argonConnector.getPipeSpec();
        Fitting f = sps.getFitting();

        List<ArgonConnector> l = argonConnectors.get(f);

        int i = Collections.binarySearch(l, argonConnector, ArgonConnectorComparator.COMPARATOR);
        if (0 > i) {
            logger.warn("deregistering nonregistered pipe");
        } else {
            l.remove(i);
        }

        clearCache();
    }

    public void registerCasing(ArgonConnector insideArgonConnector, ArgonConnector outsideArgonConnector)
    {
        if (insideArgonConnector.getPipeSpec() != outsideArgonConnector.getPipeSpec()) {
            throw new IllegalArgumentException("casing constraint violated");
        }

        synchronized (this) {
            casings.put(insideArgonConnector, outsideArgonConnector);
            clearCache();
        }
    }

    public void deregisterCasing(ArgonConnector insideArgonConnector)
    {
        synchronized (this) {
            casings.remove(insideArgonConnector);
            clearCache();
        }
    }

    public void registerConnection(InetSocketAddress socketAddress, Fitting fitting)
    {
        connectionFittings.put(socketAddress, fitting);
    }

    public Pipeline getPipeline(int sessionId)
    {
        return pipelines.get(sessionId);
    }
    
    /* Remove all of the cached chains */
    public void clearChains()
    {
        synchronized( this ) {
            clearCache();
        }
    }


    // package protected methods ----------------------------------------------

    PolicyRule selectPolicy(IPSessionDesc sd)
    {
        UvmContextImpl upi = UvmContextImpl.getInstance();
        LocalPolicyManager pmi = upi.localPolicyManager();
        LocalIntfManager im = upi.localIntfManager();
        UserPolicyRule[] userRules = pmi.getUserRules();
        InterfaceComparator c = im.getInterfaceComparator();

        for (UserPolicyRule upr : userRules) {
            if (upr.matches(sd, c)) {
                return upr;
            }
        }

        return pmi.getDefaultPolicyRule();
    }

    // private methods --------------------------------------------------------

    private List<ArgonConnectorFitting> makeChain(IPSessionDesc sd, Policy p, Fitting start)
    {
        List<ArgonConnectorFitting> argonConnectorFittings = null;

        /*
         * Check if there is a cache for this policy. First time is without the
         * lock
         */
        Map<Fitting, List<ArgonConnectorFitting>> fcs = chains.get(p);

        /* If there is a cache, check if the chain exists for this fitting */
        if (null != fcs) {
            argonConnectorFittings = fcs.get(start);
        }

        if (null == argonConnectorFittings) {
            synchronized (this) {
                /* Check if there is a cache again, after grabbing the lock */
                fcs = chains.get(p);

                if (null == fcs) {
                    /* Cache doesn't exist, create a new one */
                    fcs = new HashMap<Fitting, List<ArgonConnectorFitting>>();
                    chains.put(p, fcs);
                } else {
                    /* Cache exists, get the chain for this fitting */
                    argonConnectorFittings = fcs.get(start);
                }

                if (null == argonConnectorFittings) {
                    /*
                     * Chain hasn't been created, create a list of available
                     * casings
                     */
                    Map<ArgonConnector, ArgonConnector> availCasings = new HashMap<ArgonConnector, ArgonConnector>(
                            casings);

                    /*
                     * Chain hasn't been created, create a list of available
                     * nodes argonConnectors is ordered so iterating the list of argonConnectors
                     * will insert them in the correct order
                     */
                    Map<Fitting, List<ArgonConnector>> availArgonConnectors = new HashMap<Fitting, List<ArgonConnector>>(
                            argonConnectors);

                    int s = availCasings.size() + availArgonConnectors.size();
                    argonConnectorFittings = new ArrayList<ArgonConnectorFitting>(s);

                    /* Weld together the nodes and the casings */
                    weld(argonConnectorFittings, start, p, availArgonConnectors, availCasings);
                    
                    removeDuplicates(p, argonConnectorFittings);

                    fcs.put(start, argonConnectorFittings);
                }
            }
        }

        return argonConnectorFittings;
    }

    private void weld(List<ArgonConnectorFitting> argonConnectorFittings, Fitting start,
            Policy p, Map<Fitting, List<ArgonConnector>> availArgonConnectors,
            Map<ArgonConnector, ArgonConnector> availCasings)
    {
        weldArgonConnectors(argonConnectorFittings, start, p, availArgonConnectors, availCasings);
        weldCasings(argonConnectorFittings, start, p, availArgonConnectors, availCasings);
    }

    private boolean weldArgonConnectors(List<ArgonConnectorFitting> argonConnectorFittings, Fitting start,
            Policy p, Map<Fitting, List<ArgonConnector>> availArgonConnectors,
            Map<ArgonConnector, ArgonConnector> availCasings)
    {
        UvmContextImpl upi = UvmContextImpl.getInstance();
        LocalPolicyManager pmi = upi.localPolicyManager();

        boolean welded = false;

        boolean tryAgain;
        do {
            tryAgain = false;
            /* Iterate the Map Fittings to available Nodes */
            for (Iterator<Fitting> i = availArgonConnectors.keySet().iterator(); i
                    .hasNext();) {
                Fitting f = i.next();
                if (start.instanceOf(f)) {
                    /*
                     * If this fitting is an instance of the start, get the list
                     * of nodes
                     */
                    List<ArgonConnector> l = availArgonConnectors.get(f);

                    /* Remove this list of nodes from the available nodes */
                    i.remove();

                    for (Iterator<ArgonConnector> j = l.iterator(); j.hasNext();) {
                        ArgonConnector argonConnector = j.next();
                        if (null == argonConnector) {
                            boolean w = weldCasings(argonConnectorFittings, start, p,
                                    availArgonConnectors, availCasings);
                            if (w) {
                                welded = true;
                            }
                        } else if (pmi.matchesPolicy(argonConnector.getPipeSpec()
                                .getNode(), p)) {
                            ArgonConnectorFitting mpf = new ArgonConnectorFitting(argonConnector, start);
                            boolean w = argonConnectorFittings.add(mpf);
                            if (w) {
                                welded = true;
                            }
                        }
                    }
                    tryAgain = true;
                    break;
                }
            }
        } while (tryAgain);

        return welded;
    }

    private boolean weldCasings(List<ArgonConnectorFitting> argonConnectorFittings,
            Fitting start, Policy p, Map<Fitting, List<ArgonConnector>> availArgonConnectors,
            Map<ArgonConnector, ArgonConnector> availCasings)
    {
        UvmContextImpl upi = UvmContextImpl.getInstance();
        LocalPolicyManager pmi = upi.localPolicyManager();

        boolean welded = false;

        boolean tryAgain;
        do {
            tryAgain = false;
            for (Iterator<ArgonConnector> i = availCasings.keySet().iterator(); i
                    .hasNext();) {
                ArgonConnector insideArgonConnector = i.next();
                CasingPipeSpec ps = (CasingPipeSpec) insideArgonConnector.getPipeSpec();
                Fitting f = ps.getInput();

                if (!pmi.matchesPolicy(ps.getNode(), p)) {
                    i.remove();
                } else if (start.instanceOf(f)) {
                    ArgonConnector outsideArgonConnector = availCasings.get(insideArgonConnector);
                    i.remove();
                    int s = argonConnectorFittings.size();
                    argonConnectorFittings.add(new ArgonConnectorFitting(insideArgonConnector, start,
                            outsideArgonConnector));
                    CasingPipeSpec cps = (CasingPipeSpec) insideArgonConnector
                            .getPipeSpec();
                    Fitting insideFitting = cps.getOutput();

                    boolean w = weldArgonConnectors(argonConnectorFittings, insideFitting, p,
                            availArgonConnectors, availCasings);

                    if (w) {
                        welded = true;
                        argonConnectorFittings.add(new ArgonConnectorFitting(outsideArgonConnector,
                                insideFitting));
                    } else {
                        while (argonConnectorFittings.size() > s) {
                            argonConnectorFittings.remove(argonConnectorFittings.size() - 1);
                        }
                    }

                    tryAgain = true;
                    break;
                }
            }
        } while (tryAgain);

        return welded;
    }

    private void clearCache()
    {
        logger.debug("Clearing chains cache...");
        chains.clear();
    }
    
    private void removeDuplicates(Policy policy, List<ArgonConnectorFitting> chain)
    {
        LocalPolicyManager pmi = LocalUvmContextFactory.context().localPolicyManager();
        NodeManager nodeManager = LocalUvmContextFactory.context().nodeManager();

        Set<String> enabledNodes = nodeManager.getEnabledNodes(policy);

        Map<String, Integer> numParents = new HashMap<String, Integer>();
        Map<ArgonConnectorFitting, Integer> fittingDistance = new HashMap<ArgonConnectorFitting, Integer>();

        for (Iterator<ArgonConnectorFitting> i = chain.iterator(); i.hasNext();) {
            ArgonConnectorFitting mpf = i.next();

            Policy nodePolicy = mpf.argonConnector.node().getNodeId().getPolicy();

            if (nodePolicy == null) {
                continue;
            }

            String nodeName = mpf.argonConnector.node().getNodeDesc().getName();
            /* Remove the items that are not enabled in this policy */
            if (!enabledNodes.contains(nodeName)) {
                i.remove();
                continue;
            }

            Integer n = numParents.get(nodeName);
            int distance = pmi.getNumParents(policy, nodePolicy);

            if (distance < 0) {
                /* Removing nodes that are not in this policy */
                logger.info("The policy " + policy.getName()
                        + " is not a child of " + nodePolicy.getName());
                i.remove();
                continue;
            }

            fittingDistance.put(mpf, distance);

            /* If an existing node is closer then this node, remove this node. */
            if (n == null) {
                /*
                 * If we haven't seen another node at any distance, add it to
                 * the hash
                 */
                numParents.put(nodeName, distance);
                continue;
            } else if (distance == n) {
                /* Keep nodes at the same distance */
                continue;
            } else if (distance < n) {
                /*
                 * Current node is closer then the other one, have to remove the
                 * other node done on another iteration
                 */
                numParents.put(nodeName, distance);
            }
        }

        for (Iterator<ArgonConnectorFitting> i = chain.iterator(); i.hasNext();) {
            ArgonConnectorFitting mpf = i.next();

            Policy nodePolicy = mpf.argonConnector.node().getNodeId().getPolicy();

            /* Keep items in the NULL Racks */
            if (nodePolicy == null) {
                continue;
            }

            String nodeName = mpf.argonConnector.node().getNodeDesc().getName();

            Integer n = numParents.get(nodeName);

            if (n == null) {
                logger
                        .info("Programming error, numParents null for non-null policy.");
                continue;
            }

            Integer distance = fittingDistance.get(mpf);

            if (distance == null) {
                logger
                        .info("Programming error, distance null for a fitting.");
                continue;
            }

            if (distance > n) {
                i.remove();
            } else if (distance < n) {
                logger
                        .info("Programming error, numParents missing minimum value");
            }
        }

    }
    
}
