/**
 * $Id$
 */
package com.untangle.app.policy_manager;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.untangle.uvm.SessionMatcher;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.app.PolicyManager.PolicyManagerResult;
import com.untangle.uvm.app.App;
import com.untangle.uvm.util.Pulse;
import com.untangle.uvm.app.AppBase;
import com.untangle.uvm.vnet.PipelineConnector;

/** Implementation of the Policy Manager app */
public class PolicyManagerApp extends AppBase implements com.untangle.uvm.app.PolicyManager
{
    private final Logger logger = Logger.getLogger(getClass());
    private final PipelineConnector[] connectors = new PipelineConnector[] { };

    private final Pulse cleanerPulse = new Pulse("policy-manager-session-cleaner", new SessionExpirationWorker(this), 60000);

    private PolicyManagerSettings settings = new PolicyManagerSettings();
    
    /**
     * PolicyManagerApp
     * @param appSettings
     * @param appProperties
     */
    public PolicyManagerApp( com.untangle.uvm.app.AppSettings appSettings, com.untangle.uvm.app.AppProperties appProperties )
    {
        super( appSettings, appProperties );
    }

    /**
     * getSettings gets the current settings
     * @return PolicyManagerSettings
     */
    public PolicyManagerSettings getSettings()
    {
        return this.settings;
    }

    /**
     * setSettings sets the current settings
     * @param newSettings
     */
    public void setSettings( PolicyManagerSettings newSettings )
    {
        /**
         * Set null IDs for new Policies
         */
        for( PolicySettings policy : newSettings.getPolicies()) 
            if (policy.getPolicyId() == null) 
                policy.setPolicyId(newSettings.nextAvailablePolicyId());

        int idx = 0;
        for( PolicyRule policyRule : newSettings.getRules()) 
            policyRule.setRuleId( ++idx );
        
        /**
         * Update nextPolicyId
         * This is necessary in case they imported a bunch of policy
         * that already have policyIds
         */
        updateNextPolicyIDIfNecessary( newSettings );
        
        /**
         * sanity check settings
         */
        sanityCheck( newSettings );
        
        /**
         * Save the settings
         */
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String appID = this.getAppSettings().getId().toString();
        try {
            settingsManager.save( System.getProperty("uvm.settings.dir") + "/" + "policy-manager/" + "settings_"  + appID + ".js", newSettings );
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to save settings.",e);
            return;
        }

        /**
         * Change current settings
         */
        this.settings = newSettings;
        try {logger.debug("New Settings: \n" + new org.json.JSONObject(this.settings).toString(2));} catch (Exception e) {}


        /**
         * Clear the cache in the pipeline foundry in case of changes to policy rules
         */
        UvmContextFactory.context().pipelineFoundry().clearCache();
    }

    /**
     * getPolicyName gets the policy name for the speficied policy
     * @param policyId
     * @return the policy name
     */
    public String getPolicyName( Integer policyId )
    {
        if ( policyId == null)
            return "Policy-" + policyId;

        PolicySettings settings = getPolicySettings( policyId );
        if ( settings == null )
            return "Policy-" + policyId;
        else
            return  settings.getName();
        
    }

    /**
     * getParentPolicyId gets the parent policy of the specified policy
     * @param policyId 
     * @return the parent policyId or null without a parent
     */ 
    public Integer getParentPolicyId( Integer policyId )
    {
        PolicySettings pSettings = getPolicySettings( policyId );

        if (pSettings != null)
            return pSettings.getParentId();

        return null;
    }

