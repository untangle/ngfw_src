/**
 * $Id$
 */
package com.untangle.uvm;

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
import com.untangle.uvm.node.AppSettings;
import com.untangle.uvm.node.PolicyManager;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Subscription;

import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipelineConnector;
import com.untangle.uvm.vnet.PipelineFoundry;
import com.untangle.uvm.vnet.SessionEventHandler;


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
     * This stores a list of "hints" about connections and what fitting types they are
     * If an app knows what kind of connection/fitting should be used for a connection from the given address/port
     * It can register a hint so the pipeline foundry will treat the session accordingly
     */
    private final Map<InetSocketAddress, Fitting> fittingHints = new ConcurrentHashMap<InetSocketAddress, Fitting>();

    /**
     * This stores a map from policyId to a cache for that policy storing the list of netcap connectors for various fitting types
     */
    private static final Map<Integer, Map<Fitting, List<PipelineConnectorImpl>>> pipelineFoundryCache = new HashMap<Integer, Map<Fitting, List<PipelineConnectorImpl>>>();
    private static final Map<Integer, Map<Fitting, List<PipelineConnectorImpl>>> pipelineNonPremiumFoundryCache = new HashMap<Integer, Map<Fitting, List<PipelineConnectorImpl>>>();
    
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
    public List<PipelineConnectorImpl> weld( Long sessionId, SessionTuple sessionTuple, Integer policyId, boolean includePremium )
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
                fittings.add( 0, hint );
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
            List<PipelineConnectorImpl> acList = weldPipeline( sessionTuple, policyId, fitting, includePremium );
            pipelineConnectorList.addAll( acList );
        }
        long ct1 = System.nanoTime();

        /**
         * We now have a list of given pipelineConnectors for that policyId & fitting,
         * However, not all pipelineConnectors are interested in this traffic
         * We now iterate through each and remove ones that are not interested
         */
        long ft0 = System.nanoTime();
        String nodeList = "nodes: [ ";
        for (Iterator<PipelineConnectorImpl> i = pipelineConnectorList.iterator(); i.hasNext();) {
            PipelineConnectorImpl pipelineConnector = i.next();

            /**
             * Check that this netcap connector actually is interested in this session
             */
            if ( ! pipelineConnector.matches(sessionTuple) ) {
                // remove from pipelineConnectorList
                i.remove(); 
            } else {
                // keep in pipelineConnectorList
                nodeList += pipelineConnector.getName() + " ";
            }
        }
        nodeList += "]";
        long ft1 = System.nanoTime();

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

    public PipelineConnector create( String name, Node node, Subscription subscription, SessionEventHandler listener, Fitting inputFitting, Fitting outputFitting, Affinity affinity, Integer affinityStrength, boolean premium )
    {
        return new PipelineConnectorImpl( name, node, subscription, listener, inputFitting, outputFitting, affinity, affinityStrength, premium, null );
    }

    public PipelineConnector create( String name, Node node, Subscription subscription, SessionEventHandler listener, Fitting inputFitting, Fitting outputFitting, Affinity affinity, Integer affinityStrength, boolean premium, String buddy )
    {
        return new PipelineConnectorImpl( name, node, subscription, listener, inputFitting, outputFitting, affinity, affinityStrength, premium, buddy );
    }
    
    /**
     * Register an PipelineConnector
     */
    public synchronized void registerPipelineConnector(PipelineConnector pipelineConnector)
    {
        logger.debug( "registerPipelineConnector( " + pipelineConnector.getName() + " )" );
        synchronized (this) {
            this.pipelineConnectors.add( ((PipelineConnectorImpl) pipelineConnector) );
            Collections.sort( this.pipelineConnectors, PipelineConnectorComparator.COMPARATOR );
            clearCache();
        }
    }

    /**
     * Unregister an PipelineConnector
     */
    public void deregisterPipelineConnector(PipelineConnector pipelineConnector)
    {
        logger.debug( "deregisterPipelineConnector( " + pipelineConnector.getName() + " )" );
        synchronized (this) {
            this.pipelineConnectors.remove( (PipelineConnectorImpl) pipelineConnector );
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
     * Remove all of the cached results
     */
    public synchronized void clearCache()
    {
        logger.debug("Clearing Pipeline Foundry cache...");
        pipelineFoundryCache.clear();
        pipelineNonPremiumFoundryCache.clear();
    }

    /**
     * This creates a full pipeline for the given policyId and fitting.
     * It also maintains a cache to memoize results
     */
    private List<PipelineConnectorImpl> weldPipeline( SessionTuple sessionTuple, Integer policyId, Fitting fitting, boolean includePremium )
    {
        List<PipelineConnectorImpl> pipelineConnectorList = null;
        Map<Integer, Map<Fitting, List<PipelineConnectorImpl>>> cache;

        if (includePremium)
            cache = pipelineFoundryCache;
        else
            cache = pipelineNonPremiumFoundryCache;
        /**
         * Check if there is a cache for this policy. First time is without the lock
         */
        Map<Fitting, List<PipelineConnectorImpl>> fittingCache = cache.get(policyId);

        /**
         * If there is a cache, check if the acList exists for this fitting
         */
        if ( fittingCache != null ) {
            pipelineConnectorList = fittingCache.get( fitting );
        }

        if ( pipelineConnectorList == null ) {
            synchronized (this) {
                /* Check if there is a cache again, after grabbing the lock */
                fittingCache = cache.get( policyId );

                if ( fittingCache == null ) {
                    /* Cache doesn't exist, create a new empty cache for this policy */
                    fittingCache = new HashMap<Fitting, List<PipelineConnectorImpl>>();
                    cache.put( policyId, fittingCache );
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

                    removeUnnecessaryPipelineConnectors( policyId, availablePipelineConnectorsNodes, includePremium );
                    printPipelineConnectorList( "available connectors: ", availablePipelineConnectorsNodes );

                    addPipelineConnectors( pipelineConnectorList,
                                           availablePipelineConnectorsNodes,
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
    private void addPipelineConnectors( List<PipelineConnectorImpl> pipelineConnectorList,
                                        List<PipelineConnectorImpl> availableConnectors,
                                        Fitting fitting, Integer policyId )
    {
        PipelineConnectorImpl connectorToAdd = null;
        
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
            if ( ! policyMatch( pipelineConnector.getNode().getAppSettings().getPolicyId(), policyId) )
                continue;
            

            /**
             * Add the current pipelineConnector to the chain
             */
            connectorToAdd = pipelineConnector;
            break;
        }

        if ( connectorToAdd == null ) {
            // nothing to add
            return;
        }
        
        // before adding first check that the previous pipeline connector is a buddy
        // if it is, remove the buddy and don't add this one
        if ( pipelineConnectorList.size() > 0 ) {
            PipelineConnectorImpl prevConnector = pipelineConnectorList.get( pipelineConnectorList.size() - 1 );
            if ( ( connectorToAdd.getBuddy() != null && connectorToAdd.getBuddy().equals( prevConnector.getName() ) ) ) {
                pipelineConnectorList.remove( pipelineConnectorList.size() - 1 );
                logger.debug("Dropping both " + prevConnector.getName() + " and " + connectorToAdd.getName() + " from pipeline because nothing is in between.");
                // now continue where we left off
                addPipelineConnectors( pipelineConnectorList, availableConnectors, prevConnector.getInputFitting(), policyId );
                return;
            }

        }

        //also check that if this pipeline connector has a buddy, that the buddy is already in the pipeline somewhere
        if ( connectorToAdd.getBuddy() != null ) {
            String buddy = connectorToAdd.getBuddy();
            boolean found = false;
            for ( PipelineConnectorImpl pc : pipelineConnectorList ) {
                if ( buddy.equals( pc.getName() ) ) found = true;
            }
            
            if ( !found ) {
                logger.debug("Skipping " + connectorToAdd + " to current chain. (" + buddy + " not found )");
                // the buddy was not found, so we should not install this pipeline connector
                // remove this pipeline connector from the available list and continue
                availableConnectors.remove( connectorToAdd ); // remove from available list
                addPipelineConnectors( pipelineConnectorList, availableConnectors, fitting, policyId );
                return;
            }
        }

        pipelineConnectorList.add( connectorToAdd ); // add to current chain
        availableConnectors.remove( connectorToAdd ); // remove from available list

        logger.debug("Adding " + connectorToAdd + " to current chain.");
        printPipelineConnectorList( "current chain : ", pipelineConnectorList );
        printPipelineConnectorList( "available     : ", availableConnectors );

        Fitting outputFitting = connectorToAdd.getOutputFitting(); // this connections output fitting
        addPipelineConnectors( pipelineConnectorList, availableConnectors, outputFitting, policyId );
        return;
    }

    /**
     * Remove "duplicate" nodes from a given pipeline of pipelineConnectors
     * For example, if there are two Web Filters in a given list, it will remove the one from the parent rack.
     */
    private void removeUnnecessaryPipelineConnectors( Integer policyId, List<PipelineConnectorImpl> acList, boolean includePremium )
    {
        Map<String, Integer> numParents = new HashMap<String, Integer>();
        Map<PipelineConnectorImpl, Integer> fittingDistance = new HashMap<PipelineConnectorImpl, Integer>();

        List<String> enabledNodesInPolicy = new LinkedList<String>();
        List<Node> nodesInPolicy = UvmContextFactory.context().appManager().appInstances( policyId );
        for (Node node : nodesInPolicy) {
            if (node.getRunState() == AppSettings.AppState.RUNNING)
                enabledNodesInPolicy.add(node.getAppProperties().getName());
        }

        /**
         * Remove premium pipelineConnectors if includePremium is false
         */
        if (!includePremium) {
            for (Iterator<PipelineConnectorImpl> i = acList.iterator(); i.hasNext();) {
                PipelineConnectorImpl pipelineConnector = i.next();
                if (pipelineConnector.isPremium())
                    i.remove();
            }
        }

        /**
         * Remove inherited apps if a app in the child overrides it
         */
        for (Iterator<PipelineConnectorImpl> i = acList.iterator(); i.hasNext();) {
            PipelineConnectorImpl pipelineConnector = i.next();

            Integer nodePolicyId = pipelineConnector.node().getAppSettings().getPolicyId();

            if (nodePolicyId == null) {
                continue;
            }

            String nodeName = pipelineConnector.node().getAppProperties().getName();

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

            Integer nodePolicyId = pipelineConnector.node().getAppSettings().getPolicyId();

            /* Keep items in the NULL Racks */
            if (nodePolicyId == null) {
                continue;
            }

            String nodeName = pipelineConnector.node().getAppProperties().getName();

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
    public int getPolicyGenerationDiff(Integer childId, Integer parentId)
    {
        PolicyManager policyManager = (PolicyManager) UvmContextFactory.context().appManager().app("policy-manager");

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
    private boolean policyMatch( Integer nodePolicy, Integer policyId )
    {
        PolicyManager policyManager = (PolicyManager) UvmContextFactory.context().appManager().app("policy-manager");

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
        for ( Integer parentId = policyManager.getParentPolicyId( policyId ) ; parentId != null ; parentId = policyManager.getParentPolicyId( parentId ) ) {
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

    private static class PipelineConnectorComparator implements Comparator<PipelineConnectorImpl>
    {
        static final PipelineConnectorComparator COMPARATOR = new PipelineConnectorComparator();

        private PipelineConnectorComparator() { }

        public int compareStrength( int strength1, int strength2 )
        {
            if ( strength1 == strength2 )
                return 0;
            if ( strength1 < strength2 )
                return -1;
            else
                return 1;
        }
        
        public int compare(PipelineConnectorImpl connector1, PipelineConnectorImpl connector2)
        {
            Affinity affinity1 = connector1.getAffinity();
            Affinity affinity2 = connector2.getAffinity();

            if ( affinity1 == null )
                affinity1 = Affinity.MIDDLE;
            if ( affinity2 == null )
                affinity2 = Affinity.MIDDLE;
            
            if ( affinity1 == affinity2 ) {
                Integer strength1 = connector1.getAffinityStrength();
                if ( strength1 == null ) strength1 = 0;
                Integer strength2 = connector2.getAffinityStrength();
                if ( strength2 == null ) strength2 = 0;
                return compareStrength( strength1, strength2 );
            } else {
                int numValue1 = affinity1.numValue();
                int numValue2 = affinity2.numValue();
                return compareStrength( numValue1, numValue2 );
            }
        }
    }
}
