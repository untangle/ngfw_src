/**
 * $Id$
 */
package com.untangle.node.ips.options;

import org.apache.log4j.Logger;

import com.untangle.node.ips.IpsRule;

public class SidOption extends IpsOption
{
    private final Logger logger = Logger.getLogger(getClass());

    public SidOption(OptionArg arg)
    {
        super(arg);

        if (arg.getInitializeSettingsTime()) {
            String params = arg.getParams();

            int sid = -1;
            try {
                sid = Integer.parseInt(params);
            } catch (NumberFormatException x) {
                logger.warn("Unable to parse sid: " + params);
            }

            IpsRule rule = arg.getRule();
            rule.setSid(sid);
        }
    }
}
