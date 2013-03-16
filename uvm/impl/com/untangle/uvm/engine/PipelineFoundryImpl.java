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
 * PipelineFoundry is responsible for building a list of processing nodes for each session
 *
 * When new sessions are created weld() is called to create the list of <code>ArgonConnectors</code>
 * weld() first finds a list of all ArgonConnectors for the given policyId and fitting type (stream, http, etc)
 * From there it removes the uninterested ArgonConnectors.
 * What is left is a list of all the ArgonConnectors that participate in a given session.
 */
public class PipelineFoundryImpl implements PipelineFoundry
{
    private final Logger logger = Logger.getLogger(getClass());

    /**
     * Global PipelineFoundryImpl instance
     */
    private static final PipelineFoundryImpl PIPELINE_FOUNDRY_IMPL = new PipelineFoundryImpl();

    /**
     * A global list of all current argon connectors
     */
    private final List<ArgonConnector> argonConnectors = new LinkedList<ArgonConnector>();

    /**
     * A global list of all current casings
     */
    private final Map<ArgonConnector, ArgonConnector> casings = new HashMap<ArgonConnector, ArgonConnector>();

    /**
     * This stores a list of "hints" about connections and what fitting types they are
     * If an app knows what kind of connection/fitting should be used for a connection from the given address/port
     * It can register a hint so the pipeline foundry will treat the session accordingly
     */
    private final Map<InetSocketAddress, Fitting> fittingHints = new ConcurrentHashMap<InetSocketAddress, Fitting>();

    /**
     * This is a list of all current existing pipelines/sessions
     */
    private final Map<Long, PipelineImpl> pipelines = new ConcurrentHashMap<Long, PipelineImpl>();

    /**
     * This stores a map from policyId to a cache for that policy storing the list of argon connectors for various fitting types
     */
    private static final Map<Long, Map<Fitting, List<ArgonConnector>>> pipelineFoundryCache = new HashMap<Long, Map<Fitting, List<ArgonConnector>>>();
    
    /**
     * Private constructor to ensure singleton
     */
    private PipelineFoundryImpl() {}

    /**
     * Return the singleton instance of the PipelineFoundry
     */
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
        List<ArgonConnector> argonConnectorList = new LinkedList<ArgonConnector>();
        List<Fitting> fittings = new LinkedList<Fitting>();
        
        /**
         * Check fittingHints for hints
         */
        InetSocketAddress socketAddress = new InetSocketAddress( sessionTuple.getServerAddr(), sessionTuple.getServerPort() );
        if ( fittingHints.containsKey( socketAddress )) {
            Fitting hint = fittingHints.remove( socketAddress );
            if ( hint != null )
                fittings.add( hint );
        }

        /**
         * Check for known ports and set fitting type accordingly
         */
        if ( sessionTuple.getProtocol() == SessionTuple.PROTO_TCP ) {
            switch ( sessionTuple.getServerPort() ) {
            case 21:
                fittings.add( Fitting.FTP_CTL_STREAM );
                break;
            case 25:
                fittings.add( Fitting.SMTP_STREAM );
                break;
            case 80:
                fittings.add( Fitting.HTTP_STREAM );
                break;
            case 443:
                fittings.add( Fitting.HTTPS_STREAM );
                break;
            default:
                fittings.add( Fitting.OCTET_STREAM );
                break;
            }
        }

        /**
         * all sessions have octect_stream fitting
         */
        fittings.add( Fitting.OCTET_STREAM );

        long ct0 = System.nanoTime();
        for ( Fitting fitting : fittings ) {
            List<ArgonConnector> acList = weldPipeline( sessionTuple, policyId, fitting );
            argonConnectorList.addAll( acList );
        }
        long ct1 = System.nanoTime();

        /**
         * We now have a list of given argonConnectors for that policyId & fitting,
         * However, not all argonConnectors are interested in this traffic
         * We now iterate through each and remove ones that are not interested
         */
        long ft0 = System.nanoTime();
        List<ArgonAgent> argonAgentList = new ArrayList<ArgonAgent>(argonConnectorList.size());
        String nodeList = "nodes: [";
        for (Iterator<ArgonConnector> i = argonConnectorList.iterator(); i.hasNext();) {
            ArgonConnector argonConnector = i.next();
            PipeSpec pipeSpec = argonConnector.getPipeSpec();

            /**
             * Check that this argon connector actually is interested in this session
             */
            if ( ! pipeSpec.matches(sessionTuple) ) {
                argonConnectorList.remove( argonConnector );
            } else {
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
                         " total time: " + (t1 - t0) +
                         " weld time: " + (ct1 - ct0) +
                         " filter time: " + (ft1 - ft0));
        }

