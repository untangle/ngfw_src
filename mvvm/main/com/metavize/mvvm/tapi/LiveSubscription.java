/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: LiveSubscription.java,v 1.3 2005/01/30 09:20:31 amread Exp $
 */

package com.metavize.mvvm.tapi;

public class LiveSubscription
{
    private int id;

    private Subscription desc;

    // This shouldn't be public, but it doesn't hurt that much. XX
    public LiveSubscription(int id, Subscription desc)
    {
        this.id = id;
        this.desc = desc;
    }

    public int id() { return id; }

    public Subscription desc() { return desc; }
}
