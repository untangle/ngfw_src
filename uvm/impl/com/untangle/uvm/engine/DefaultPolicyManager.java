/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.uvm.engine;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.license.ProductIdentifier;
import com.untangle.uvm.policy.LocalPolicyManager;
import com.untangle.uvm.policy.Policy;
import com.untangle.uvm.policy.PolicyConfiguration;
import com.untangle.uvm.policy.PolicyException;
import com.untangle.uvm.policy.PolicyManager;
import com.untangle.uvm.policy.SystemPolicyRule;
import com.untangle.uvm.policy.UserPolicyRule;
import com.untangle.uvm.policy.UserPolicyRuleSet;
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

    // package protected variables (used by PipelineFoundryImpl)
    private volatile UserPolicyRule[] userRules;
    private volatile SystemPolicyRule[] sysRules;

    private Policy defaultPolicy;

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

        UvmContextFactory.context().runTransaction(tw);

        logger.info("Initialized PolicyManager");
    }

    private static final Policy[] POLICY_ARRAY_PROTO = new Policy[0];

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

    protected boolean isInUse(Policy p)
    {
        synchronized(policyRuleLock) {
            for (SystemPolicyRule spr : sysRules) {
                if (spr.getPolicy() == p)
                    return true;
            }
            for (UserPolicyRule upr : userRules) {
                if (upr.getPolicy() == p)
                    return true;
            }
        }

        LocalNodeManager tm = UvmContextFactory.context().nodeManager();
        return 0 < tm.nodeInstances(p).size();
    }

    public SystemPolicyRule[] getSystemPolicyRules() {
        return sysRules;
    }

    public void setSystemPolicyRule(final SystemPolicyRule rule,
                                    final Policy p, final boolean inbound,
                                    final String description) {
        // more Sanity checking (policy) XXX
        synchronized(policyRuleLock) {
            for (int i = 0; i < sysRules.length; i++) {
                if (sysRules[i] == rule) {
                    TransactionWork tw = new TransactionWork()
                        {
                            public boolean doWork(Session s)
                            {
                                rule.setPolicy(p);
                                rule.setInbound(inbound);
                                rule.setDescription(description);
                                s.saveOrUpdate(rule);
                                return true;
                            }

                            public Object getResult() { return null; }
                        };
                    UvmContextFactory.context().runTransaction(tw);
                }
            }
        }
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
            UvmContextFactory.context().runTransaction(tw);
        }
    }

    // For da UI
    public PolicyConfiguration getPolicyConfiguration() {

        List pl = new ArrayList();
        pl.add(defaultPolicy);
        PolicyConfiguration result = new PolicyConfiguration(pl, sysRules, userRules);
        return result;
    }

    // For da UI
    public void setPolicyConfiguration(PolicyConfiguration pc)
        throws PolicyException
    {
        List syspc = pc.getSystemPolicyRules();
        List userpc = pc.getUserPolicyRules();

        // Sanity check the system rules
        if (syspc == null || syspc.size() < 1)
            throw new PolicyException("System rules missing or empty");
        // Really need more checking here.  They shouldn't be able to delete or add any rows. XXX
        SystemPolicyRule[] newSysRules = new SystemPolicyRule[syspc.size()];
        int i = 0;
        for (Object o : syspc) {
            SystemPolicyRule spr = (SystemPolicyRule)o;
            newSysRules[i++] = spr;
        }

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
            for (SystemPolicyRule newspr : newSysRules) {
                boolean foundIt = false;
                for (SystemPolicyRule oldspr : sysRules) {
                    if (newspr.isSameRow(oldspr)) {
                        if (foundIt)
                            throw new PolicyException("System Policy rule duplicated");
                        foundIt = true;
                        setSystemPolicyRule(oldspr, newspr.getPolicy(), newspr.isInbound(), newspr.getDescription());
                    }
                }
                if (!foundIt)
                    throw new PolicyException("System Policy rule to be changed not found");
            }

            setUserPolicyRules(newUserRules);
        }
    }

    // LocalPolicyManager methods ---------------------------------------------

    // UVM calls in here at boot time and whenever a new interface is
    // added or removed, passing all interfaces.  We automatically add
    // or removeSystemPolicyRules as appropriate.  We also build the
    // in-memory UserPolicyRule list.
    //
    // Note that the interfaces list should be sorted from outside to
    // inside.  This is because we create the missing system policy
    // rules given this sorting.
    public void reconfigure(final byte[] interfaces)
    {
        // For now do nothing
        if (defaultPolicy == null)
            // Always
            return;

        synchronized(policyRuleLock) {
            if (logger.isDebugEnabled()) {
                logger.debug("Setting interfaces to " + interfaces);
            }

            TransactionWork tw = new TransactionWork()
                {
                    public boolean doWork(Session s)
                    {
                        Query sysq = s.createQuery("from SystemPolicyRule spr");
                        List existingSys = sysq.list();
                        Query userq = s.createQuery("from UserPolicyRuleSet uprs");
                        UserPolicyRuleSet uprs = (UserPolicyRuleSet) userq.uniqueResult();
                        List existingUser = uprs.getRules();

                        List goodSys = new ArrayList();
                        Set goodUser = new HashSet();

                        // For each interface pair
                        for (int i = 0; i < interfaces.length - 1; i++) {
                            for (int j = i+1; j < interfaces.length; j++) {
                                byte firstIntf = interfaces[i];
                                byte secondIntf = interfaces[j];

                                // Add in the missing system rules
                                boolean foundForward = false;
                                boolean foundBackward = false;
                                for (Object o : existingSys) {
                                    SystemPolicyRule spr = (SystemPolicyRule)o;
                                    byte clientIntf = spr.getClientIntf();
                                    byte serverIntf = spr.getServerIntf();
                                    if (clientIntf == firstIntf && serverIntf == secondIntf) {
                                        if (foundForward) {
                                            logger.fatal("Found extra SystemPolicyRule for ci: " + clientIntf + ", si: " + serverIntf);
                                        } else {
                                            if (logger.isDebugEnabled())
                                                logger.debug("Found existing SystemPolicyRule for ci: " + clientIntf + ", si: " + serverIntf);
                                            goodSys.add(spr);
                                            foundForward = true;
                                        }
                                    } else if (clientIntf == secondIntf && serverIntf == firstIntf) {
                                        if (foundBackward) {
                                            logger.fatal("Found extra SystemPolicyRule for ci: " + clientIntf + ", si: " + serverIntf);
                                        } else {
                                            if (logger.isDebugEnabled())
                                                logger.debug("Found existing SystemPolicyRule for ci: " + clientIntf + ", si: " + serverIntf);
                                            goodSys.add(spr);
                                            foundBackward = true;
                                        }
                                    }
                                }
                                if (!foundForward) {
                                    logger.info("Adding new default inbound SystemPolicyRule for ci: " + firstIntf + ", si: " + secondIntf);
                                    SystemPolicyRule newInRule = new SystemPolicyRule(firstIntf, secondIntf, defaultPolicy, true);
                                    s.saveOrUpdate(newInRule);
                                    goodSys.add(newInRule);
                                }
                                if (!foundBackward) {
                                    logger.info("Adding new default outbound SystemPolicyRule for ci: " + secondIntf + ", si: " + firstIntf);
                                    SystemPolicyRule newOutRule = new SystemPolicyRule(secondIntf, firstIntf, defaultPolicy, false);
                                    s.saveOrUpdate(newOutRule);
                                    goodSys.add(newOutRule);
                                }

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

                        // Finally, get rid of the extra system ones.
                        existingSys.removeAll(goodSys);
                        for (Object o : existingSys) {
                            SystemPolicyRule spr = (SystemPolicyRule)o;
                            logger.info("Removing unused SystemPolicyRule for ci: " + spr.getClientIntf() + ", si: " + spr.getServerIntf());
                            s.delete(spr);
                        }

                        userRuleSet = uprs;
                        userRules = (UserPolicyRule[]) existingUser.toArray(new UserPolicyRule[] { });
                        sysRules = (SystemPolicyRule[]) goodSys.toArray(new SystemPolicyRule[] { });

                        return true;
                    }

                    public Object getResult() { return null; }
                };
            UvmContextFactory.context().runTransaction(tw);
        }
    }

    public UserPolicyRule[] getUserRules()
    {
        return userRules;
    }

    public SystemPolicyRule[] getSystemRules()
    {
        return sysRules;
    }

    public String productIdentifier()
    {
        return ProductIdentifier.POLICY_MANAGER;
    }
}
