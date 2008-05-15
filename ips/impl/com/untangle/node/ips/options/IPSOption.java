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

package com.untangle.node.ips.options;

import java.lang.reflect.*;
import java.util.regex.PatternSyntaxException;

import com.untangle.node.ips.IPSDetectionEngine;
import com.untangle.node.ips.IPSRule;
import com.untangle.node.ips.IPSRuleSignatureImpl;
import com.untangle.node.ips.IPSSessionInfo;
import com.untangle.uvm.vnet.event.*;
import org.apache.log4j.Logger;

public abstract class IPSOption
{
    protected final IPSRuleSignatureImpl signature;

    protected final boolean negationFlag;

    private static final Logger log = Logger.getLogger(IPSOption.class);

    protected IPSOption(OptionArg arg)
    {
        this.signature = arg.getSignature();
        this.negationFlag = arg.getNegationFlag();
    }

    // Overriden in concrete children that are runnable
    public boolean runnable()
    {
        return false;
    }

    // Overriden in concrete children that are runnable
    public boolean run(IPSSessionInfo sessionInfo)
    {
        return true;
    }

    public static IPSOption buildOption(IPSDetectionEngine engine,
                                        IPSRuleSignatureImpl signature,
                                        IPSRule rule,
                                        String optionName,
                                        String params,
                                        boolean initializeSettingsTime)
    {
        boolean negationFlag = false;
        if(params.charAt(0) == '!')  {
            negationFlag = true;
            params = params.replaceFirst("!","").trim();
        }

        if(params.charAt(0) == '\"' && params.charAt(params.length()-1) == '\"')
            params = params.substring(1,params.length()-1);

        // XXX get rid of this reflection

        OptionArg oa = new OptionArg(engine, rule, signature, params,
                                     initializeSettingsTime, negationFlag);

        IPSOption option = null;

        optionName = optionName.toLowerCase();
        char ch = optionName.charAt(0);
        try {
            optionName = optionName.replaceFirst("" + ch, "" + (char)(ch - 'a' + 'A'));
        } catch(PatternSyntaxException e) {
            log.error("Bad option name", e);
        }

        try {
            // First look for a three arg one, then the two arg one
            // (since most don't care about initializeSettingsTime).
            Class clazz = Class
                .forName("com.untangle.node.ips.options."+optionName+"Option");
            Constructor constructor = clazz
                .getConstructor(new Class[] { IPSOption.class });
            option = (IPSOption)constructor.newInstance(new Object[] { oa });
        } catch (ClassNotFoundException e) {
            log.info("Could not load option: " + optionName + ", ignoring rule: " + signature.getSid());
            signature.remove(true);
        } catch (NoSuchMethodException e) {
            log.warn("Could not load option", e);
        } catch (InstantiationException e) {
            log.warn("Could not load option", e);
        } catch (IllegalAccessException e) {
            log.warn("Could not load option", e);
        } catch (InvocationTargetException e) {
            log.warn("Could not load option", e);
        }

        return option;
    }

    public boolean optEquals(IPSOption o)
    {
        return negationFlag == o.negationFlag;
    }

    public int optHashCode()
    {
        return 17 * 37 + (negationFlag ? 1 : 0);
    }
}

