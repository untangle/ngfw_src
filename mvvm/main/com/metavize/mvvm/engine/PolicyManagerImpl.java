/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

package com.metavize.mvvm.engine;

import java.util.*;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.policy.Policy;
import com.metavize.mvvm.policy.PolicyConfiguration;
import com.metavize.mvvm.policy.PolicyException;
import com.metavize.mvvm.policy.PolicyManager;
import com.metavize.mvvm.policy.SystemPolicyRule;
import com.metavize.mvvm.policy.UserPolicyRule;
import com.metavize.mvvm.policy.UserPolicyRuleSet;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Query;
import net.sf.hibernate.Session;
import net.sf.hibernate.Transaction;
import org.apache.log4j.Logger;

public class PolicyManagerImpl implements PolicyManager
{
    private static final String INITIAL_POLICY_NAME = "Default";
    private static final String INITIAL_POLICY_NOTES = "The default policy";

    private static final Logger logger = Logger.getLogger( PolicyManagerImpl.class );

    private static PolicyManagerImpl POLICY_MANAGER = new PolicyManagerImpl();

    private List<Policy> allPolicies; // Also contains default one
    private Policy defaultPolicy;

    private Object policyRuleLock = new Object();
    private UserPolicyRuleSet userRuleSet;
    private SystemPolicyRule[] sysRules;
    private UserPolicyRule[] userRules;

    private PolicyManagerImpl() {
        allPolicies = new ArrayList<Policy>();

        Session s = MvvmContextFactory.context().openSession();
        try {
            Transaction tx = s.beginTransaction();

            Query q = s.createQuery("from Policy p");
            List results = q.list();

            if (results.size() == 0) {
                logger.info("Empty policy table.  Creating default policy.");
                defaultPolicy = new Policy(true, INITIAL_POLICY_NAME, INITIAL_POLICY_NOTES);
                allPolicies.add(defaultPolicy);
                s.save(defaultPolicy);
            } else {
                for (Object o : results) {
                    Policy policy = (Policy)o;
                    allPolicies.add(policy);
                    if (policy.isDefault()) {
                        assert defaultPolicy == null;
                        defaultPolicy = policy;
                    }
                }
                assert defaultPolicy != null;
            }

            q = s.createQuery("from UserPolicyRuleSet uprs");
            UserPolicyRuleSet uprs = (UserPolicyRuleSet) q.uniqueResult();
            if (uprs == null) {
                logger.info("Empty User Policy Rule Set.  Creating empty one.");
                uprs = new UserPolicyRuleSet();
                s.save(uprs);
            }

            tx.commit();
        } catch (HibernateException exn) {
            logger.fatal("could not get Policies", exn);
            // Now what? XXX
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                logger.warn("could not close session", exn);
            }
        }

