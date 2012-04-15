/*
 * $HeadURL: svn://chef/branch/prod/web-ui/work/src/ips/api/com/untangle/node/ips/IpsSettings.java $
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

import java.io.Serializable;

/**
 * Statistics for the Ips node.
 *
 * @author <a href="mailto:mahotz@untangle.com">Michael Hotz</a>
 * @version 1.0
 */
@SuppressWarnings("serial")
public class IpsStatistics implements Serializable
{
    private int rulesLength;
    private int variablesLength;
    private int immutableVariablesLength;

    private int totalAvailable;
    private int totalBlocking;
    private int totalLogging;

    public IpsStatistics() { }

    public int getRulesLength()
    {
        return this.rulesLength;
    }

    public void setRulesLength(int rulesLength)
    {
        this.rulesLength = rulesLength;
    }

    public int getVariablesLength()
    {
        return this.variablesLength;
    }

    public void setVariablesLength(int variablesLength)
    {
        this.variablesLength = variablesLength;
    }

    public int getImmutableVariablesLength()
    {
        return this.immutableVariablesLength;
    }

    public void setImmutableVariablesLength(int immutableVariablesLength)
    {
        this.immutableVariablesLength = immutableVariablesLength;
    }

    public int getTotalAvailable()
    {
        return this.totalAvailable;
    }

    public void setTotalAvailable(int totalAvailable)
    {
        this.totalAvailable = totalAvailable;
    }

    public int getTotalBlocking()
    {
        return this.totalBlocking;
    }

    public void setTotalBlocking(int totalBlocking)
    {
        this.totalBlocking = totalBlocking;
    }

    public int getTotalLogging()
    {
        return this.totalLogging;
    }

    public void setTotalLogging(int totalLogging)
    {
        this.totalLogging = totalLogging;
    }
}
