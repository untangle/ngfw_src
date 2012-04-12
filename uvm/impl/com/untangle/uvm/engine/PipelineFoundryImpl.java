/**
 * $Id$
 */
package com.untangle.uvm.engine;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.argon.ArgonAgent;
import com.untangle.uvm.argon.ArgonIPSessionDesc;
import com.untangle.uvm.argon.SessionEndpoints;
import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.node.IPSessionDesc;
import com.untangle.uvm.node.NodeManager;
import com.untangle.uvm.node.SessionEvent;
import com.untangle.uvm.node.SessionStatsEvent;
import com.untangle.uvm.node.PolicyManager;
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
 */
public class PipelineFoundryImpl implements PipelineFoundry
{
    private static final PipelineFoundryImpl PIPELINE_FOUNDRY_IMPL = new PipelineFoundryImpl();

    private final Logger logger = Logger.getLogger(getClass());

    private final Map<Fitting, List<ArgonConnector>> argonConnectors = new HashMap<Fitting, List<ArgonConnector>>();

    private final Map<ArgonConnector, ArgonConnector> casings = new HashMap<ArgonConnector, ArgonConnector>();

    private final Map<InetSocketAddress, Fitting> connectionFittings = new ConcurrentHashMap<InetSocketAddress, Fitting>();

    private final Map<Long, PipelineImpl> pipelines = new ConcurrentHashMap<Long, PipelineImpl>();

    /**
     * This stores a map from policyId to something XXX
     */
    private static final Map<Long, Map<Fitting, List<ArgonConnectorFitting>>> chains = new HashMap<Long, Map<Fitting, List<ArgonConnectorFitting>>>();
    
    private PipelineFoundryImpl() {}

    public static PipelineFoundryImpl foundry()
    {
        return PIPELINE_FOUNDRY_IMPL;
    }

