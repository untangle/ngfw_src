
/**
 * $Id$
 */
package com.untangle.uvm.engine;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Comparator;
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
import com.untangle.uvm.node.SessionTuple;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.NodeSettings;
import com.untangle.uvm.node.PolicyManager;
import com.untangle.uvm.vnet.Affinity;

import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipelineConnector;
import com.untangle.uvm.vnet.PipeSpec;
import com.untangle.uvm.vnet.Pipeline;
import com.untangle.uvm.vnet.PipelineFoundry;
import com.untangle.uvm.vnet.SoloPipeSpec;
import com.untangle.uvm.vnet.CasingPipeSpec;
import com.untangle.uvm.vnet.event.SessionEventListener;

/**
 * Implements PipelineFoundry.
 * PipelineFoundry is responsible for building a list of processing nodes for each session
 *
 * When new sessions are created weld() is called to create the list of <code>PipelineConnectors</code>
 * weld() first finds a list of all PipelineConnectors for the given policyId and fitting type (stream, http, etc)
 * From there it removes the uninterested PipelineConnectors.
 * What is left is a list of all the PipelineConnectors that participate in a given session.
 */
public class PipelineFoundryImpl implements PipelineFoundry
{
    private final Logger logger = Logger.getLogger(getClass());

    /**
     * Global PipelineFoundryImpl instance
     */
    private static final PipelineFoundryImpl PIPELINE_FOUNDRY_IMPL = new PipelineFoundryImpl();

    /**
     * A global list of all current netcap connectors
     */
    private final List<PipelineConnectorImpl> pipelineConnectors = new LinkedList<PipelineConnectorImpl>();

