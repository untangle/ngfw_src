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
