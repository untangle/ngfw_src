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

package com.untangle.uvm.policy;

import java.util.List;

import com.untangle.uvm.license.LicensedProduct;

public interface LocalPolicyManager extends LicensedProduct
{
    /**
     * @see PolicyManager#getPolicies()
     */
    Policy[] getPolicies();

    /**
     * @see PolicyManager#getDefaultPolicy()
     */
    Policy getDefaultPolicy();

    /**
     * @see PolicyManager#getPolicy()
     */
    Policy getPolicy(String name);

    /**
     * @see PolicyManager#addPolicy()
     */
    void addPolicy(String name, String notes) throws PolicyException;

    /**
     * @see PolicyManager#removePolicy()
     */
    void removePolicy(Policy policy) throws PolicyException;

    void setPolicy(Policy rule, String name, String notes)
        throws PolicyException;

    /**
     * @see PolicyManager#getSystemPolicyRules()
     */
    SystemPolicyRule[] getSystemPolicyRules();

    /**
     * @see PolicyManager#setSystemPolicyRule()
     */
    void setSystemPolicyRule(SystemPolicyRule rule, Policy p, boolean inbound,
                             String description);

    /**
     * @see PolicyManager#getUserPolicyRules()
     */
    UserPolicyRule[] getUserPolicyRules();

    /**
     * @see PolicyManager#setUserPolicyRules()
     */
    void setUserPolicyRules(List rules);

    /**
     * @see PolicyManager#getPolicyConfiguration()
     */
    PolicyConfiguration getPolicyConfiguration();

    /**
     * @see PolicyManager#setPolicyConfiguration()
     */
    void setPolicyConfiguration(PolicyConfiguration pc) throws PolicyException;

    void reconfigure(byte[] interfaces);
    UserPolicyRule[] getUserRules();
    SystemPolicyRule[] getSystemRules();
}