    /**
     * A global list of all current casings
     */
    private final Map<PipelineConnectorImpl, PipelineConnectorImpl> casings = new HashMap<PipelineConnectorImpl, PipelineConnectorImpl>();

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
     * This stores a map from policyId to a cache for that policy storing the list of netcap connectors for various fitting types
     */
    private static final Map<Long, Map<Fitting, List<PipelineConnectorImpl>>> pipelineFoundryCache = new HashMap<Long, Map<Fitting, List<PipelineConnectorImpl>>>();
    
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
     * "weld" is builds a list of all the interested pipelineAgents for a given session
     * It does so based on the given policyId and all the nodes/apps given subscriptions.
     */
    public List<PipelineConnectorImpl> weld( Long sessionId, SessionTuple sessionTuple, Long policyId )
    {
        Long t0 = System.nanoTime();
        List<PipelineConnectorImpl> pipelineConnectorList = new LinkedList<PipelineConnectorImpl>();
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
                if ( ! fittings.contains( Fitting.FTP_CTL_STREAM ) ) fittings.add( Fitting.FTP_CTL_STREAM );
                break;
            case 25:
                if ( ! fittings.contains( Fitting.SMTP_STREAM ) ) fittings.add( Fitting.SMTP_STREAM );
                break;
            case 80:
                if ( ! fittings.contains( Fitting.HTTP_STREAM ) )  fittings.add( Fitting.HTTP_STREAM );
                break;
            case 443:
                if ( ! fittings.contains( Fitting.HTTPS_STREAM ) ) fittings.add( Fitting.HTTPS_STREAM );
                break;
            default:
                break;
            }
        }

        /**
         * All sessions are OCTET stream, add it if it isn't already there
         */
        if ( ! fittings.contains( Fitting.OCTET_STREAM ) ) fittings.add( Fitting.OCTET_STREAM );

        long ct0 = System.nanoTime();
        for ( Fitting fitting : fittings ) {
            List<PipelineConnectorImpl> acList = weldPipeline( sessionTuple, policyId, fitting );
            pipelineConnectorList.addAll( acList );
        }
        long ct1 = System.nanoTime();

        /**
         * We now have a list of given pipelineConnectors for that policyId & fitting,
         * However, not all pipelineConnectors are interested in this traffic
         * We now iterate through each and remove ones that are not interested
         */
        long ft0 = System.nanoTime();
        String nodeList = "nodes: [";
        for (Iterator<PipelineConnectorImpl> i = pipelineConnectorList.iterator(); i.hasNext();) {
            PipelineConnectorImpl pipelineConnector = i.next();
            PipeSpec pipeSpec = pipelineConnector.getPipeSpec();

            /**
             * Check that this netcap connector actually is interested in this session
             */
            if ( ! pipeSpec.matches(sessionTuple) ) {
                // remove from pipelineConnectorList
                i.remove(); 
            } else {
                // keep in pipelineConnectorList
                nodeList += pipeSpec.getName() + " ";
            }
        }
        nodeList += "]";
        long ft1 = System.nanoTime();

        PipelineImpl pipeline = new PipelineImpl(sessionId, pipelineConnectorList);
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

        return pipelineConnectorList;
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
    }

    /**
     * Create an PipelineConnectorImpl.
     * This is here because PipelineConnectorImpl is in Impl and things in API need to create them. Should fix this
     */
    public PipelineConnectorImpl createPipelineConnector(PipeSpec spec, SessionEventListener listener, Fitting input, Fitting output)
    {
        return new PipelineConnectorImpl( spec, listener, input, output );
    }

    /**
     * Register an Netcap Connector
     */
    public synchronized void registerPipelineConnector(PipelineConnector pipelineConnector)
    {
        this.pipelineConnectors.add( ((PipelineConnectorImpl) pipelineConnector) );
        Collections.sort( this.pipelineConnectors, PipelineConnectorComparator.COMPARATOR );
        clearCache();
    }

    /**
     * Unregister an Netcap Connector
     */
    public void deregisterPipelineConnector(PipelineConnector pipelineConnector)
    {
        this.pipelineConnectors.remove( (PipelineConnectorImpl) pipelineConnector );
        clearCache();
    }
    
    /**
     * Register a Casing
     */
    public void registerCasing( PipelineConnector insidePipelineConnector, PipelineConnector outsidePipelineConnector )
    {
        if (insidePipelineConnector.getPipeSpec() != outsidePipelineConnector.getPipeSpec()) {
            throw new IllegalArgumentException("casing constraint violated");
        }

        synchronized (this) {
            casings.put( ((PipelineConnectorImpl) insidePipelineConnector) , ((PipelineConnectorImpl) outsidePipelineConnector) );
            clearCache();
        }
    }

    /**
     * Unregister a Casing
     */
    public void deregisterCasing( PipelineConnector insidePipelineConnector )
    {
        synchronized (this) {
            casings.remove( ((PipelineConnectorImpl)insidePipelineConnector) );
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

    /**
     * This creates a full pipeline for the given policyId and fitting.
     * It also maintains a cache to memoize results
     */
    private List<PipelineConnectorImpl> weldPipeline( SessionTuple sessionTuple, Long policyId, Fitting fitting )
    {
        List<PipelineConnectorImpl> pipelineConnectorList = null;

        /**
         * Check if there is a cache for this policy. First time is without the lock
         */
        Map<Fitting, List<PipelineConnectorImpl>> fittingCache = pipelineFoundryCache.get(policyId);

        /**
         * If there is a cache, check if the acList exists for this fitting
         */
        if ( fittingCache != null ) {
            pipelineConnectorList = fittingCache.get( fitting );
        }

        if ( pipelineConnectorList == null ) {
            synchronized (this) {
                /* Check if there is a cache again, after grabbing the lock */
                fittingCache = pipelineFoundryCache.get( policyId );

                if ( fittingCache == null ) {
                    /* Cache doesn't exist, create a new empty cache for this policy */
                    fittingCache = new HashMap<Fitting, List<PipelineConnectorImpl>>();
                    pipelineFoundryCache.put( policyId, fittingCache );
                } else {
                    /* Cache exists, get the acList for this fitting */
                    pipelineConnectorList = fittingCache.get( fitting );
                }

                /**
                 * No previously stored result was found in the cache
                 * We now need to calculate the correct result from scratch
                 */
                if ( pipelineConnectorList == null ) {

                    pipelineConnectorList = new LinkedList<PipelineConnectorImpl>();

                    List<PipelineConnectorImpl> availablePipelineConnectorsNodes = new LinkedList<PipelineConnectorImpl>( this.pipelineConnectors );
                    List<PipelineConnectorImpl> availablePipelineConnectorsCasings = new LinkedList<PipelineConnectorImpl>( this.casings.keySet() );

                    removeDuplicates( policyId, availablePipelineConnectorsNodes );
                    printPipelineConnectorList( "nodes: ", availablePipelineConnectorsNodes );

                    removeDuplicates( policyId, availablePipelineConnectorsCasings );
                    printPipelineConnectorList( "casings: ", availablePipelineConnectorsCasings );
                    
                    addPipelineConnectors( pipelineConnectorList,
                                           availablePipelineConnectorsNodes,
                                           availablePipelineConnectorsCasings,
                                           fitting, policyId );

                    fittingCache.put( fitting, pipelineConnectorList );
                }
            }
        }

        return pipelineConnectorList;
    }

    /**
     * Add all netcap connectors to the list that match this policy and fitting type
     */
    private boolean addPipelineConnectors( List<PipelineConnectorImpl> pipelineConnectorList,
                                           List<PipelineConnectorImpl> availableConnectors,
                                           List<PipelineConnectorImpl> availableCasings, Fitting fitting, Long policyId )
    {
        boolean added = false;

        /**
         * Iterate through all the netcapConnections and look for ones that fit the current fitting type
         */
        for ( Iterator<PipelineConnectorImpl> i = availableConnectors.iterator(); i.hasNext() ; ) {
            PipelineConnectorImpl pipelineConnector = i.next();

            /**
             * If this pipelineConnector is the wrong fitting type, skip it
             */
            if ( ! fitting.equals( pipelineConnector.getInputFitting() ) )
                continue;

            /**
             * If this pipelineConnector is not on this policy, skip it
             */
            if ( ! policyMatch( pipelineConnector.getPipeSpec().getNode().getNodeSettings().getPolicyId(), policyId) )
                continue;
            
            pipelineConnectorList.add( pipelineConnector );
            added = true;
        }

        /**
         * Now we should add in any casings
         */
        boolean addedCasings = addCasings( pipelineConnectorList, availableConnectors, availableCasings, fitting, policyId );
        if ( addedCasings ) {
            added = true;
        }

        return added;
    }

    /**
     * Add all casings to the list that match this policy and fitting type
     * Also calls addPipelineConnectors recursively to add netcap connectors
     * for the "inner" fitting type
     */
    private boolean addCasings( List<PipelineConnectorImpl> pipelineConnectorList,
                                List<PipelineConnectorImpl> availableConnectors,
                                List<PipelineConnectorImpl> availableCasings, Fitting fitting, Long policyId )
    {
        boolean addedCasing = false;

        for (Iterator<PipelineConnectorImpl> i = availableCasings.iterator(); i.hasNext();) {
            PipelineConnectorImpl insidePipelineConnector = i.next();
            PipelineConnectorImpl outsidePipelineConnector = casings.get( insidePipelineConnector );
            
            /**
             * If this insidePipelineConnector is the wrong fitting type, skip it
             */
            if ( ! fitting.equals( insidePipelineConnector.getInputFitting() ) )
                continue;

            /**
             * If this insidePipelineConnector is not on this policy, skip it
             */
            if ( ! policyMatch( insidePipelineConnector.getPipeSpec().getNode().getNodeSettings().getPolicyId(), policyId) ) 
                continue;

            /**
             * Add this casing
             */
            pipelineConnectorList.add( insidePipelineConnector );

            /**
             * add in any pipelineConnectors that should be inside the casing
             */
            boolean addedSubPipelineConnectors = addPipelineConnectors( pipelineConnectorList, availableConnectors, availableCasings, insidePipelineConnector.getOutputFitting(), policyId );

            /**
             * If no nodes were interested in this casing's traffic, just remove it
             * Otherwise, add the other casing
             */
            if ( ! addedSubPipelineConnectors ) {
                pipelineConnectorList.remove( insidePipelineConnector );
            } else {
                pipelineConnectorList.add( outsidePipelineConnector );
                addedCasing = true;
            }
        }

        return addedCasing;
    }

    /**
     * Remove "duplicate" nodes from a given pipeline of pipelineConnectors
     * For example, if there are two Web Filters in a given list, it will remove the one from the parent rack.
     */
    private void removeDuplicates( Long policyId, List<PipelineConnectorImpl> acList )
    {
        Map<String, Integer> numParents = new HashMap<String, Integer>();
        Map<PipelineConnectorImpl, Integer> fittingDistance = new HashMap<PipelineConnectorImpl, Integer>();

        List<String> enabledNodesInPolicy = new LinkedList<String>();
        List<Node> nodesInPolicy = UvmContextFactory.context().nodeManager().nodeInstances( policyId );
        for (Node node : nodesInPolicy) {
            if (node.getRunState() == NodeSettings.NodeState.RUNNING)
                enabledNodesInPolicy.add(node.getNodeProperties().getName());
        }
        
        for (Iterator<PipelineConnectorImpl> i = acList.iterator(); i.hasNext();) {
            PipelineConnectorImpl pipelineConnector = i.next();

            Long nodePolicyId = pipelineConnector.node().getNodeSettings().getPolicyId();

            if (nodePolicyId == null) {
                continue;
            }

            String nodeName = pipelineConnector.node().getNodeProperties().getName();

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

            fittingDistance.put(pipelineConnector, distance);

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

        for (Iterator<PipelineConnectorImpl> i = acList.iterator(); i.hasNext();) {
            PipelineConnectorImpl pipelineConnector = i.next();

            Long nodePolicyId = pipelineConnector.node().getNodeSettings().getPolicyId();

            /* Keep items in the NULL Racks */
            if (nodePolicyId == null) {
                continue;
            }

            String nodeName = pipelineConnector.node().getNodeProperties().getName();

            Integer n = numParents.get(nodeName);

            if (n == null) {
                logger.warn("numParents null for non-null policy.");
                continue;
            }

            Integer distance = fittingDistance.get( pipelineConnector );

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
     * Utility function to print any list of pipelineConnectors
     */
    private void printPipelineConnectorList( String prefix, java.util.Collection<PipelineConnectorImpl> pipelineConnectors )
    {
        if (logger.isDebugEnabled()) {
            String strList = prefix + "pipelineConnectors: [";

            if ( pipelineConnectors == null )
                strList += " null";
            else {
                for (PipelineConnectorImpl ac : pipelineConnectors) {
                    strList += " " + ac;
                }
            }
            
            strList += " ]";
            
            logger.debug( strList );
        }
    }

    private static class PipelineConnectorComparator implements Comparator<PipelineConnector>
    {
        static final PipelineConnectorComparator COMPARATOR = new PipelineConnectorComparator();

        private PipelineConnectorComparator() { }

        public int compare(PipelineConnector mp1, PipelineConnector mp2)
        {
            SoloPipeSpec ps1 = null == mp1 ? null : (SoloPipeSpec)mp1.getPipeSpec();
            SoloPipeSpec ps2 = null == mp2 ? null : (SoloPipeSpec)mp2.getPipeSpec();

            Affinity ra1 = null == ps1 ? null : ps1.getAffinity();
            Affinity ra2 = null == ps2 ? null : ps2.getAffinity();

            if (null == ra1) {
                if (null == ra2) {
                    return 0;
                } else {
                    if (Affinity.CLIENT == ra2) {
                        return 1;
                    } else if (Affinity.SERVER == ra2) {
                        return -1;
                    } else {
                        throw new RuntimeException("programmer malfunction");
                    }
                }
            } else if (null == ra2) {
                if (Affinity.CLIENT == ra1) {
                    return -1;
                } else if (Affinity.SERVER == ra2) {
                    return 1;
                } else {
                    throw new RuntimeException("programmer malfunction");
                }
            } else if (ra1 == ra2) {
                int s1 = ps1.getStrength();
                int s2 = ps2.getStrength();

                if (s1 == s2) {
                    if (mp1 == mp2) {
                        return 0;
                    } else {
                        int mp1Id = System.identityHashCode(mp1);
                        int mp2Id = System.identityHashCode(mp2);
                        return mp1Id < mp2Id ? -1 : 1;
                    }
                } else if (ra1 == Affinity.CLIENT) {
                    return s1 < s2 ? 1 : -1;
                } else if (ra1 == Affinity.SERVER) {
                    return s1 < s2 ? -1 : 1;
                } else {
                    throw new RuntimeException("programmer malfunction");
                }
            } else if (Affinity.CLIENT == ra1) {
                return -1;
            } else if (Affinity.SERVER == ra1) {
                return 1;
            } else {
                throw new RuntimeException("programmer malfunction");
            }
        }
    }
}