    /**
     * findPolicyId finds the policy that should handle the specified session attributes
     * @param protocol
     * @param clientIntf
     * @param serverIntf
     * @param clientAddr
     * @param serverAddr
     * @param clientPort
     * @param serverPort
     * @return the result
     */
    public PolicyManagerResult findPolicyId( short protocol, int clientIntf, int serverIntf, InetAddress clientAddr, InetAddress serverAddr, int clientPort, int serverPort )
    {
        if ( !isLicenseValid() )
            return new PolicyManagerResult(1,0);

        for (PolicyRule rule : this.settings.getRules()) {
            if (rule.isMatch(protocol,
                             clientIntf, serverIntf,
                             clientAddr, serverAddr,
                             clientPort, serverPort)) {
                return new PolicyManagerResult(rule.getTargetPolicy(),rule.getRuleId());
            }

        }

        /* if none matched - return default policy (1) */
        return new PolicyManagerResult(1,0);
    }

    /**
     * getPoliciesInfo gets the policy information (for the UI)
     * @return a list of the policies
     */
    public ArrayList<JSONObject> getPoliciesInfo()
    {
        ArrayList<JSONObject> policyList = new ArrayList<JSONObject>();
        for ( PolicySettings policySettings: settings.getPolicies() ) {
            try {
                JSONObject json = new org.json.JSONObject();

                json.put("policyId", policySettings.getPolicyId());
                json.put("name", policySettings.getName());
                policyList.add(json);
            } catch (Exception e) {
                logger.warn("Error generating policy list",e);
            }
        }
        
        return policyList;
    }

    /**
     * initializeSettings initializes new PolicySettings
     */
    public void initializeSettings()
    {
        logger.info("Initializing Settings...");
        PolicyManagerSettings settings = new PolicyManagerSettings();
        settings.getPolicies().add(new PolicySettings(1, "Default Policy", "The Default Policy", null));
        this.setSettings(settings);
    }

    /**
     * getPolicyGenerationDiff gets the difference of generations of policies
     * if the child is the child of the parent, then 1 is returned
     * if child is the grandchild of the parent, then 2 is returned etc
     * @param childId
     * @param parentId
     * @return the difference
     */
    public int getPolicyGenerationDiff(Integer childId, Integer parentId)
    {
        if (null == childId) {
            return 0;
        }
        
        if (null == parentId) {
            return -1;
        }

        int distance = 0;
        logger.debug( "Checking the policy: <" + childId + " ->" + parentId);
               
        while (childId != null) {
            logger.debug( "  checking the policy <" + childId + ">" );

            if (childId.equals(parentId)) {
                logger.debug( "  the policy <" + childId + "> matches." );
                return distance;
            }

            childId = getParentPolicyId(childId);
            distance++;
        }

        return -1;
    }

    /**
     * getPolicyIds get a list of policyIds
     * @return int[]
     */
    public int[] getPolicyIds()
    {
        int size = settings.getPolicies().size();
        int policyIds[] = new int[size];

        int i = 0;
        for ( PolicySettings policy : settings.getPolicies() ) {
            policyIds[i] = policy.getPolicyId();
            i++;
        }

        return policyIds;
    }

    /**
     * postInit hook
     */
    protected void postInit()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String appID = this.getAppSettings().getId().toString();
        PolicyManagerSettings readSettings = null;
        String settingsFileName = System.getProperty("uvm.settings.dir") + "/policy-manager/" + "settings_" + appID + ".js";

        try {
            readSettings = settingsManager.load( PolicyManagerSettings.class, settingsFileName );
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to load settings:",e);
        }
        
