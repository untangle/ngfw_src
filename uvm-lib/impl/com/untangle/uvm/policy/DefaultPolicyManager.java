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

package com.untangle.uvm.policy;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

import com.untangle.uvm.ArgonManager;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.UvmException;
import com.untangle.uvm.license.LicenseStatus;
import com.untangle.uvm.license.ProductIdentifier;
import com.untangle.uvm.localapi.SessionMatcherFactory;
import com.untangle.uvm.node.LocalNodeManager;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.Validator;
import com.untangle.uvm.security.Tid;
import com.untangle.uvm.util.TransactionWork;
import com.untangle.uvm.vnet.PipelineFoundry;

class DefaultPolicyManager implements LocalPolicyManager
{
    private static final String INITIAL_POLICY_NAME = "Default Rack";
    private static final String INITIAL_POLICY_NOTES = "The default rack";

    private final Logger logger = Logger.getLogger(getClass());

    private volatile UserPolicyRule[] userRules = new UserPolicyRule[0];
    /* This is the complete original list of rules that are disabled,
     * going to the default rack. */
    private volatile UserPolicyRule[] cUserRules = new UserPolicyRule[0];

    private Policy defaultPolicy;
    private DefaultPolicyRule defaultPolicyRule;

    private Object policyRuleLock = new Object();
    private UserPolicyRuleSet userRuleSet;

    // constructor ------------------------------------------------------------

    DefaultPolicyManager() {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    defaultPolicy = null;
                    Query q = s.createQuery("from Policy p order by id asc");
                    List results = q.list();
                    for (Object o : results) {
                        Policy policy = (Policy)o;
                        if (policy.isDefault()) {
                            defaultPolicy = policy;
                            break;
                        }
                    }
                    if (defaultPolicy == null) {
                        logger.info("Empty policy table.  Creating default policy.");
                        defaultPolicy = new Policy(true, INITIAL_POLICY_NAME, INITIAL_POLICY_NOTES);
                        s.save(defaultPolicy);
                    }
                    defaultPolicyRule = new DefaultPolicyRule(defaultPolicy);

                    q = s.createQuery("from UserPolicyRuleSet uprs");
                    results = q.list();
                    if (results.size() == 0) {
                        logger.info("Empty User Policy Rule Set.  Creating empty one.");
                        UserPolicyRuleSet uprs = new UserPolicyRuleSet();
                        s.save(uprs);
                    } else if (results.size() > 1) {
                        logger.fatal("Found " + results.size() + " user policy rule sets! Deleting all but first");
                        for (int i = 1; i < results.size(); i++) {
                            UserPolicyRuleSet uprs = (UserPolicyRuleSet) results.get(i);
                            s.delete(uprs);
                        }
                    }
                    return true;
                }

