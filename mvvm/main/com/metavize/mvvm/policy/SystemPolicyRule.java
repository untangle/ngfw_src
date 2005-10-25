/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.policy;

import java.util.*;

import com.metavize.mvvm.argon.IPSessionDesc;

/**
 * System Policy Rules.  These are the "fallback" matchers in the
 * policy selector, rows are created automatically by the system when
 * interfaces are added and cannot be added or deleted by the user.
 *
 * @author
 * @version 1.0
 * @hibernate.class
 * table="SYSTEM_POLICY_RULE"
 */
public class SystemPolicyRule extends PolicyRule
{
    // constructors -----------------------------------------------------------

    SystemPolicyRule() { }

    public SystemPolicyRule(byte clientIntf, byte serverIntf, Policy policy,
                            boolean inbound) {
        super(clientIntf, serverIntf, policy, inbound);
    }

    public boolean isSameRow(SystemPolicyRule pr)
    {
        return getId().equals(pr.getId());
    }

    // PolicyRule methods -----------------------------------------------------

    public boolean matches(IPSessionDesc sd)
    {
        return clientIntf == sd.clientIntf()
            && serverIntf == sd.serverIntf();
    }

    // Object methods ---------------------------------------------------------

    public boolean equals(Object o)
    {
        if (!(o instanceof SystemPolicyRule)) {
            return false;
        } else {
            SystemPolicyRule pr = (SystemPolicyRule)o;
            return ((policy == null ? pr.policy == null : policy.equals(pr.policy)) &&
                    clientIntf == pr.clientIntf &&
                    serverIntf == pr.serverIntf &&
                    inbound == pr.inbound);
        }
    }

    public int hashCode()
    {
        return (null == policy ? 0 : policy.hashCode()) + clientIntf * 7 + serverIntf * 5;
    }
}
