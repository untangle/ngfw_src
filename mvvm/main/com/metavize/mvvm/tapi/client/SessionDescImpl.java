/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

package com.metavize.mvvm.tapi.client;

import java.io.Serializable;

import com.metavize.mvvm.tapi.SessionDesc;
import com.metavize.mvvm.tapi.SessionStats;

abstract class SessionDescImpl implements SessionDesc, Serializable {
    private static final long serialVersionUID = 2962776047684793850L;

    protected int id;

    protected SessionStats stats;

    protected SessionDescImpl(int id, SessionStats stats)
    {
        this.id = id;
        this.stats = stats;
    }

    public int id()
    {
        return id;
    }

    public SessionStats stats()
    {
        return stats;
    }
}
