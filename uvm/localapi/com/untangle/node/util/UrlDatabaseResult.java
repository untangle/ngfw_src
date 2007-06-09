/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id: SpywareHttpHandler.java 8668 2007-01-29 19:17:09Z amread $
 */

package com.untangle.node.util;

public class UrlDatabaseResult<T>
{
    private final boolean black;
    private final T o;

    UrlDatabaseResult(boolean black, T o)
    {
        this.black = black;
        this.o = o;
    }

    public boolean blacklisted()
    {
        return black;
    }

    public boolean whitelisted()
    {
        return !black;
    }
}
