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
}
