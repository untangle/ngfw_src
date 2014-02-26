/**
 * $Id$
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