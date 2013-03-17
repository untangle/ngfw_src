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

public class MsgOption extends IpsOption
{
    private static final String BLEEDING_PREFIX = "BLEEDING-EDGE";

    public MsgOption(OptionArg arg)
    {
        super(arg);

        String params = arg.getParams();

        // reomve useless 'BLEEDING-EDGE' prefix
        if (params.length() > BLEEDING_PREFIX.length()) {
            String beginParams = params.substring(0, BLEEDING_PREFIX.length());
            if (beginParams.equalsIgnoreCase(BLEEDING_PREFIX))
                params = params.substring(BLEEDING_PREFIX.length()).trim();
        }
        signature.setMessage(params);
    }
}
