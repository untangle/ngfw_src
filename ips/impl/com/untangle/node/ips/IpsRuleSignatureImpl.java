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

package com.untangle.node.ips;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.apache.log4j.Logger;

import com.untangle.node.ips.options.IpsOption;
import com.untangle.uvm.vnet.IPSession;

public class IpsRuleSignatureImpl
{
    /***************************************
     * These are options that are safe to ignore
     * Any other option *WILL DROP THE RULE*
     *
     * These rules should all be added at some point!
     *****************************************/
    private static final String[] IGNORE_SAFE_OPTIONS = { "rev", "priority" };
    /** **************************************/

    private static final Map<Integer, long[]> ruleTimes
        = new HashMap<Integer, long[]>();

    private final int sid;
    private final int action;
    private final String string;

    private final List<IpsOption> options = new ArrayList<IpsOption>();

    private String message = null;
    private String classification = null;
    private String url = null;
    private boolean removeFlag = false;

    private static final Logger log = Logger.getLogger(IpsRuleSignature.class);

    public IpsRuleSignatureImpl(IpsNodeImpl ips, IpsRule rule,
                                String signatureString, int action,
                                boolean initSettingsTime, String string)
    {
        this.sid = rule.getSid();
        this.action = action;
        this.string = null == string ? "Starting.." : string;

        String replaceChar = ""+0xff42;
        signatureString = signatureString.replaceAll("\\\\;",replaceChar);
        String options[] = signatureString.trim().split(";");
        for (int i = 0; i < options.length; i++) {
            options[i].trim();
            options[i] = options[i].replaceAll(replaceChar,"\\\\;");
            int delim = options[i].indexOf(':');
            if (delim < 0) {
                addOption(ips.getEngine(), rule, options[i].trim(), "No Params",
                          initSettingsTime);
            } else {
                String opt = options[i].substring(0,delim).trim();
                addOption(ips.getEngine(), rule, opt,
                          options[i].substring(delim+1).trim(),
                          initSettingsTime);
            }

            if (remove()) {
                // Early exit.  Don't bother with rest of options.
                break;
            }
        }
    }

    public void remove(boolean remove)
    {
        removeFlag = remove;
    }

    public boolean remove()
    {
        return removeFlag;
    }

    public int getSid()
    {
        return sid;
    }

    public IpsOption getOption(String name, IpsOption callingOption)
    {
        String[] parents = new String[] { name };
        return getOption(parents, callingOption);
    }

