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

package com.untangle.node.ips.options;

import java.lang.reflect.*;
import java.util.regex.PatternSyntaxException;

import com.untangle.uvm.tapi.event.*;
import com.untangle.node.ips.IPSDetectionEngine;
import com.untangle.node.ips.IPSRuleSignature;
import com.untangle.node.ips.IPSSessionInfo;
import org.apache.log4j.Logger;

public abstract class IPSOption {
    protected IPSRuleSignature signature;
    protected boolean negationFlag = false;

    private static final Logger log = Logger.getLogger(IPSOption.class);

    protected IPSOption(IPSRuleSignature signature, String params) {

        this.signature = signature;
    }

    // Overriden in concrete children that are runnable
    public boolean runnable() {
        return false;
    }

    // Overriden in concrete children that are runnable
    public boolean run(IPSSessionInfo sessionInfo) {
        return true;
    }

    public static IPSOption buildOption(IPSDetectionEngine engine, IPSRuleSignature signature, String optionName,
                                        String params, boolean initializeSettingsTime) {

        boolean flag = false;
        if(params.charAt(0) == '!')  {
            flag = true;
            params = params.replaceFirst("!","").trim();
        }

        if(params.charAt(0) == '\"' && params.charAt(params.length()-1) == '\"')
            params = params.substring(1,params.length()-1);

        // XXX get rid of this reflection

        IPSOption option = null;
        Class optionDefinition;
        Class[] fourArgsClass = new Class[] { IPSDetectionEngine.class, IPSRuleSignature.class, String.class, Boolean.TYPE };
        Object[] fourOptionArgs = new Object[] { engine, signature, params, initializeSettingsTime };
        Class[] threeArgsClass = new Class[] { IPSRuleSignature.class, String.class, Boolean.TYPE };
        Object[] threeOptionArgs = new Object[] { signature, params, initializeSettingsTime };
        Class[] twoArgsClass = new Class[] { IPSRuleSignature.class, String.class };
        Object[] twoOptionArgs = new Object[] { signature, params };
        Constructor optionConstructor;

        optionName = optionName.toLowerCase();
        char ch = optionName.charAt(0);
        try {
            optionName = optionName.replaceFirst(""+ch,""+(char)(ch - 'a' + 'A'));
        } catch(PatternSyntaxException e) {
            log.error("Bad option name", e);
        }

        try {
            // First look for a three arg one, then the two arg one (since most don't care about
            // initializeSettingsTime).
            optionDefinition = Class.forName("com.untangle.node.ips.options."+optionName+"Option");

            // XXX remove reflection
            try {
                optionConstructor = optionDefinition.getConstructor(fourArgsClass);
                option = (IPSOption) createObject(optionConstructor, fourOptionArgs);
            } catch (NoSuchMethodException exn) {
                try {
                    optionConstructor = optionDefinition.getConstructor(threeArgsClass);
                    option = (IPSOption) createObject(optionConstructor, threeOptionArgs);
                } catch (NoSuchMethodException e) {
                    optionConstructor = optionDefinition.getConstructor(twoArgsClass);
                    option = (IPSOption) createObject(optionConstructor, twoOptionArgs);
                }
            }
            if (option != null)
                option.negationFlag = flag;
        } catch (ClassNotFoundException e) {
            log.info("Could not load option(ClassNotFound): " + optionName + ", ignoring rule: " + signature.rule().getText());
            signature.remove(true);
        } catch (NoSuchMethodException e) {
            log.error("Could not load option(NoSuchMethod): ", e);
        }
        return option;
    }

    private static Object createObject(Constructor constructor, Object[] arguments) {
        Object object = null;
        try {
            object = constructor.newInstance(arguments);
        } catch (InstantiationException e) {
            log.error("Could not create object(InstantiationException): ", e);
        } catch (IllegalAccessException e) {
            log.error("Could not create object(IllegalAccessException): ", e);
        } catch (IllegalArgumentException e) {
            log.error("Could not create object(IllegalArgumentException): ", e);
        } catch (InvocationTargetException e) {
            log.error("Could not create object(InvocationTargetException): ", e.getTargetException());
        }
        return object;
    }
}

