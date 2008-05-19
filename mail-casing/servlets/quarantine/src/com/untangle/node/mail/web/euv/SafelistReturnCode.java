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

package com.untangle.node.mail.web.euv;

public class SafelistReturnCode
{
    static final SafelistReturnCode EMPTY = new SafelistReturnCode( 0, 0, new String[0] );
    /* This is the number of entries that were safelisted. */
    private final int safelistCount;

    /* The new number of total records in this inbox */
    private final int totalRecords;

    /* The new safelist */
    private final String[] safelist;

    SafelistReturnCode( int safelistCount, int totalRecords, String[] safelist )
    {
        this.safelistCount = safelistCount;
        this.totalRecords = totalRecords;
        this.safelist = safelist;
    }

    public int getSafelistCount()
    {
        return this.safelistCount;
    }

    public int getTotalRecords()
    {
        return this.totalRecords;
    }

    public String[] getSafelist()
    {
        return this.safelist;
    }
}
