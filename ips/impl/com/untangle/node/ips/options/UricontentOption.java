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

import com.sun.org.apache.xerces.internal.impl.xpath.regex.BMPattern;
import com.untangle.node.ips.IpsSessionInfo;

public class UricontentOption extends IpsOption
{
    private BMPattern uriPattern;
    private String stringPattern;
    private boolean nocase = false;

    public UricontentOption(OptionArg arg)
    {
        super(arg);

        stringPattern = arg.getParams();
        uriPattern = new BMPattern(stringPattern, nocase);
    }

    public void setNoCase()
    {
        nocase = true;
        uriPattern = new BMPattern(stringPattern, nocase);
    }

    public boolean runnable()
    {
        return true;
    }

    public boolean run(IpsSessionInfo sessionInfo)
    {
        String path = sessionInfo.getUriPath();
        if(path != null) {
            int result = uriPattern.matches(path, 0, path.length());
            return negationFlag ^ (result >= 0);
        }
        return false;
    }

    public boolean optEquals(Object o)
    {
        if (!(o instanceof UricontentOption)) {
            return false;
        }

        UricontentOption uo = (UricontentOption)o;

        if (!super.optEquals(uo)) {
            return false;
        }

        return stringPattern.equals(uo.stringPattern)
            && nocase == nocase;
    }

    public int optHashCode()
    {
        int result = 17;
        result = result * 37 + super.optHashCode();
        result = result * 37 + stringPattern.hashCode();
        result = result * 37 + (nocase ? 1 : 0);
        return result;
    }
}
