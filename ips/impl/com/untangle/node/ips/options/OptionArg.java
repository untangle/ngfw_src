/*
 * $HeadURL: svn://chef/branch/prod/web-ui/work/src/ips/impl/com/untangle/node/ips/options/IPSOption.java $
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

package com.untangle.node.ips.options;

import com.untangle.node.ips.IPSDetectionEngine;
import com.untangle.node.ips.IPSRule;
import com.untangle.node.ips.IPSRuleSignatureImpl;

public class OptionArg
{
    private final IPSDetectionEngine engine;
    private final IPSRule rule;
    private final IPSRuleSignatureImpl signature;
    private final String params;
    private final boolean initializeSettingsTime;
    private final boolean negationFlag;

    public OptionArg(IPSDetectionEngine engine, IPSRule rule,
                     IPSRuleSignatureImpl signature, String params,
                     boolean initializeSettingsTime, boolean negationFlag)
    {
        this.engine = engine;
        this.rule = rule;
        this.signature = signature;
        this.params = params;
        this.initializeSettingsTime = initializeSettingsTime;
        this.negationFlag = negationFlag;
    }

    public IPSDetectionEngine getEngine()
    {
        return engine;
    }

    public IPSRule getRule()
    {
        return rule;
    }

    public IPSRuleSignatureImpl getSignature()
    {
        return signature;
    }

    public String getParams()
    {
        return params;
    }

    public boolean getInitializeSettingsTime()
    {
        return initializeSettingsTime;
    }

    public boolean getNegationFlag()
    {
        return negationFlag;
    }
}