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

import org.apache.log4j.Logger;

import com.untangle.node.ips.IpsDetectionEngine;
import com.untangle.node.ips.IpsRule;
import com.untangle.node.ips.RuleClassification;

class ClasstypeOption extends IpsOption
{
    static final int HIGH_PRIORITY = 1;
    static final int MEDIUM_PRIORITY = 2;
    static final int LOW_PRIORITY = 3;
    static final int INFORMATIONAL_PRIORITY = 4; // Super low priority

    private final Logger logger = Logger.getLogger(getClass());

    public ClasstypeOption(OptionArg arg)
    {
        super(arg);

        IpsDetectionEngine engine = arg.getEngine();
        String params = arg.getParams();

        RuleClassification rc = null;
        if (engine != null)
            // Allow null for testing.
            rc = engine.getClassification(params);
        if (rc == null) {
            logger.warn("Unable to find rule classification: " + params);
            // use default classification text for signature
        } else {
            signature.setClassification(rc.getDescription());

            if (true == arg.getInitializeSettingsTime()) {
                IpsRule rule = arg.getRule();
                int priority = rc.getPriority();
                // logger.debug("Rule Priority for " + rule.getDescription() + " is " + priority);
                switch (priority) {
                case HIGH_PRIORITY:
                    rule.setLive(true);
                    rule.setLog(true);
                    break;
                case MEDIUM_PRIORITY:
                    rule.setLog(true);
                    break;
                default:
                    break;
                }
            }
        }
    }
}