    @SuppressWarnings("unchecked")
	public IpsOption getOption(String[] names, IpsOption callingOption)
    {
        Class[] optionDefinitions = new Class[names.length];
        for (int i = 0; i < names.length; i++) {
            try {
                optionDefinitions[i] = Class.forName("com.untangle.node.ips.options."+names[i]);
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
        ListIterator<IpsOption> it = options.listIterator(index);

        while(it.hasPrevious()) {
            IpsOption option = it.previous();
            for (int i = 0; i < optionDefinitions.length; i++) {
                if (optionDefinitions[i] != null && optionDefinitions[i].isInstance(option))
                    return option;
            }
        }
        return null;
    }

    public void setMessage(String msg)
    {
        message = msg;
    }

    public String getMessage()
    {
        return message;
    }

    public void setClassification(String classification)
    {
        this.classification = classification;
    }

    public String getClassification()
    {
        return classification;
    }

    public void setURL(String url)
    {
        this.url = url;
        return;
    }

    public String getURL()
    {
        return url;
    }

    public boolean execute(IpsNodeImpl ips, IpsSessionInfo info)
    {
        boolean result = true;
        long startTime = 0;
        if (IpsDetectionEngine.DO_PROFILING)
            startTime = System.nanoTime();
        for (IpsOption option : options) {
            boolean opres = option.run(info);
            // if (log.isDebugEnabled())
            // log.debug("res: " + opres + ", rule " + rule.getSid() + " option " + option.getClass().getName());
            if (!opres) {
                // do not match
                result = false;
                break;
            }
        }

        if (IpsDetectionEngine.DO_PROFILING) {
            // Throw away last three digits as they are always zero on linux.
            long elapsed = (System.nanoTime() - startTime) / 1000l;
            synchronized(ruleTimes) {
                long[] existingCountAndTime = ruleTimes.get(sid);
                if (existingCountAndTime == null) {
                    existingCountAndTime = new long[2];
                    existingCountAndTime[0] = 1;
                    existingCountAndTime[1] = elapsed;
                    ruleTimes.put(sid, existingCountAndTime);
                } else {
                    existingCountAndTime[0]++;
                    existingCountAndTime[1] += elapsed;
                }
            }
        }

        if (result)
            doAction(ips, info); // match
        return result;
    }

    private void doAction(IpsNodeImpl ips, IpsSessionInfo info)
    {
        IPSession session = info.getSession();
        if (null == session) {
            log.error("Session is null; cannot act on event: " + classification + ", " + message);
            return;
        }

        // XXX this is not a good way to get a reference to the node
        IpsDetectionEngine engine = ips.getEngine();

        boolean blocked = false;
        switch(action) {
        case IpsRule.ALERT:
            // Can't happen right now.
            log.warn("Alert: "+classification + ", " + message);
            ips.statisticManager.incrLogged();
            engine.incrementDetectCount();
            break;

        case IpsRule.LOG:
            log.debug("Log: "+classification + ", " + message);
            ips.statisticManager.incrLogged();
            engine.incrementDetectCount();
            break;

        case IpsRule.BLOCK:
            log.info("Block: "+classification + ", " + message);
            blocked = true;
            ips.statisticManager.incrBlocked();
            engine.incrementBlockCount();
            info.blockSession();
            break;
        }

        //Add list number that this rule came from
        ips.log(new IpsLogEvent(session.pipelineEndpoints(), sid,
                                classification, message, blocked));
    }

    public String toString()
    {
        return string;
    }

    // package protected static methods ----------------------------------------

    static void dumpRuleTimes()
    {
        if (IpsDetectionEngine.DO_PROFILING) {
            StringBuilder sb = new StringBuilder(String.format("\n%10s %12s %10s\n",
                                                               "Count", "Micros", "Rule"));
            synchronized(ruleTimes) {
                for (Integer sid : ruleTimes.keySet()) {
                    long[] countAndTime = ruleTimes.get(sid);
                    sb.append(String.format("%10d %12d %10d\n",
                                            countAndTime[0], countAndTime[1], sid));
                }

                ruleTimes.clear();
            }
            log.warn(sb.toString());

        }
    }

    // private methods ---------------------------------------------------------

    private void addOption(IpsDetectionEngine engine, IpsRule rule,
                           String optionName, String params,
                           boolean initializeSettingsTime)
    {
        for (int i = 0; i < IGNORE_SAFE_OPTIONS.length; i++) {
            if (optionName.equalsIgnoreCase(IGNORE_SAFE_OPTIONS[i])) {
                return;
            }
        }
        IpsOption option = IpsOption
            .buildOption(engine, this, rule, optionName, params,
                         initializeSettingsTime);
        if (option != null && option.runnable()) {
            options.add(option);
        } else if (option == null) {
            log.info("Could not add option: " + optionName);
            removeFlag = true;
        }
    }

    // Object methods ----------------------------------------------------------

    public boolean equals(Object o)
    {
        if (!(o instanceof IpsRuleSignatureImpl)) {
            return false;
        }

        IpsRuleSignatureImpl irs = (IpsRuleSignatureImpl)o;

        if (sid != irs.sid || action != irs.action
            || removeFlag != irs.removeFlag) {
            return false;
        }

        if (options.size() != irs.options.size()) {
            return false;
        }
        Iterator<IpsOption> i = options.iterator();
        Iterator<IpsOption> j = irs.options.iterator();
        while (i.hasNext() && j.hasNext()) {
            if (!i.next().optEquals(j.next())) {
                return false;
            }
        }
        if (i.hasNext() || j.hasNext()) {
            return false;
        }

        if (null == string || null == irs.string) {
            if (string != irs.string) {
                return false;
            }
        } else {
            if (!string.equals(irs.string)) {
                return false;
            }
        }

        if (null == message || null == irs.message) {
            if (message != irs.message) {
                return false;
            }
        } else {
            if (!message.equals(irs.message)) {
                return false;
            }
        }

        if (null == classification || null == irs.classification) {
            if (classification != irs.classification) {
                return false;
            }
        } else {
            if (!classification.equals(irs.classification)) {
                return false;
            }
        }

        if (null == url || null == irs.url) {
            if (url != irs.url) {
                return false;
            }
        } else {
            if (!url.equals(irs.url)) {
                return false;
            }
        }

        return true;
    }

    public int hashCode()
    {
        int result = 17;
        for (IpsOption o : options) {
            result = 37 * result + o.optHashCode();
        }

        result = result * 37 + sid;
        result = result * 37 + action;
        result = result * 37 + (null == string ? 0 : string.hashCode());
        result = result * 37 + (null == message ? 0 : message.hashCode());
        result = result * 37 + (null == classification ? 0 : classification.hashCode());
        result = result * 37 + (null == url ? 0 : url.hashCode());
        result = result * 37 + (removeFlag ? 1 : 0);
        return result;
    }
}
