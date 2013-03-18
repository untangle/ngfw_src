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

import com.untangle.node.ips.IpsSessionInfo;
import com.untangle.uvm.vnet.event.IPDataEvent;

public class DsizeOption extends IpsOption
{
    private final Logger log = Logger.getLogger(getClass());

    private int min;
    private int max;

    public DsizeOption(OptionArg arg)
    {
        super(arg);

        String params = arg.getParams();

        char ch = params.charAt(0);
        String range[] = params.split("<>");
        try {
            if(range.length == 2) {
                min = Integer.parseInt(range[0].trim());
                max = Integer.parseInt(range[1].trim());
            }
            else if(ch == '<') {
                min = 0;
                max = Integer.parseInt(params.substring(1).trim());
            }
            else if(ch == '>') {
                min = Integer.parseInt(params.substring(1).trim());
                max = Integer.MAX_VALUE;
            }
            else
                min = max = Integer.parseInt(params.trim());
        }
        catch(NumberFormatException e) {
            log.error("Invalid Dsize param: " + params);
            min = 0;
            max = Integer.MAX_VALUE;
        }
    }

    public boolean runnable()
    {
        return true;
    }

    //XXX - check negation flag?
    public boolean run(IpsSessionInfo sessionInfo)
    {
        IPDataEvent event = sessionInfo.getEvent();
        int size = event.data().remaining();
        if(min <= size && max >= size)
            return true;
        return false;
    }

    public boolean optEquals(Object o)
    {
        if (!(o instanceof DsizeOption)) {
            return false;
        }

        DsizeOption dso = (DsizeOption)o;

        if (!super.optEquals(dso)) {
            return false;
        }

        return min == dso.min
            && max == dso.max;
    }

    public int optHashCode()
    {
        int result = 17;
        result = result * 37 + super.optHashCode();
        result = result * 37 + min;
        result = result * 37 + max;
        return result;
    }
}