    public List<ArgonAgent> weld(IPSessionDesc sessionDesc, Long policyId)
    {
        Long t0 = System.nanoTime();

        InetAddress sAddr = sessionDesc.serverAddr();
        int sPort = sessionDesc.serverPort();

        InetSocketAddress socketAddress = new InetSocketAddress(sAddr, sPort);
        Fitting start = connectionFittings.remove(socketAddress);

        if (SessionEndpoints.PROTO_TCP == sessionDesc.protocol()) {
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
        List<ArgonConnectorFitting> chain = makeChain(sessionDesc, policyId, start);
        long ct1 = System.nanoTime();

        // filter list
        long ft0 = System.nanoTime();

        List<ArgonAgent> argonAgentList = new ArrayList<ArgonAgent>(chain.size());
        List<ArgonConnectorFitting> acFittingList = new ArrayList<ArgonConnectorFitting>(chain.size());

        ArgonConnector end = null;

        String nodeList = "nodes: [";
        for (Iterator<ArgonConnectorFitting> i = chain.iterator(); i.hasNext();) {
            ArgonConnectorFitting acFitting = i.next();

            if (null != end) {
                if (acFitting.argonConnector == end) {
                    end = null;
                }
            } else {
                ArgonConnector argonConnector = acFitting.argonConnector;
                PipeSpec pipeSpec = argonConnector.getPipeSpec();

                // We want the node if its policy matches (this policy or one of
                // is parents), or the node has no
                // policy (is a service).
                if (pipeSpec.matches(sessionDesc)) {
                    acFittingList.add(acFitting);
                    argonAgentList.add(((ArgonConnectorImpl) argonConnector).getArgonAgent());
                    nodeList += pipeSpec.getName() + " ";
                } else {
                    end = acFitting.end;
                }
            }
        }
        nodeList += "]";

        long ft1 = System.nanoTime();

        PipelineImpl pipeline = new PipelineImpl(sessionDesc.id(), acFittingList);
        pipelines.put(sessionDesc.id(), pipeline);

        Long t1 = System.nanoTime();
        if (logger.isDebugEnabled()) {
            logger.debug("session_id: " + sessionDesc.id() +
                         " policyId: " + policyId + " " +
                         nodeList );
            logger.debug("session_id: " + sessionDesc.id() +
                         " pipe in " + (t1 - t0) +
                         " made: " + (ct1 - ct0) +
                         " filtered: " + (ft1 - ft0) +
                         " chain: " + acFittingList);
        }

        return argonAgentList;
    }

    public SessionEvent createInitialSessionEvent(IPSessionDesc start, String username, String hostname)
    {
        return new SessionEvent(start, username, hostname);
    }

    public void registerEndpoints(SessionEvent pe)
    {
        UvmContextFactory.context().logEvent(pe);
    }

    public void destroy(ArgonIPSessionDesc start, ArgonIPSessionDesc end, SessionEvent pe)
    {
        PipelineImpl pipeline = pipelines.remove(start.id());

        if (logger.isDebugEnabled()) {
            logger.debug("removed: " + pipeline + " for: " + start.id());
        }

        // Endpoints can be null, if the session was never properly
        // set up at all (unknown server interface for example)
        if (pe != null)
            UvmContextFactory.context().logEvent(new SessionStatsEvent(start, end, pe));

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

    public Pipeline getPipeline(long sessionId)
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

    protected List<PipelineImpl> getCurrentPipelines()
    {
        return new LinkedList<PipelineImpl>(this.pipelines.values());
    }
    
    // private methods --------------------------------------------------------

    private List<ArgonConnectorFitting> makeChain(IPSessionDesc sessionDesc, Long policyId, Fitting start)
    {
        List<ArgonConnectorFitting> argonConnectorFittings = null;

        /*
         * Check if there is a cache for this policy. First time is without the
         * lock
         */
        Map<Fitting, List<ArgonConnectorFitting>> fcs = chains.get(policyId);

        /* If there is a cache, check if the chain exists for this fitting */
        if (null != fcs) {
            argonConnectorFittings = fcs.get(start);
        }

        if (null == argonConnectorFittings) {
            synchronized (this) {
                /* Check if there is a cache again, after grabbing the lock */
                fcs = chains.get(policyId);

                if (null == fcs) {
                    /* Cache doesn't exist, create a new one */
                    fcs = new HashMap<Fitting, List<ArgonConnectorFitting>>();
                    chains.put(policyId, fcs);
                } else {
                    /* Cache exists, get the chain for this fitting */
                    argonConnectorFittings = fcs.get(start);
                }

                if (null == argonConnectorFittings) {
                    /*
                     * Chain hasn't been created, create a list of available
                     * casings
                     */
                    Map<ArgonConnector, ArgonConnector> availCasings = new HashMap<ArgonConnector, ArgonConnector>(casings);

                    /*
                     * Chain hasn't been created, create a list of available
                     * nodes argonConnectors is ordered so iterating the list of argonConnectors
                     * will insert them in the correct order
                     */
                    Map<Fitting, List<ArgonConnector>> availArgonConnectors = new HashMap<Fitting, List<ArgonConnector>>( argonConnectors );

                    int s = availCasings.size() + availArgonConnectors.size();
                    argonConnectorFittings = new ArrayList<ArgonConnectorFitting>(s);

                    /* Weld together the nodes and the casings */
                    weld( argonConnectorFittings, start, policyId, availArgonConnectors, availCasings );
                    

                    removeDuplicates( policyId, argonConnectorFittings );

                    fcs.put( start, argonConnectorFittings );
                }
            }
        }

        return argonConnectorFittings;
    }

    private void weld( List<ArgonConnectorFitting> argonConnectorFittings, Fitting start,
                       Long policyId, Map<Fitting, List<ArgonConnector>> availArgonConnectors,
                       Map<ArgonConnector, ArgonConnector> availCasings)
    {
        weldArgonConnectors(argonConnectorFittings, start, policyId, availArgonConnectors, availCasings);
        weldCasings(argonConnectorFittings, start, policyId, availArgonConnectors, availCasings);
    }

    private boolean weldArgonConnectors(List<ArgonConnectorFitting> argonConnectorFittings, Fitting start,
                                        Long policyId, Map<Fitting, List<ArgonConnector>> availArgonConnectors,
                                        Map<ArgonConnector, ArgonConnector> availCasings)
    {
        boolean welded = false;

        boolean tryAgain;
        do {
            tryAgain = false;
            /* Iterate the Map Fittings to available Nodes */
            for (Iterator<Fitting> i = availArgonConnectors.keySet().iterator(); i.hasNext();) {
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
                            boolean w = weldCasings(argonConnectorFittings, start, policyId, availArgonConnectors, availCasings);
                            if (w) {
                                welded = true;
                            }
                        } else if (policyMatch(argonConnector.getPipeSpec().getNode().getPolicyId(), policyId)) {
                            ArgonConnectorFitting acFitting = new ArgonConnectorFitting(argonConnector, start);
                            boolean w = argonConnectorFittings.add(acFitting);
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

    private boolean weldCasings(List<ArgonConnectorFitting> argonConnectorFittings, Fitting start,
                                Long policyId, Map<Fitting, List<ArgonConnector>> availArgonConnectors,
                                Map<ArgonConnector, ArgonConnector> availCasings)
    {
        boolean welded = false;

        boolean tryAgain;
        do {
            tryAgain = false;
            for (Iterator<ArgonConnector> i = availCasings.keySet().iterator(); i.hasNext();) {
                ArgonConnector insideArgonConnector = i.next();
                CasingPipeSpec ps = (CasingPipeSpec) insideArgonConnector.getPipeSpec();
                Fitting f = ps.getInput();

                if (!policyMatch(ps.getNode().getPolicyId(), policyId)) {
                    i.remove();
                } else if (start.instanceOf(f)) {
                    ArgonConnector outsideArgonConnector = availCasings.get(insideArgonConnector);
                    i.remove();
                    int s = argonConnectorFittings.size();
                    argonConnectorFittings.add(new ArgonConnectorFitting(insideArgonConnector, start, outsideArgonConnector));
                    CasingPipeSpec cps = (CasingPipeSpec) insideArgonConnector.getPipeSpec();
                    Fitting insideFitting = cps.getOutput();

                    boolean w = weldArgonConnectors(argonConnectorFittings, insideFitting, policyId, availArgonConnectors, availCasings);

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
    
    private void removeDuplicates(Long policyId, List<ArgonConnectorFitting> chain)
    {
        Set<String> enabledNodes = UvmContextFactory.context().nodeManager().getEnabledNodes(policyId);

        Map<String, Integer> numParents = new HashMap<String, Integer>();
        Map<ArgonConnectorFitting, Integer> fittingDistance = new HashMap<ArgonConnectorFitting, Integer>();

        for (Iterator<ArgonConnectorFitting> i = chain.iterator(); i.hasNext();) {
            ArgonConnectorFitting acFitting = i.next();

            Long nodePolicyId = acFitting.argonConnector.node().getNodeSettings().getPolicyId();

            if (nodePolicyId == null) {
                continue;
            }

            String nodeName = acFitting.argonConnector.node().getNodeDesc().getName();
            /* Remove the items that are not enabled in this policy */
            if (!enabledNodes.contains(nodeName)) {
                i.remove();
                continue;
            }

            Integer n = numParents.get(nodeName);
            int distance = getPolicyGenerationDiff(policyId, nodePolicyId);

            if (distance < 0) {
                /* Removing nodes that are not in this policy */
                logger.info("The policy " + policyId + " is not a child of " + nodePolicyId);
                i.remove();
                continue;
            }

            fittingDistance.put(acFitting, distance);

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
            ArgonConnectorFitting acFitting = i.next();

            Long nodePolicyId = acFitting.argonConnector.node().getNodeSettings().getPolicyId();

            /* Keep items in the NULL Racks */
            if (nodePolicyId == null) {
                continue;
            }

            String nodeName = acFitting.argonConnector.node().getNodeDesc().getName();

            Integer n = numParents.get(nodeName);

            if (n == null) {
                logger.info("Programming error, numParents null for non-null policy.");
                continue;
            }

            Integer distance = fittingDistance.get(acFitting);

            if (distance == null) {
                logger.info("Programming error, distance null for a fitting.");
                continue;
            }

            if (distance > n) {
                i.remove();
            } else if (distance < n) {
                logger.info("Programming error, numParents missing minimum value");
            }
        }

    }

    public int getPolicyGenerationDiff(Long childId, Long parentId)
    {
        PolicyManager policyManager = (PolicyManager) UvmContextFactory.context().nodeManager().node("untangle-node-policy");

        if ( policyManager != null )
            return policyManager.getPolicyGenerationDiff( childId, parentId );

        if ( childId == null ) {
            return 0;
        }
        
        if ( childId.equals( parentId ) ) {
            return 0;
        }
        
        return -1;
    }

    private boolean policyMatch(Long nodePolicy, Long policyId)
    {
        /**
         * If nodePolicy is null its a service so it matches all policies
         */
        if ( nodePolicy == null )
            return true;

        /**
         * Otherwise test for equality
         */
        if ( nodePolicy == null || policyId == null )
            return (nodePolicy == policyId);
        else
            return nodePolicy.equals(policyId);
    }
}