        logger.info("Initialized PolicyManager");
    }

    public static PolicyManagerImpl policyManager()
    {
        return POLICY_MANAGER;
    }


    private static final Policy[] POLICY_ARRAY_PROTO = new Policy[0];

    public Policy[] getPolicies() {
        return (Policy[]) allPolicies.toArray(POLICY_ARRAY_PROTO);
    }

    public Policy getDefaultPolicy() {
        return defaultPolicy;
    }

    public void addPolicy(String name, String notes)
        throws PolicyException
    {
        if (name == null)
            throw new PolicyException("New policy must have a name");
        for (Policy p : allPolicies) {
            if (name.equalsIgnoreCase(p.getName()))
                throw new PolicyException("A policy named " + name + " already exists");
        }

        Policy p = new Policy(false, name, notes);

        Session s = MvvmContextFactory.context().openSession();
        try {
            Transaction tx = s.beginTransaction();

            s.saveOrUpdateCopy(p);

            tx.commit();
        } catch (HibernateException exn) {
            logger.error("could not save Policy", exn);
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                logger.warn("could not close session", exn); // XXX TransExn
            }
        }

        allPolicies.add(p);
    }

    public void removePolicy(Policy p)
        throws PolicyException
    {
        if (p == null)
            throw new PolicyException("Must specify a policy to remove");
        if (p.isDefault())
            throw new PolicyException("Cannot remove the default policy");
        if (!allPolicies.contains(p))
            throw new PolicyException("Policy " + p.getName() + " not found in all policies");
        if (isInUse(p))
            throw new PolicyException("Policy " + p.getName() + " cannot be removed because it is in use");

        Session s = MvvmContextFactory.context().openSession();
        try {
            Transaction tx = s.beginTransaction();

            s.delete(p);

            tx.commit();
        } catch (HibernateException exn) {
            logger.error("could not remove Policy", exn);
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                logger.warn("could not close session", exn); // XXX TransExn
            }
        }

        allPolicies.remove(p);
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

        // Here we need to check the transform manager to make sure. XXXXXXXX
        throw new Error("ack");
    }

    public SystemPolicyRule[] getSystemPolicyRules() {
        return sysRules;
    }

    public void setSystemPolicy(SystemPolicyRule rule, Policy p, boolean inbound) {
        // more Sanity checking (policy) XXX
        synchronized(policyRuleLock) {
            for (int i = 0; i < sysRules.length; i++) {
                if (sysRules[i] == rule) {
                    Session s = MvvmContextFactory.context().openSession();
                    try {
                        Transaction tx = s.beginTransaction();

                        rule.setPolicy(p);
                        rule.setInbound(inbound);
                        s.saveOrUpdateCopy(rule);

                        tx.commit();
                    } catch (HibernateException exn) {
                        logger.error("could not save change to SystemRule", exn);
                    } finally {
                        try {
                            s.close();
                        } catch (HibernateException exn) {
                            logger.warn("could not close hibernate session", exn);
                        }
                    }
                }
            }
        }
    }

    public UserPolicyRuleSet getUserPolicyRules() {
        return userRuleSet;
    }

    public void setUserPolicyRules(UserPolicyRuleSet ruleSet) {
        // Sanity checking XXX
        synchronized(policyRuleLock) {
            Session s = MvvmContextFactory.context().openSession();
            try {
                Transaction tx = s.beginTransaction();

                s.saveOrUpdateCopy(ruleSet);
                userRuleSet = ruleSet;
                userRules = (UserPolicyRule[]) ruleSet.getRules().toArray(new UserPolicyRule[] { });

                tx.commit();
            } catch (HibernateException exn) {
                logger.warn("could not get HttpSettings", exn);
            } finally {
                try {
                    s.close();
                } catch (HibernateException exn) {
                    logger.warn("could not close hibernate session", exn);
                }
            }
        }
    }


    // For da UI
    public PolicyConfiguration getPolicyConfiguration() {
        PolicyConfiguration result = new PolicyConfiguration(allPolicies, sysRules, userRules);
        return result;
    }

    // For da UI
    public void setPolicyConfiguration(PolicyConfiguration pc)
        throws PolicyException
    {
        // Need to do a lot of error checking here...
        // Need implmenetation XXX
    }

    // package protected methods ----------------------------------------------

    // MVVM calls in here at boot time and whenever a new interface is
    // added or removed, passing all interfaces.  We automatically add
    // or removeSystemPolicyRules as appropriate.  We also build the
    // in-memory UserPolicyRule list.
    void reconfigure(byte[] interfaces)
    {
        // For now do nothing
        if (allPolicies.size() == 0)
            // Always
            return;

        synchronized(policyRuleLock) {
            if (logger.isDebugEnabled())
                logger.debug("Setting interfaces to " + interfaces);
            Session s = MvvmContextFactory.context().openSession();
            try {
                Transaction tx = s.beginTransaction();

                Query sysq = s.createQuery("from SystemPolicyRule spr");
                List existingSys = sysq.list();
                Query userq = s.createQuery("from UserPolicyRuleSet uprs");
                UserPolicyRuleSet uprs = (UserPolicyRuleSet) userq.uniqueResult();
                List existingUser = uprs.getRules();

                Set goodSys = new HashSet();
                Set goodUser = new HashSet();

                // For each interface pair
                for (int i = 0; i < interfaces.length - 1; i++) {
                    for (int j = i+1; j < interfaces.length; j++) {
                        byte firstIntf = interfaces[i];
                        byte secondIntf = interfaces[j];

                        // Add in the missing system rules
                        boolean foundSys = false;
                        for (Object o : existingSys) {
                            SystemPolicyRule spr = (SystemPolicyRule)o;
                            byte clientIntf = spr.getClientIntf();
                            byte serverIntf = spr.getServerIntf();
                            if ((clientIntf == firstIntf && serverIntf == secondIntf) ||
                                (clientIntf == secondIntf && serverIntf == firstIntf)) {
                                // Good to go.
                                if (logger.isDebugEnabled())
                                    logger.debug("Found existing SystemPolicyRule for ci: " + clientIntf + ", si: " + serverIntf);
                                goodSys.add(spr);
                                foundSys = true;
                            }
                        }
                        if (!foundSys) {
                            logger.info("Adding new default inbound SystemPolicyRule for ci: " + firstIntf + ", si: " + secondIntf);
                            SystemPolicyRule newInRule = new SystemPolicyRule(firstIntf, secondIntf, defaultPolicy, true);
                            logger.info("Adding new default outbound SystemPolicyRule for ci: " + secondIntf + ", si: " + firstIntf);
                            SystemPolicyRule newOutRule = new SystemPolicyRule(secondIntf, firstIntf, defaultPolicy, false);
                            s.save(newInRule);
                            s.save(newOutRule);
                            goodSys.add(newInRule);
                            goodSys.add(newOutRule);
                        }

                        // Record good user rules.
                        for (Object o : existingUser) {
                            UserPolicyRule upr = (UserPolicyRule)o;
                            byte clientIntf = upr.getClientIntf();
                            byte serverIntf = upr.getServerIntf();
                            if ((clientIntf == firstIntf && serverIntf == secondIntf) ||
                                (clientIntf == secondIntf && serverIntf == firstIntf)) {
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
                s.saveOrUpdateCopy(uprs);

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

                tx.commit();
            } catch (HibernateException exn) {
                logger.fatal("could not get PolicyRules", exn);
                // Now what? XXX
            } finally {
                try {
                    s.close();
                } catch (HibernateException exn) {
                    logger.warn("could not close session", exn);
                }
            }
        }
    }
}