                public Object getResult() { return null; }
            };

        LocalUvmContextFactory.context().runTransaction(tw);

        /* This is in a separate function for historical reasons, but could
         * be joined with loading uprs above. */
        loadUserRules();

        logger.info("Initialized PolicyManager");
    }

    public Policy[] getPolicies() {
        Policy[] result = new Policy[] { defaultPolicy };
        return result;
    }

    public Policy getPolicy(String name)
    {
        if (name.equals(INITIAL_POLICY_NAME))
            return defaultPolicy;
        return null;
    }

    public Policy getDefaultPolicy() {
        return defaultPolicy;
    }

    public void addPolicy(String name, String notes)
        throws PolicyException
    {
        throw new PolicyException("Professional edition only");
    }

    public void addPolicy(String name, String notes, Policy parent)
        throws PolicyException
    {
        throw new PolicyException("Professional edition only");
    }

    public void removePolicy(final Policy p) throws PolicyException
    {
        throw new PolicyException("Professional edition only");
    }

    public void setPolicy(final Policy p, String name, String notes,
                          Policy parent)
        throws PolicyException
    {
        throw new PolicyException("Professional edition only");
    }

    public UserPolicyRule[] getUserPolicyRules() {
        return userRules;
    }

    public void setUserPolicyRules(final List<UserPolicyRule> rules) {
        // Sanity checking XXX
        synchronized(policyRuleLock) {
            TransactionWork tw = new TransactionWork()
                {
                    public boolean doWork(Session s)
                    {
                        List<UserPolicyRule> urs = userRuleSet.getRules();
                        urs.clear();
                        urs.addAll(rules);
                        updateRules(rules);
                        s.saveOrUpdate(userRuleSet);
                        return true;
                    }

                    public Object getResult() { return null; }
                };
            LocalUvmContextFactory.context().runTransaction(tw);
        }
        
        updateEngines();
    }

    // For da UI
    public PolicyConfiguration getPolicyConfiguration()
    {
        List<Policy> pl = new ArrayList<Policy>();
        pl.add(defaultPolicy);

        PolicyConfiguration result = new PolicyConfiguration(pl, cUserRules);
        result.setHasRackManagement(false);
        
        LocalNodeManager nodeManager = LocalUvmContextFactory.context().nodeManager();
        List<Tid> l = nodeManager.nodeInstances( "untangle-node-adconnector" );
        result.setHasUserManagement(false);
        
        if ( l.size() > 0 ) {
            try {
            LicenseStatus ls = LocalUvmContextFactory.context().localLicenseManager().getLicenseStatus(ProductIdentifier.ADDRESS_BOOK);
            if ( !ls.isExpired()) {
                result.setHasUserManagement(true);
            }
            } catch ( UvmException e ) {
                logger.warn( "Unable to update adconnector status.");
            }
        }

        return result;
    }

    // For da UI
    public void setPolicyConfiguration(PolicyConfiguration pc)
        throws PolicyException
    {
        List<UserPolicyRule> userpc = pc.getUserPolicyRules();

        // Sanity check the user rules
        if (userpc == null)
            throw new PolicyException("User rules missing");
        // Really need more checking here.  XXX
        List<UserPolicyRule> newUserRules = new ArrayList<UserPolicyRule>(userpc.size());
        for (Object o : userpc) {
            UserPolicyRule upr = (UserPolicyRule)o;
            newUserRules.add(upr);
        }

        // Now do the actual setting
        synchronized(policyRuleLock) {
            setUserPolicyRules(newUserRules);
        }
        
        updateEngines();
    }

    /**
     * shut down all sessions associated with the given policy - shut down
     * all sessions if policy is null.
     *
     * ***TODO: add logic to shutdown sessions for just a given policy.
     *
     * ***TODO: this should potentially be refactored into a RemoteArgonManager
     *          class with this being its only method - this way the existing
     *          shutdownMatches() method can be used.
     */
    public void shutdownSessions(Policy policy)
    {
        ArgonManager argonManager = LocalUvmContextFactory.context().argonManager();
    argonManager.shutdownMatches(SessionMatcherFactory.makePolicyInstance(policy));
    }

    // LocalPolicyManager methods ---------------------------------------------
    public UserPolicyRule[] getUserRules()
    {
        return userRules;
    }

    public String productIdentifier()
    {
        return ProductIdentifier.POLICY_MANAGER;
    }

    public PolicyRule getDefaultPolicyRule()
    {
        return defaultPolicyRule;
    }

    /* re/load the user rules from the database */
    void loadUserRules()
    {
        // For now do nothing (this should never be true)
        // intended for the case where something was initialized improperly?
        if (defaultPolicy == null) {
            return; // Always
        }

        synchronized(policyRuleLock) {
            TransactionWork tw = new TransactionWork()
                {
                    public boolean doWork(Session s)
                    {
                        Query userq = s.createQuery("from UserPolicyRuleSet uprs");
                        UserPolicyRuleSet uprs = (UserPolicyRuleSet) userq.uniqueResult();
                        List<UserPolicyRule> existingUser = uprs.getRules();

                        userRuleSet = uprs;
                        updateRules(existingUser);

                        return true;
                    }

                    public Object getResult() { return null; }
                };
            LocalUvmContextFactory.context().runTransaction(tw);
        }
    }

    /**
     * create updated policy rules.  designed to minimize suprises
     * when going from trial -> expired.  user policy rules with
     * non-default policies (not null or the default policy) are
     * disabled.  This doesn't affect the database at all, as rules
     * are copied, but if the user goes into the policy manager and
     * saves settings, the new values are written to the database.
     */
    private void updateRules(List<UserPolicyRule> userPolicyRules)
    {
        List<UserPolicyRule> userPolicyList = new LinkedList<UserPolicyRule>();
        List<UserPolicyRule> completePolicyList = new LinkedList<UserPolicyRule>();

        for (UserPolicyRule upr : userPolicyRules){
            /* Disable the rules with non-default policies */
            Policy policy = upr.getPolicy();
            if ((policy == null ) || (policy.equals(this.defaultPolicy))) {
                userPolicyList.add(upr);
                completePolicyList.add(upr);
            } else {
                /* Create a new rule, that is not live, that goes to
                 * the default rack */
                UserPolicyRule newRule =
                    new UserPolicyRule(upr.getClientIntf(),upr.getServerIntf(),
                                       this.defaultPolicy,upr.getProtocol(),
                                       upr.getClientAddr(),upr.getServerAddr(),
                                       upr.getClientPort(),upr.getServerPort(),
                                       upr.getStartTime(),upr.getEndTime(),
                                       upr.getDayOfWeek(),upr.getUser(),
                                       false,upr.isInvertEntireDuration());

                completePolicyList.add(newRule);
            }
        }

        this.userRules = userPolicyList.toArray(new UserPolicyRule[0]);
        this.cUserRules = completePolicyList.toArray(new UserPolicyRule[0]);
        
        updateEngines();
    }

    public boolean matchesPolicy(Node node, Policy p)
    {
        Policy tp = node.getTid().getPolicy();
        
        if (null == tp) {
            return true;
        }

        if (tp.equals(p)) {
            return true;
        }

        return false;
    }
    
    @Override
    public int getNumParents(Policy child, Policy parent)
    {
        if ( null == child ) {
            return 0;
        }
        
        if (child.equals( parent )) {
            return 0;
        }
        
        return -1;
    }


    public Policy getParent(Policy p)
    {
        return null;
    }

    public Validator getValidator() {
        return new PolicyValidator();
    }
    
    void updateEngines()
    {
        /* At startup, these can be null */
        LocalNodeManager nodeManager = LocalUvmContextFactory.context().nodeManager();
        if ( nodeManager != null ) {
            nodeManager.flushNodeStateCache();
        }
        
        PipelineFoundry pipelineFoundry = LocalUvmContextFactory.context().pipelineFoundry();
        if ( pipelineFoundry != null ) {
            pipelineFoundry.clearChains();
        }
    }
}
