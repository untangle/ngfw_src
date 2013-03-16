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
import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.node.SessionTuple;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.NodeManager;
import com.untangle.uvm.node.NodeSettings;
import com.untangle.uvm.node.SessionEvent;
import com.untangle.uvm.node.SessionStatsEvent;
import com.untangle.uvm.node.PolicyManager;
import com.untangle.uvm.argon.ArgonSession;
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

    /**
     * This stores a list of "hints" about connections and what fitting types they are
     * If an app knows what kind of connection/fitting should be used for a connection from the given address/port
     * It can register a hint so the pipeline foundry will treat the session accordingly
     */
    private final Map<InetSocketAddress, Fitting> fittingHints = new ConcurrentHashMap<InetSocketAddress, Fitting>();

    private final Map<Long, PipelineImpl> pipelines = new ConcurrentHashMap<Long, PipelineImpl>();

    /**
     * This stores a map from policyId to something XXX
     */
    private static final Map<Long, Map<Fitting, List<ArgonConnector>>> chains = new HashMap<Long, Map<Fitting, List<ArgonConnector>>>();
    
    private PipelineFoundryImpl() {}

    public static PipelineFoundryImpl foundry()
    {
        return PIPELINE_FOUNDRY_IMPL;
    }

    /**
     * "weld" is builds a list of all the interested argonAgents for a given session
     * It does so based on the given policyId and all the nodes/apps given subscriptions.
     */
    public List<ArgonAgent> weld( Long sessionId, SessionTuple sessionTuple, Long policyId )
    {
        Long t0 = System.nanoTime();

        Fitting start = null;
        
        /**
         * Check fittingHints for hints
         */
        InetSocketAddress socketAddress = new InetSocketAddress( sessionTuple.getServerAddr(), sessionTuple.getServerPort() );
        if ( fittingHints.containsKey( socketAddress )) {
            start = fittingHints.remove( socketAddress );
        }

        /**
         * Check for known ports and set fitting type accordingly
         */
        if ( start == null ) {
            if ( sessionTuple.getProtocol() == SessionTuple.PROTO_TCP ) {
                switch ( sessionTuple.getServerPort() ) {
                case 21:
                    start = Fitting.FTP_CTL_STREAM;
                    break;
                case 25:
                    start = Fitting.SMTP_STREAM;
                    break;
                case 80:
                    start = Fitting.HTTP_STREAM;
                    break;
                case 443:
                    start = Fitting.HTTPS_STREAM;
                    break;
                default:
                    start = Fitting.OCTET_STREAM;
                    break;
                }
            }
        }

        /**
         * If the fitting type still isn't known its just an octet stream
         */
        if ( start == null ) {
            start = Fitting.OCTET_STREAM; 
        }

        long ct0 = System.nanoTime();
        List<ArgonConnector> chain = weldForFitting( sessionTuple, policyId, start );
        long ct1 = System.nanoTime();

        // filter list
        long ft0 = System.nanoTime();

        List<ArgonAgent> argonAgentList = new ArrayList<ArgonAgent>(chain.size());
        List<ArgonConnector> argonConnectorList = new ArrayList<ArgonConnector>(chain.size());

        String nodeList = "nodes: [";
        for (Iterator<ArgonConnector> i = chain.iterator(); i.hasNext();) {
            ArgonConnector argonConnector = i.next();
            PipeSpec pipeSpec = argonConnector.getPipeSpec();

            // We want the node if its policy matches (this policy or one of
            // is parents), or the node has no
            // policy (is a service).
            if (pipeSpec.matches(sessionTuple)) {
                argonConnectorList.add( argonConnector );
                argonAgentList.add( ((ArgonConnectorImpl) argonConnector).getArgonAgent() );
                nodeList += pipeSpec.getName() + " ";
            }
        }
        nodeList += "]";

        long ft1 = System.nanoTime();

        PipelineImpl pipeline = new PipelineImpl(sessionId, argonConnectorList);
        pipelines.put(sessionId, pipeline);

        Long t1 = System.nanoTime();
        if (logger.isDebugEnabled()) {
            logger.debug("session_id: " + sessionId +
                         " policyId: " + policyId + " " +
                         nodeList );
            logger.debug("session_id: " + sessionId +
                         " pipe in " + (t1 - t0) +
                         " made: " + (ct1 - ct0) +
                         " filtered: " + (ft1 - ft0) +
                         " chain: " + argonConnectorList);
        }

        return argonAgentList;
    }

    public void destroy( long sessionId )
    {
        PipelineImpl pipeline = pipelines.remove( sessionId );

        if (logger.isDebugEnabled()) {
            logger.debug("removed: " + pipeline + " for: " + sessionId);
        }

        pipeline.destroy();
    }

    public ArgonConnector createArgonConnector(PipeSpec spec, SessionEventListener listener, Fitting input, Fitting output)
    {
        return new ArgonConnectorImpl( spec,listener, input, output );
    }

    public synchronized void registerArgonConnector(ArgonConnector argonConnector)
    {
        SoloPipeSpec sps = (SoloPipeSpec) argonConnector.getPipeSpec();
        Fitting fitting = sps.getFitting();

        List<ArgonConnector> argonConnectorList = argonConnectors.get(fitting);

        if (argonConnectorList == null) {
            argonConnectorList = new ArrayList<ArgonConnector>();
            argonConnectorList.add(null);
            argonConnectors.put(fitting, argonConnectorList);
        }

        int i = Collections.binarySearch(argonConnectorList, argonConnector, ArgonConnectorComparator.COMPARATOR);
        argonConnectorList.add(0 > i ? -i - 1 : i, argonConnector);

        clearCache();
    }

    public void deregisterArgonConnector(ArgonConnector argonConnector)
    {
        SoloPipeSpec sps = (SoloPipeSpec) argonConnector.getPipeSpec();
        Fitting fitting = sps.getFitting();

        List<ArgonConnector> argonConnectorList = argonConnectors.get(fitting);

        int i = Collections.binarySearch(argonConnectorList, argonConnector, ArgonConnectorComparator.COMPARATOR);
        if ( i < 0 ) {
            logger.warn("Deregistering non-registered pipe: " + argonConnector, new Exception());
        } else {
            argonConnectorList.remove(i);
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

    /**
     * registerConnection tells PipelineFoundry that connections from the socketAddress address/port pair
     * is the following type of fitting.
     * It is used only by the FTP-casing currently to tell use which connections are FTP_DATA_STREAM connections
     */
    public void addConnectionFittingHint( InetSocketAddress socketAddress, Fitting fitting )
    {
        fittingHints.put(socketAddress, fitting);
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

    private List<ArgonConnector> weldForFitting(SessionTuple sessionTuple, Long policyId, Fitting start)
    {
        List<ArgonConnector> argonConnectorList = null;

        /*
         * Check if there is a cache for this policy. First time is without the
         * lock
         */
        Map<Fitting, List<ArgonConnector>> fcs = chains.get(policyId);

        /* If there is a cache, check if the chain exists for this fitting */
        if (null != fcs) {
            argonConnectorList = fcs.get(start);
        }

        if ( argonConnectorList == null ) {
            synchronized (this) {
                /* Check if there is a cache again, after grabbing the lock */
                fcs = chains.get(policyId);

                if ( fcs == null ) {
                    /* Cache doesn't exist, create a new one */
                    fcs = new HashMap<Fitting, List<ArgonConnector>>();
                    chains.put(policyId, fcs);
                } else {
                    /* Cache exists, get the chain for this fitting */
                    argonConnectorList = fcs.get(start);
                }

                if ( argonConnectorList == null ) {
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
                    argonConnectorList = new ArrayList<ArgonConnector>(s);

                    /* Weld together the nodes and the casings */
                    weld( argonConnectorList, start, policyId, availArgonConnectors, availCasings );

                    removeDuplicates( policyId, argonConnectorList );

                    fcs.put( start, argonConnectorList );
                }
            }
        }

        return argonConnectorList;
    }

    private void weld( List<ArgonConnector> argonConnectorList, Fitting start,
                       Long policyId, Map<Fitting, List<ArgonConnector>> availArgonConnectors,
                       Map<ArgonConnector, ArgonConnector> availCasings)
    {
        weldArgonConnectors(argonConnectorList, start, policyId, availArgonConnectors, availCasings);
        weldCasings(argonConnectorList, start, policyId, availArgonConnectors, availCasings);
    }

    private boolean weldArgonConnectors( List<ArgonConnector> argonConnectorList,
                                         Fitting start,
                                         Long policyId,
                                         Map<Fitting, List<ArgonConnector>> availArgonConnectors,
                                         Map<ArgonConnector, ArgonConnector> availCasings )
    {
        boolean welded = false;

        boolean tryAgain;
        do {
            tryAgain = false;

            /**
             * Iterate through all the argonConnections and look for ones that fit the current fitting type
             */
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
                        if ( argonConnector == null ) {
                            boolean w = weldCasings(argonConnectorList, start, policyId, availArgonConnectors, availCasings);
                            if (w) {
                                welded = true;
                            }
                        } else if (policyMatch(argonConnector.getPipeSpec().getNode().getNodeSettings().getPolicyId(), policyId)) {
                            boolean w = argonConnectorList.add( argonConnector );
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

    private boolean weldCasings(List<ArgonConnector> argonConnectorList, Fitting start,
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

                if (!policyMatch(ps.getNode().getNodeSettings().getPolicyId(), policyId)) {
                    i.remove();
                } else if (start.instanceOf(f)) {
                    ArgonConnector outsideArgonConnector = availCasings.get(insideArgonConnector);
                    i.remove();
                    int s = argonConnectorList.size();

                    argonConnectorList.add( insideArgonConnector );
                    
                    CasingPipeSpec cps = (CasingPipeSpec) insideArgonConnector.getPipeSpec();
                    Fitting insideFitting = cps.getOutput();

                    boolean w = weldArgonConnectors(argonConnectorList, insideFitting, policyId, availArgonConnectors, availCasings);

                    if (w) {
                        welded = true;
                        argonConnectorList.add( outsideArgonConnector );
                    } else {
                        while (argonConnectorList.size() > s) {
                            argonConnectorList.remove(argonConnectorList.size() - 1);
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
    
    private void removeDuplicates( Long policyId, List<ArgonConnector> chain )
    {
        Map<String, Integer> numParents = new HashMap<String, Integer>();
        Map<ArgonConnector, Integer> fittingDistance = new HashMap<ArgonConnector, Integer>();

        List<String> enabledNodesInPolicy = new LinkedList<String>();
        List<Node> nodesInPolicy = UvmContextFactory.context().nodeManager().nodeInstances( policyId );
        for (Node node : nodesInPolicy) {
            if (node.getRunState() == NodeSettings.NodeState.RUNNING)
                enabledNodesInPolicy.add(node.getNodeProperties().getName());
        }
        
        for (Iterator<ArgonConnector> i = chain.iterator(); i.hasNext();) {
            ArgonConnector argonConnector = i.next();

            Long nodePolicyId = argonConnector.node().getNodeSettings().getPolicyId();

            if (nodePolicyId == null) {
                continue;
            }

            String nodeName = argonConnector.node().getNodeProperties().getName();

            /**
             * Remove the items that are not enabled in this policy
             * This is to ensure that if an app is in the child and not enable, it is not inherited from the parent
             */
            if (!enabledNodesInPolicy.contains(nodeName)) {
                i.remove();
                continue;
            }


            Integer n = numParents.get(nodeName);
            int distance = getPolicyGenerationDiff(policyId, nodePolicyId);

            if (distance < 0) {
                /* Removing nodes that are not in this policy */
                logger.debug("The policy " + policyId + " is not a child of " + nodePolicyId);
                i.remove();
                continue;
            }

            fittingDistance.put(argonConnector, distance);

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

        for (Iterator<ArgonConnector> i = chain.iterator(); i.hasNext();) {
            ArgonConnector argonConnector = i.next();

            Long nodePolicyId = argonConnector.node().getNodeSettings().getPolicyId();

            /* Keep items in the NULL Racks */
            if (nodePolicyId == null) {
                continue;
            }

            String nodeName = argonConnector.node().getNodeProperties().getName();

            Integer n = numParents.get(nodeName);

            if (n == null) {
                logger.warn("numParents null for non-null policy.");
                continue;
            }

            Integer distance = fittingDistance.get( argonConnector );

            if (distance == null) {
                logger.warn("null distance for a fitting.");
                continue;
            }

            if (distance > n) {
                i.remove();
            } else if (distance < n) {
                logger.warn("numParents missing minimum value");
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

    private boolean policyMatch( Long nodePolicy, Long policyId )
    {
        /**
         * This returns true if the nodePolicy would process the session on policyId
         * This is true if nodePolicy == null (its a service app and thus processes all sessions)
         * This is true if policyId == nodePolicy (its a filtering app and lives in the policyId rack)
         * or if one of policyId's parents' policyId == nodePolicy. (its a filtering app and lives one of policyId rack's parents, grandparents, etc)
         */

        PolicyManager policyManager = (PolicyManager) UvmContextFactory.context().nodeManager().node("untangle-node-policy");

        /**
         * If nodePolicy is null its a service so it matches all policies
         */
        if ( nodePolicy == null )
            return true;

        /**
         * policyId == null means "No Rack"
         * so no nodes match this policy (except services which are handled above)
         */
        if ( policyId == null ) {
            return false;
        }
        
        /**
         * Otherwise test for equality
         */
        if ( policyId.equals(nodePolicy) )
            return true;

        /**
         * Now check the parents if policyManager exists otherwise return false
         */
        if ( policyManager == null )
            return false;

        /**
         * Recursively check the parent rack of the nodePolicy
         */
        for ( Long parentId = policyManager.getParentPolicyId( policyId ) ; parentId != null ; parentId = policyManager.getParentPolicyId( parentId ) ) {
            /**
             * does this node live in the parent of the session's policy?
             * if so then this node should process this session
             * dupes will be removed later...
             */
            if ( parentId.equals( nodePolicy ) )
                return true;
        }

        return false;
    }
}
