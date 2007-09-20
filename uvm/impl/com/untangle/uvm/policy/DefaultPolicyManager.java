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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.license.ProductIdentifier;
import com.untangle.uvm.node.LocalNodeManager;
import com.untangle.uvm.node.firewall.intf.IntfMatcher;
import com.untangle.uvm.util.TransactionWork;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

class DefaultPolicyManager implements LocalPolicyManager
{
    private static final String INITIAL_POLICY_NAME = "Default Rack";
    private static final String INITIAL_POLICY_NOTES = "The default rack";

    private final Logger logger = Logger.getLogger(getClass());

    private volatile UserPolicyRule[] userRules = new UserPolicyRule[0];
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

    public void removePolicy(final Policy p) throws PolicyException
    {
        throw new PolicyException("Professional edition only");
    }

    public void setPolicy(final Policy p, String name, String notes)
        throws PolicyException
    {
        throw new PolicyException("Professional edition only");
    }

    public UserPolicyRule[] getUserPolicyRules() {
        return userRules;
    }

    public void setUserPolicyRules(final List rules) {
        // Sanity checking XXX
        synchronized(policyRuleLock) {
            TransactionWork tw = new TransactionWork()
                {
                    public boolean doWork(Session s)
                    {
                        List urs = userRuleSet.getRules();
                        urs.clear();
                        urs.addAll(rules);
                        userRules = (UserPolicyRule[])rules.toArray(new UserPolicyRule[] { });
                        s.saveOrUpdate(userRuleSet);
                        return true;
                    }

                    public Object getResult() { return null; }
                };
            LocalUvmContextFactory.context().runTransaction(tw);
        }
    }

    // For da UI
    public PolicyConfiguration getPolicyConfiguration() {

        List pl = new ArrayList();
        pl.add(defaultPolicy);

        PolicyConfiguration result = new PolicyConfiguration(pl, cUserRules);
        return result;
    }

    // For da UI
    public void setPolicyConfiguration(PolicyConfiguration pc)
        throws PolicyException
    {
        List userpc = pc.getUserPolicyRules();

        // Sanity check the user rules
        if (userpc == null)
            throw new PolicyException("User rules missing");
        // Really need more checking here.  XXX
        List newUserRules = new ArrayList(userpc.size());
        for (Object o : userpc) {
            UserPolicyRule upr = (UserPolicyRule)o;
            newUserRules.add(upr);
        }

        // Now do the actual setting
        synchronized(policyRuleLock) {
            setUserPolicyRules(newUserRules);
        }
    }

    // LocalPolicyManager methods ---------------------------------------------

    // UVM calls in here at boot time and whenever a new interface is
    // added or removed, passing all interfaces.  We also build the
    // in-memory UserPolicyRule list.
    public void reconfigure(final byte[] interfaces)
    {
        // For now do nothing (this should never be true)
        // intended for the case where something was initialized improperly?
        if (defaultPolicy == null) {
            return; // Always
        }

        synchronized(policyRuleLock) {
            if (logger.isDebugEnabled()) {
                logger.debug("Setting interfaces to " + interfaces);
            }

            TransactionWork tw = new TransactionWork()
                {
                    public boolean doWork(Session s)
                    {
                        Query userq = s.createQuery("from UserPolicyRuleSet uprs");
                        UserPolicyRuleSet uprs = (UserPolicyRuleSet) userq.uniqueResult();
                        List existingUser = uprs.getRules();

                        Set goodUser = new HashSet();

                        // For each interface pair
                        for (int i = 0; i < interfaces.length - 1; i++) {
                            for (int j = i+1; j < interfaces.length; j++) {
                                byte firstIntf = interfaces[i];
                                byte secondIntf = interfaces[j];

                                boolean foundForward = false;
                                boolean foundBackward = false;

                                // Record good user rules.
                                for (Object o : existingUser) {
                                    UserPolicyRule upr = (UserPolicyRule)o;
                                    IntfMatcher clientIntf = upr.getClientIntf();
                                    IntfMatcher serverIntf = upr.getServerIntf();
                                    if ((clientIntf.isMatch(firstIntf) && serverIntf.isMatch(secondIntf)) ||
                                        (clientIntf.isMatch(secondIntf) && serverIntf.isMatch(firstIntf))) {
                                        // Good to go.
                                        if (logger.isDebugEnabled())
                                            logger.debug("Found existing UserPolicyRule for ci: " + clientIntf + ", si: " + serverIntf);
                                        goodUser.add(upr);
                                    }
                                }
                            }
                        }

                        // Get rid of the extra user rules.
                        existingUser.retainAll(goodUser);
                        uprs.setRules(existingUser);
                        s.saveOrUpdate(uprs);

                        userRuleSet = uprs;

                        return true;
                    }

                    public Object getResult() { return null; }
                };
            LocalUvmContextFactory.context().runTransaction(tw);
        }
    }

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
    }
}
