/**
 * $Id$
 */
package com.untangle.uvm;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.app.SessionTuple;
import com.untangle.uvm.app.App;
import com.untangle.uvm.app.AppSettings;
import com.untangle.uvm.app.PolicyManager;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Subscription;

import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipelineConnector;
import com.untangle.uvm.vnet.PipelineFoundry;
import com.untangle.uvm.vnet.SessionEventHandler;


/**
 * Implements PipelineFoundry.
 * PipelineFoundry is responsible for building a list of processing
 * apps for each session
 *
 * When new sessions are created weld() is called to create the list
 * of <code>PipelineConnectors</code> weld() first finds a list of all
 * PipelineConnectors for the given policyId and fitting type (stream,
 * http, etc) From there it removes the uninterested
 * PipelineConnectors. What is left is a list of all the
 * PipelineConnectors that participate in a given session.
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
    private final List<PipelineConnectorImpl> pipelineConnectors = new LinkedList<>();

    /**
     * This stores a list of "hints" about connections and what fitting types they are
     * If an app knows what kind of connection/fitting should be used for a connection from the given address/port
     * It can register a hint so the pipeline foundry will treat the session accordingly
     */
    private final Map<InetSocketAddress, Fitting> fittingHints = new ConcurrentHashMap<>();

    /**
     * This stores a map from policyId to a cache for that policy storing the list of netcap connectors for various fitting types
     */
    private static final Map<Integer, Map<Fitting, List<PipelineConnectorImpl>>> pipelineFoundryCache = new HashMap<>();
    private static final Map<Integer, Map<Fitting, List<PipelineConnectorImpl>>> pipelineNonPremiumFoundryCache = new HashMap<>();
    
    /**
     * Private constructor to ensure singleton
     */
    private PipelineFoundryImpl() {}

    /**
     * Return the singleton instance of the PipelineFoundry
     * @return singleton
     */
    public static PipelineFoundryImpl foundry()
    {
        return PIPELINE_FOUNDRY_IMPL;
    }

    /**
     * "weld" is builds a list of all the interested pipelineAgents for a given session
     * It does so based on the given policyId and all the apps/apps given subscriptions.
     * @param sessionId
     * @param sessionTuple
     * @param policyId
     * @param includePremium
     * @return list
     */
    public List<PipelineConnectorImpl> weld( Long sessionId, SessionTuple sessionTuple, Integer policyId, boolean includePremium )
    {
        Long t0 = System.nanoTime();
        List<PipelineConnectorImpl> pipelineConnectorList = new LinkedList<>();
        List<Fitting> fittings = new LinkedList<>();

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
        String appList = "apps: [ ";
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
                appList += pipelineConnector.getName() + " ";
            }
        }
        appList += "]";
        long ft1 = System.nanoTime();

        Long t1 = System.nanoTime();
        if (logger.isDebugEnabled()) {
            logger.debug("session_id: " + sessionId +
                         " policyId: " + policyId + " " +
                         appList );
            logger.debug("session_id: " + sessionId +
                         " total time: " + (t1 - t0) +
                         " weld time: " + (ct1 - ct0) +
                         " filter time: " + (ft1 - ft0));
        }

        return pipelineConnectorList;
    }

    /**
     * Create a PipelineConnector
     * @param name
     * @param app
     * @param subscription
     * @param listener
     * @param inputFitting
     * @param outputFitting
     * @param affinity
     * @param affinityStrength
     * @param premium
     * @return PipelineConnector
     */
    public PipelineConnector create( String name, App app, Subscription subscription, SessionEventHandler listener, Fitting inputFitting, Fitting outputFitting, Affinity affinity, Integer affinityStrength, boolean premium )
    {
        return new PipelineConnectorImpl( name, app, subscription, listener, inputFitting, outputFitting, affinity, affinityStrength, premium, null );
    }

    /**
     * Create a PipelineConnector
     * @param name
     * @param app
     * @param subscription
     * @param listener
     * @param inputFitting
     * @param outputFitting
     * @param affinity
     * @param affinityStrength
     * @param premium
     * @param buddy
     * @return PipelineConnector
     */
    public PipelineConnector create( String name, App app, Subscription subscription, SessionEventHandler listener, Fitting inputFitting, Fitting outputFitting, Affinity affinity, Integer affinityStrength, boolean premium, String buddy )
    {
        return new PipelineConnectorImpl( name, app, subscription, listener, inputFitting, outputFitting, affinity, affinityStrength, premium, buddy );
    }
    
    /**
     * Register an PipelineConnector
     * @param pipelineConnector
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
     * @param pipelineConnector
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
     * @param socketAddress
     * @param fitting
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
     * Return JSONArray of application pipelines for this policyId.
     * @param policyId Policy id to compare
     * @param protocol short of IP protocol (6=TCP, 17=UDP)
     * @param clientIp String of client IP address.
     * @param serverIp String of server IP address.
     * @param clientPort int of client port.
     * @param serverPort int of server port
     * @return String of application ids and their numeric order
     */
    public JSONArray getPipelineOrder(Integer policyId, short protocol, String clientIp, String serverIp, int clientPort, int serverPort)
    {
        JSONArray result = null;
        int index = 0;
        List<PipelineConnectorImpl> pipelineConnectors = null;
        try{
            SessionTuple sessionTuple = new SessionTuple(protocol, InetAddress.getByName(clientIp), InetAddress.getByName(serverIp), clientPort, serverPort);
            pipelineConnectors = weld( 0L, sessionTuple, policyId, true );
            result = new JSONArray();
            for(PipelineConnectorImpl pci : pipelineConnectors){
                JSONObject jo = new JSONObject(pci);
                jo.remove("class");
                result.put(index++, jo);
            }
        }catch( Exception e){
            logger.warn("getPipelineOrder: Unable to calculate:", e);
        }
        return result;
    }

    /**
     * This creates a full pipeline for the given policyId and fitting.
     * It also maintains a cache to memoize results
     * @param sessionTuple
     * @param policyId
     * @param fitting
     * @param includePremium
     * @return list
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
                    fittingCache = new HashMap<>();
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

                    pipelineConnectorList = new LinkedList<>();

                    List<PipelineConnectorImpl> availablePipelineConnectorsApps = new LinkedList<>( this.pipelineConnectors );

                    removeUnnecessaryPipelineConnectors( policyId, availablePipelineConnectorsApps, includePremium );
                    printPipelineConnectorList( "available connectors: ", availablePipelineConnectorsApps );

                    addPipelineConnectors( pipelineConnectorList,
                                           availablePipelineConnectorsApps,
                                           fitting, policyId );

                    fittingCache.put( fitting, pipelineConnectorList );
                }
            }
        }

        return pipelineConnectorList;
    }

    /**
     * Add all netcap connectors to the list that match this policy and fitting type
     * @param pipelineConnectorList
     * @param availableConnectors
     * @param fitting
     * @param policyId
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
            if ( ! policyMatch( pipelineConnector.getApp().getAppSettings().getPolicyId(), policyId) )
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
     * Remove "duplicate" apps from a given pipeline of pipelineConnectors
     * For example, if there are two Web Filters in a given list, it will remove the one from the parent rack.
     * @param policyId
     * @param acList
     * @param includePremium
     */
    private void removeUnnecessaryPipelineConnectors( Integer policyId, List<PipelineConnectorImpl> acList, boolean includePremium )
    {
        Map<String, Integer> numParents = new HashMap<>();
        Map<PipelineConnectorImpl, Integer> fittingDistance = new HashMap<>();

        List<String> enabledAppsInPolicy = new LinkedList<>();
        List<App> appsInPolicy = UvmContextFactory.context().appManager().appInstances( policyId );
        for (App app : appsInPolicy) {
            if (app.getRunState() == AppSettings.AppState.RUNNING)
                enabledAppsInPolicy.add(app.getAppProperties().getName());
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

            Integer appPolicyId = pipelineConnector.app().getAppSettings().getPolicyId();

            if (appPolicyId == null) {
                continue;
            }

            String appName = pipelineConnector.app().getAppProperties().getName();

            /**
             * Remove the items that are not enabled in this policy
             * This is to ensure that if an app is in the child and not enabled, it is not inherited from the parent
             */
            if (!enabledAppsInPolicy.contains(appName)) {
                i.remove();
                continue;
            }


            Integer n = numParents.get(appName);
            int distance = getPolicyGenerationDiff(policyId, appPolicyId);

            if (distance < 0) {
                /* Removing apps that are not in this policy */
                logger.debug("The policy " + policyId + " is not a child of " + appPolicyId);
                i.remove();
                continue;
            }

            fittingDistance.put(pipelineConnector, distance);

            /* If an existing app is closer then this app, remove this app. */
            if (n == null) {
                /**
                 * If we haven't seen another app at any distance, add it to
                 * the hash
                 */
                numParents.put(appName, distance);
                continue;
            } else if (distance == n) {
                /* Keep apps at the same distance */
                continue;
            } else if (distance < n) {
                /**
                 * Current app is closer then the other one, have to remove the
                 * other app done on another iteration
                 */
                numParents.put(appName, distance);
            }
        }

        for (Iterator<PipelineConnectorImpl> i = acList.iterator(); i.hasNext();) {
            PipelineConnectorImpl pipelineConnector = i.next();

            Integer appPolicyId = pipelineConnector.app().getAppSettings().getPolicyId();

            /* Keep items in the NULL Racks */
            if (appPolicyId == null) {
                continue;
            }

            String appName = pipelineConnector.app().getAppProperties().getName();

            Integer n = numParents.get(appName);

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
     * @param childId
     * @param parentId
     * @return diff
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
     * This returns true if the appPolicy would process the session on policyId
     * This is true if appPolicy == null (its a service app and thus processes all sessions)
     * This is true if policyId == appPolicy (its a filtering app and lives in the policyId rack)
     * or if one of policyId's parents' policyId == appPolicy. (its a filtering app and lives one of policyId rack's parents, grandparents, etc)
     * @param appPolicy
     * @param policyId
     * @return true if match, false otherwise
     */
    private boolean policyMatch( Integer appPolicy, Integer policyId )
    {
        PolicyManager policyManager = (PolicyManager) UvmContextFactory.context().appManager().app("policy-manager");

        /**
         * If appPolicy is null its a service so it matches all policies
         */
        if ( appPolicy == null )
            return true;

        /**
         * policyId == null means "No Rack"
         * so no apps match this policy (except services which are handled above)
         */
        if ( policyId == null ) {
            return false;
        }
        
        /**
         * Otherwise test for equality
         */
        if ( policyId.equals(appPolicy) )
            return true;

        /**
         * Now check the parents if policyManager exists otherwise return false
         */
        if ( policyManager == null )
            return false;

        /**
         * Recursively check the parent rack of the appPolicy
         */
        for ( Integer parentId = policyManager.getParentPolicyId( policyId ) ; parentId != null ; parentId = policyManager.getParentPolicyId( parentId ) ) {
            /**
             * does this app live in the parent of the session's policy?
             * if so then this app should process this session
             * dupes will be removed later...
             */
            if ( parentId.equals( appPolicy ) )
                return true;
        }

        return false;
    }

    /**
     * Utility function to print any list of pipelineConnectors
     * @param prefix
     * @param pipelineConnectors
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

    /**
     * PipelineConnectorComparator sorts the PipelienConnectors into the correct order
     * to process a session
     */
    private static class PipelineConnectorComparator implements Comparator<PipelineConnectorImpl>
    {
        static final PipelineConnectorComparator COMPARATOR = new PipelineConnectorComparator();

        /**
         * Private construcotor - use singleton
         */
        private PipelineConnectorComparator() { }

        /**
         * compareStrength - used to sort by strength
         * @param strength1
         * @param strength2
         * @return -1 if strength1 < strength2, 0 if equal, 1 if strength1 > strength2
         */
        public int compareStrength( int strength1, int strength2 )
        {
            if ( strength1 == strength2 )
                return 0;
            if ( strength1 < strength2 )
                return -1;
            else
                return 1;
        }
        
        /**
         * Compare to pipeline connectors - used to sort by affinity/strength
         * @param connector1
         * @param connector2
         * @return -1 if connector1 < connector2, 0 if equal, 1 if connector1 > connector2
         */
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
