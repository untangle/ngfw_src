/*
 * $HeadURL: svn://chef/branch/prod/web-ui/work/src/ips/impl/com/untangle/node/ips/options/IpsOption.java $
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

import com.untangle.node.ips.IpsDetectionEngine;
import com.untangle.node.ips.IpsRule;
import com.untangle.node.ips.IpsRuleSignatureImpl;

public class OptionArg
{
    private final IpsDetectionEngine engine;
    private final IpsRule rule;
    private final IpsRuleSignatureImpl signature;
    private final String params;
    private final boolean initializeSettingsTime;
    private final boolean negationFlag;

    public OptionArg(IpsDetectionEngine engine, IpsRule rule,
                     IpsRuleSignatureImpl signature, String params,
                     boolean initializeSettingsTime, boolean negationFlag)
    {
        this.engine = engine;
        this.rule = rule;
        this.signature = signature;
        this.params = params;
        this.initializeSettingsTime = initializeSettingsTime;
        this.negationFlag = negationFlag;
    }

    public IpsDetectionEngine getEngine()
    {
        return engine;
    }

    public IpsRule getRule()
    {
        return rule;
    }

    public IpsRuleSignatureImpl getSignature()
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