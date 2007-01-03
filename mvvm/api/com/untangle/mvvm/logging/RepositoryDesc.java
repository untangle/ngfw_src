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

package com.untangle.mvvm.logging;

import java.io.Serializable;

public class RepositoryDesc implements Serializable
{
    private final String name;

    public RepositoryDesc(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }
}
