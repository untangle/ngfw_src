/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.node.http;

public enum UserWhitelistMode
{
    NONE("None"),
    USER_ONLY("User Only"),
    USER_AND_GLOBAL("User and Global");

    private final String string;

    private UserWhitelistMode(String string)
    {
        this.string = string;
    }

    public String toString()
    {
        return string;
    }
}
