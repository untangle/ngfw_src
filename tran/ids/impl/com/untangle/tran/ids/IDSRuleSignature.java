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

package com.untangle.tran.ids;

import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;

import com.untangle.mvvm.tapi.IPSession;
import com.untangle.mvvm.tapi.event.*;
import com.untangle.mvvm.tran.Transform;
import com.untangle.tran.ids.options.*;
import org.apache.log4j.Logger;

public class IDSRuleSignature {

    /***************************************
     * These are options that are safe to ignore
     * Any other option *WILL DROP THE RULE*
     *
     * These rules should all be added at some point!
     *****************************************/
    private String[] ignoreSafeOptions = { "rev","priority" };
    /** **************************************/

    private static final int DETECT_COUNTER   = Transform.GENERIC_1_COUNTER;
    private static final int BLOCK_COUNTER    = Transform.GENERIC_2_COUNTER;

    private static HashMap<IDSRule,long[]> ruleTimes = new HashMap<IDSRule,long[]>();

    private final IDSTransformImpl ids;
    private final int action;
    private final IDSRule rule;

    // XXX Vector
    private final List<IDSOption> options = new Vector<IDSOption>();

    private String toString = "Starting..";
    private String message = "No message set";
    private String classification = "Rule is not classified";
    private String url = "Rule is not documented";
    private boolean removeFlag = false;

    private static final Logger log = Logger.getLogger(IDSRuleSignature.class);

    public IDSRuleSignature(IDSTransformImpl ids, int action, IDSRule rule) {
        this.ids = ids;
        this.action = action;
        this.rule = rule;
    }

    public void remove(boolean remove) {
        removeFlag = remove;
    }

    public boolean remove() {
        return removeFlag;
    }

    public IDSRule rule() {
        return rule;
    }

    public void addOption(String optionName, String params, boolean initializeSettingsTime) {
        for(int i = 0; i < ignoreSafeOptions.length; i++) {
            if(optionName.equalsIgnoreCase(ignoreSafeOptions[i]))
                return;
        }

        IDSOption option = IDSOption.buildOption(ids.getEngine(),this,optionName,params, initializeSettingsTime);
        if(option != null && option.runnable())
            options.add(option);
        else if(option == null) {
            log.info("Could not add option: " + optionName);
            removeFlag = true;
        }
    }

    public IDSOption getOption(String name, IDSOption callingOption) {
        String[] parents = new String[] { name };
        return getOption(parents, callingOption);
    }

    public IDSOption getOption(String[] names, IDSOption callingOption) {
        Class[] optionDefinitions = new Class[names.length];
        for (int i = 0; i < names.length; i++) {
            try {
                optionDefinitions[i] = Class.forName("com.untangle.tran.ids.options."+names[i]);
            } catch (ClassNotFoundException e) {
                log.error("Could not load option: " + e.getMessage());
                optionDefinitions[i] = null;
            }
        }

        /**Have to iterate backwards over the options so that options that
         * act as modifiers will modify the correct option
         * eg, in situations where there are multiple content options.
         */
        int index = options.indexOf(callingOption);
        index = (index < 0) ? options.size():index;
        ListIterator<IDSOption> it = options.listIterator(index);

        while(it.hasPrevious()) {
            IDSOption option = it.previous();
            for (int i = 0; i < optionDefinitions.length; i++) {
                if (optionDefinitions[i] != null && optionDefinitions[i].isInstance(option))
                    return option;
            }
        }
        return null;
    }

    public void setMessage(String msg) {
        message = msg;
    }

    public String getMessage() {
        return message;
    }

    public void setClassification(String classification) {
        this.classification = classification;
        return;
    }

    public String getClassification() {
        return classification;
    }

    public void setURL(String url) {
        this.url = url;
        return;
    }

    public String getURL() {
        return url;
    }

    public boolean execute(IDSSessionInfo info) {
        boolean result = true;
        long startTime = 0;
        if (IDSDetectionEngine.DO_PROFILING)
            startTime = System.nanoTime();
        for(IDSOption option : options) {
            if(false == option.run(info)) {
                // do not match
                result = false;
                break;
            }
        }

        if (IDSDetectionEngine.DO_PROFILING) {
            // Throw away last three digits as they are always zero on linux.
            long elapsed = (System.nanoTime() - startTime) / 1000l;
            synchronized(ruleTimes) {
                long[] existingCountAndTime = ruleTimes.get(rule);
                if (existingCountAndTime == null) {
                    existingCountAndTime = new long[2];
                    existingCountAndTime[0] = 1;
                    existingCountAndTime[1] = elapsed;
                    ruleTimes.put(rule, existingCountAndTime);
                } else {
                    existingCountAndTime[0]++;
                    existingCountAndTime[1] += elapsed;
                }
            }
        }

        if (result)
            doAction(info); // match
        return result;
    }

    private void doAction(IDSSessionInfo info) {
        IPSession session = info.getSession();
        if (null == session) {
            log.error("Session is null; cannot act on event: " + classification + ", " + message);
            return;
        }

        // XXX this is not a good way to get a reference to the transform
        IDSDetectionEngine engine = ids.getEngine();

        boolean blocked = false;
        switch(action) {
        case IDSRule.ALERT:
            // Can't happen right now.
            log.warn("Alert: "+classification + ", " + message);
            ids.statisticManager.incrLogged();
            engine.updateUICount(DETECT_COUNTER);
            break;

        case IDSRule.LOG:
            log.debug("Log: "+classification + ", " + message);
            ids.statisticManager.incrLogged();
            engine.updateUICount(DETECT_COUNTER);
            break;

        case IDSRule.BLOCK:
            log.info("Block: "+classification + ", " + message);
            blocked = true;
            ids.statisticManager.incrBlocked();
            engine.updateUICount(BLOCK_COUNTER);
            info.blockSession();
            break;
        }

        ids.log(new IDSLogEvent(session.pipelineEndpoints(), rule.getSid(), classification, message, blocked)); //Add list number that this rule came from
    }

    public void setToString(String string) {
        toString = string;
    }

    public String toString() {
        return toString;
    }

    static void dumpRuleTimes() {
        if (IDSDetectionEngine.DO_PROFILING) {
            StringBuilder sb = new StringBuilder(String.format("\n%10s %12s %10s\n",
                                                               "Count", "Micros", "Rule"));
            synchronized(ruleTimes) {
                for (IDSRule rule : ruleTimes.keySet()) {
                    long[] countAndTime = ruleTimes.get(rule);
                    sb.append(String.format("%10d %12d %10d\n",
                                            countAndTime[0], countAndTime[1], rule.getSid()));
                }
                ruleTimes.clear();
            }
            log.warn(sb.toString());
        }
    }
}
