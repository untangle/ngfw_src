/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.logging;

import java.io.Serializable;

public class FilterDesc implements Serializable
{
    private final String name;

    public FilterDesc(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }
}