        /**
         * If there are still no settings, just initialize
         */
        if (readSettings == null) {
            logger.warn("No settings found - Initializing new settings.");

            this.initializeSettings();
        }
        else {
            logger.info("Loading Settings...");

            // UPDATE settings if necessary

            // this is necessary because you could import settings in 9.3
            // and possibly have a wrong nextPolicyId
            updateNextPolicyIDIfNecessary(readSettings);
            
            this.settings = readSettings;
            logger.debug("Settings: " + this.settings.toJSONString());
        }
    }

    /**
     * preStop hook
     * @param isPermanentTransition
     */
    @Override
    protected void preStop( boolean isPermanentTransition )
    {
        cleanerPulse.stop();
    }

    /**
     * postStart hook
     * @param isPermanentTransition
     */
    @Override
    protected void postStart( boolean isPermanentTransition )
    {
        cleanerPulse.start();
    }

    /**
     * preDestroy hook
     */
    @Override
    protected void preDestroy()
    {
        super.preDestroy();

        if ( this.settings.getPolicies().size() > 1 ) {
            throw new RuntimeException( "Unable to destroy policy manager when there are " + this.settings.getPolicies().size() + " policies" );
        }
    }

    /**
     * getConnectors returns the PipelineConnectors for policy
     * @return PipelineConnector[]
     */
    @Override
    protected PipelineConnector[] getConnectors()
    {
        return this.connectors;
    }

    /**
     * getPolicySettings for the specified policyId
     * @param policyId
     * @return PolicySettings
     */
    private PolicySettings getPolicySettings( Integer policyId )
    {
        return getPolicySettings( policyId, this.settings );
    }

    /**
     * getPolicySettings for the specified policyId, in the specified settings
     * @param policyId
     * @param settings
     * @return PolicySettings
     */
    private PolicySettings getPolicySettings( Integer policyId, PolicyManagerSettings settings )
    {
        for (PolicySettings policy : settings.getPolicies()) {
            if (policy.getPolicyId().equals(policyId))
                return policy;
        }
        return null;
    }

    /**
     * sanityCheck the PolicyManagerSettings
     * throws a RuntimeException if something bad is found
     * @param settings
     */
    private void sanityCheck( PolicyManagerSettings settings )
    {
        if (settings == null)
            throw new RuntimeException("NULL settings invalid");
        if (settings.getPolicies() == null)
            throw new RuntimeException("NULL policy list invalid");
        if (settings.getRules() == null)
            throw new RuntimeException("NULL rule list invalid");

        boolean found = false;        
        HashSet<Integer> policyIds = new HashSet<Integer>();
        for ( PolicySettings policy : settings.getPolicies() ) {
            if (policy.getPolicyId() == null)
                throw new RuntimeException("NULL policy ID");
            if (policy.getName() == null)
                throw new RuntimeException("NULL policy Name");
            if (policy.getPolicyId().equals(policy.getParentId()))
                throw new RuntimeException("Policy can not have itself as a parent.");
            if (policy.getPolicyId().equals(1))
                found = true;
            policyIds.add(policy.getPolicyId());
        }
        if (!found)
            throw new RuntimeException("Missing policy Id 1. Can not remove the default policy. [1]");

        for ( PolicySettings policy : settings.getPolicies() ) {

            if (policy.getParentId() != null && policy.getParentId() != 0L && getPolicySettings(policy.getParentId(), settings) == null)
                throw new RuntimeException("Missing policy Id " + policy.getParentId() + ". Can not remove parent of another policy/rack.");
        }

        for ( PolicySettings policy1 : settings.getPolicies() ) {
            int dupeIds = 0;
            for ( PolicySettings policy2 : settings.getPolicies() ) {
                if (policy1.getPolicyId().equals(policy2.getPolicyId())) {
                    dupeIds++; /* it should find itself */
                    if (dupeIds > 1)
                        throw new RuntimeException("Duplicate policy Id: " + policy1.getPolicyId());
                } else {
                    if (policy1.getName().equals(policy2.getName()))
                        throw new RuntimeException("Duplicate policy name: " + policy1.getName());
                }
            }
        }

        for (PolicyRule rule : settings.getRules()) {
            if (rule.getTargetPolicy() != null && rule.getTargetPolicy() != 0L) {
                if (getPolicySettings(rule.getTargetPolicy(), settings) == null) {
                    throw new RuntimeException("Missing policy Id " + rule.getTargetPolicy() + ". Policy is currently used in a policy rule.");
                }
            }
        }

        for ( App app : UvmContextFactory.context().appManager().appInstances() ) {
            if (app.getAppSettings().getPolicyId() == null)
                continue;
            if (!policyIds.contains(app.getAppSettings().getPolicyId()))
                throw new RuntimeException("Missing policy: " + app.getAppSettings().getPolicyId() + " (Required by " + app.getAppProperties().getDisplayName() + " [" + app.getAppSettings().getId() + "]). Cannot delete non-empty racks.");
        }
        

        return;
    }

    /**
     * SessionExpirationWorker periodically checks existing sessions
     * If a session belongs on another policy - it resets the session
     */
    private static class SessionExpirationWorker implements Runnable
    {
        ExpiredPolicyMatcher matcher = new ExpiredPolicyMatcher();
        PolicyManagerApp app;

        /**
         * SessionExpirationWorker
         * @param app
         */
        public SessionExpirationWorker( PolicyManagerApp app)
        {
            this.app = app;
        }
        
        /**
         * run
         */
        public void run()
        {
            app.killMatchingSessionsGlobal(matcher);
        }
    }

    /**
     * ExpiredPolicyMatcher periodically checks existing sessions
     * If a session belongs on another policy - it resets the session
     */
    private static class ExpiredPolicyMatcher implements SessionMatcher
    {
        private static final Logger logger = Logger.getLogger(ExpiredPolicyMatcher.class);

        /**
         * isMatch
         * @param oldPolicyId
         * @param protocol
         * @param clientIntf
         * @param serverIntf
         * @param clientAddr
         * @param serverAddr
         * @param clientPort
         * @param serverPort
         * @param attachments
         * @return true if the session should be a different policy
         */
        public boolean isMatch( Integer oldPolicyId, short protocol, int clientIntf, int serverIntf, InetAddress clientAddr, InetAddress serverAddr, int clientPort, int serverPort, Map<String,Object> attachments )
        {
            PolicyManagerApp policyManager = (PolicyManagerApp) UvmContextFactory.context().appManager().app("policy-manager");
            Integer newPolicyId = null;
            Integer newPolicyRuleId = null;
            if (policyManager != null) {
                PolicyManagerResult result = policyManager.findPolicyId( protocol,
                                                                         clientIntf, serverIntf,
                                                                         clientAddr, serverAddr,
                                                                         clientPort, serverPort );
                if ( result != null ) {
                    newPolicyId = result.policyId;
                    newPolicyRuleId = result.policyRuleId;
                }
            }

            if (logger.isDebugEnabled())
                logger.debug("Evaluating session for policy switch: " +
                             clientAddr.toString() + ":" + clientPort + " -> " +
                             serverAddr.toString() + ":" + serverPort +
                             " Old policy: " + oldPolicyId + " New policy: " + newPolicyId);

            /** If either policy is null, just check if they are equal */
            if (newPolicyId == null || oldPolicyId == null ) {
                return newPolicyId != oldPolicyId;   
            } else if (newPolicyId == oldPolicyId) {
                return false;
            } else if (newPolicyId.equals(oldPolicyId)) {
                return false;
            } else {
                logger.info("Resetting session for policy switch: " +
                            clientAddr.toString() + ":" + clientPort + " -> " +
                            serverAddr.toString() + ":" + serverPort +
                            " Old policy: " + oldPolicyId + " New policy: " + newPolicyId + " New rule ID: " + newPolicyRuleId);
                return true;
            }
        }
    }

    /**
     * Check to make sure nextPolicyId is bigger than any existisng
     * policy ID This was somehow messed up in 9.3 or prior
     * @param settings
     */
    private void updateNextPolicyIDIfNecessary( PolicyManagerSettings settings )
    {
        int biggestPolicyId = 1;
        for (PolicySettings policy : settings.getPolicies()) {
            if (policy.getPolicyId() > biggestPolicyId)
                biggestPolicyId = policy.getPolicyId();
        }
        if (settings.getNextPolicyId() <= biggestPolicyId) {
            logger.info("Updating next policy ID");
            settings.setNextPolicyId( biggestPolicyId+1 );
        }
    }
}