        return argonAgentList;
    }

    /**
     * Remove the given session/pipeline from the current global list
     */
    public void removePipeline( long sessionId )
    {
        PipelineImpl pipeline = pipelines.remove( sessionId );

        if (logger.isDebugEnabled()) {
            logger.debug("removed: " + pipeline + " for: " + sessionId);
        }

        pipeline.destroy();
    }

    /**
     * Create an ArgonConnector.
     * This is here because ArgonConnectorImpl is in Impl and things in API need to create them. Should fix this
     */
    public ArgonConnector createArgonConnector(PipeSpec spec, SessionEventListener listener, Fitting input, Fitting output)
    {
        return new ArgonConnectorImpl( spec,listener, input, output );
    }

    /**
     * Register an Argon Connector
     */
    public synchronized void registerArgonConnector(ArgonConnector argonConnector)
    {
        this.argonConnectors.add( argonConnector);
        Collections.sort( this.argonConnectors, ArgonConnectorComparator.COMPARATOR );
        clearCache();
    }

    /**
     * Unregister an Argon Connector
     */
    public void deregisterArgonConnector(ArgonConnector argonConnector)
    {
        this.argonConnectors.remove( argonConnector );
        clearCache();
    }

    /**
     * Register a Casing
     */
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

    /**
     * Unregister a Casing
     */
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

    /**
     * Get a pipeline by sessionId
     * Returns null if not found
     */
    public Pipeline getPipeline(long sessionId)
    {
        return pipelines.get(sessionId);
    }
    
    /**
     * Remove all of the cached results
     */
    public synchronized void clearCache()
    {
        logger.debug("Clearing Pipeline Foundry cache...");
        pipelineFoundryCache.clear();
    }

    // private methods --------------------------------------------------------

    /**
     * This creates a full pipeline for the given policyId and fitting.
     * It also maintains a cache to memoize results
     */
    private List<ArgonConnector> weldPipeline( SessionTuple sessionTuple, Long policyId, Fitting fitting )
    {
        List<ArgonConnector> argonConnectorList = null;

        /**
         * Check if there is a cache for this policy. First time is without the lock
         */
        Map<Fitting, List<ArgonConnector>> fittingCache = pipelineFoundryCache.get(policyId);

        /**
         * If there is a cache, check if the acList exists for this fitting
         */
        if ( fittingCache != null ) {
            argonConnectorList = fittingCache.get( fitting );
        }

        if ( argonConnectorList == null ) {
            synchronized (this) {
                /* Check if there is a cache again, after grabbing the lock */
                fittingCache = pipelineFoundryCache.get( policyId );

                if ( fittingCache == null ) {
                    /* Cache doesn't exist, create a new empty cache for this policy */
                    fittingCache = new HashMap<Fitting, List<ArgonConnector>>();
                    pipelineFoundryCache.put( policyId, fittingCache );
                } else {
                    /* Cache exists, get the acList for this fitting */
                    argonConnectorList = fittingCache.get( fitting );
                }

                /**
                 * No previously stored result was found in the cache
                 * We now need to calculate the correct result from scratch
                 */
                if ( argonConnectorList == null ) {
                    argonConnectorList = new LinkedList<ArgonConnector>();

                    addArgonConnectors( argonConnectorList, fitting, policyId );

                    removeDuplicates( policyId, argonConnectorList );

                    fittingCache.put( fitting, argonConnectorList );
                }
            }
        }

        return argonConnectorList;
    }

    /**
     * Add all argon connectors to the list that match this policy and fitting type
     */
    private boolean addArgonConnectors( List<ArgonConnector> argonConnectorList, Fitting fitting, Long policyId )
    {
        boolean added = false;

        /**
         * Iterate through all the argonConnections and look for ones that fit the current fitting type
         */
        for ( Iterator<ArgonConnector> i = argonConnectors.iterator(); i.hasNext() ; ) {
            ArgonConnector argonConnector = i.next();

            /**
             * If this argonConnector is the wrong fitting type, skip it
             */
            if ( ! fitting.equals( argonConnector.getInputFitting() ) )
                continue;

            /**
             * If this argonConnector is not on this policy, skip it
             */
            if ( ! policyMatch( argonConnector.getPipeSpec().getNode().getNodeSettings().getPolicyId(), policyId) )
                continue;
            
            argonConnectorList.add( argonConnector );
            added = true;
        }

        /**
         * Now we should add in any casings
         */
        boolean addedCasings = addCasings( argonConnectorList, fitting, policyId );
        if ( addedCasings ) {
            added = true;
        }

        return added;
    }

    /**
     * Add all casings to the list that match this policy and fitting type
     * Also calls addArgonConnectors recursively to add argon connectors
     * for the "inner" fitting type
     */
    private boolean addCasings( List<ArgonConnector> argonConnectorList, Fitting fitting, Long policyId )
    {
        boolean addedCasing = false;

        for (Iterator<ArgonConnector> i = casings.keySet().iterator(); i.hasNext();) {
            ArgonConnector insideArgonConnector = i.next();
            ArgonConnector outsideArgonConnector = casings.get( insideArgonConnector );
            
            Fitting inputFitting = insideArgonConnector.getInputFitting();
                
            /**
             * If this insideArgonConnector is the wrong fitting type, skip it
             */
            if ( ! fitting.equals( insideArgonConnector.getInputFitting() ) )
                continue;

            /**
             * If this insideArgonConnector is not on this policy, skip it
             */
            if ( ! policyMatch( insideArgonConnector.getPipeSpec().getNode().getNodeSettings().getPolicyId(), policyId) ) 
                continue;

            /**
             * Add this casing
             */
            argonConnectorList.add( insideArgonConnector );

            /**
             * add in any argonConnectors that should be inside the casing
             */
            boolean addedSubArgonConnectors = addArgonConnectors( argonConnectorList, insideArgonConnector.getOutputFitting(), policyId );

            /**
             * If no nodes were interested in this casing's traffic, just remove it
             * Otherwise, add the other casing
             */
            if ( ! addedSubArgonConnectors ) {
                argonConnectorList.remove( insideArgonConnector );
            } else {
                argonConnectorList.add( outsideArgonConnector );
                addedCasing = true;
            }
        }

        return addedCasing;
    }

    /**
     * Remove "duplicate" nodes from a given pipeline of argonConnectors
     * For example, if there are two Web Filters in a given list, it will remove the one from the parent rack.
     */
    private void removeDuplicates( Long policyId, List<ArgonConnector> acList )
    {
        Map<String, Integer> numParents = new HashMap<String, Integer>();
        Map<ArgonConnector, Integer> fittingDistance = new HashMap<ArgonConnector, Integer>();

        List<String> enabledNodesInPolicy = new LinkedList<String>();
        List<Node> nodesInPolicy = UvmContextFactory.context().nodeManager().nodeInstances( policyId );
        for (Node node : nodesInPolicy) {
            if (node.getRunState() == NodeSettings.NodeState.RUNNING)
                enabledNodesInPolicy.add(node.getNodeProperties().getName());
        }
        
        for (Iterator<ArgonConnector> i = acList.iterator(); i.hasNext();) {
            ArgonConnector argonConnector = i.next();

            Long nodePolicyId = argonConnector.node().getNodeSettings().getPolicyId();

            if (nodePolicyId == null) {
                continue;
            }

            String nodeName = argonConnector.node().getNodeProperties().getName();

            /**
             * Remove the items that are not enabled in this policy
             * This is to ensure that if an app is in the child and not enabled, it is not inherited from the parent
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
                /**
                 * If we haven't seen another node at any distance, add it to
                 * the hash
                 */
                numParents.put(nodeName, distance);
                continue;
            } else if (distance == n) {
                /* Keep nodes at the same distance */
                continue;
            } else if (distance < n) {
                /**
                 * Current node is closer then the other one, have to remove the
                 * other node done on another iteration
                 */
                numParents.put(nodeName, distance);
            }
        }

        for (Iterator<ArgonConnector> i = acList.iterator(); i.hasNext();) {
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

    /**
     * Returns the difference in # generatinos of two policies.
     * -1 if childId is not a decendent of parentId
     * 0 if childId = parentId
     * 1 if childId is the direct child of parentId
     * 2 if childId is the grandchild of parentId
     * etc
     */
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

    /**
     * This returns true if the nodePolicy would process the session on policyId
     * This is true if nodePolicy == null (its a service app and thus processes all sessions)
     * This is true if policyId == nodePolicy (its a filtering app and lives in the policyId rack)
     * or if one of policyId's parents' policyId == nodePolicy. (its a filtering app and lives one of policyId rack's parents, grandparents, etc)
     */
    private boolean policyMatch( Long nodePolicy, Long policyId )
    {
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

    /**
     * Lookup a list of argon connector for the given fittings & policyId
     */
    private List<ArgonConnector> cacheLookup( Long policyId, Fitting fitting )
    {
        /**
         * Check if there is a cache for this policy. First time is without the lock
         */
        Map<Fitting, List<ArgonConnector>> fittingCache = pipelineFoundryCache.get(policyId);

        if ( fittingCache == null )
            return null;
        
        /**
         * If there is a cache, return the result that exists for this fitting (or null)
         */
        return fittingCache.get( fitting );
    }
    
    /**
     * Utility function to print any list of argonConnectors
     */
    private void printArgonConnectorList( List<ArgonConnector> argonConnectors )
    {
        if (logger.isDebugEnabled()) {
            String strList = "argonConnectors: [";

            if ( argonConnectors == null )
                strList += " null";
            else {
                for (ArgonConnector ac : argonConnectors) {
                    strList += " " + ac;
                }
            }
            
            strList += " ]";
            
            logger.debug( strList );
        }
    }
}
