/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: Affinity.java,v 1.1 2005/01/30 09:20:31 amread Exp $
 */

package com.metavize.mvvm.tapi;

// XXX make this an enum when i dump xdoclet
public class Affinity
{
    public static final Affinity CLIENT = new Affinity("client");
    public static final Affinity SERVER = new Affinity("server");

    public static final Affinity INSIDE = new Affinity("inside");
    public static final Affinity OUTSIDE = new Affinity("outside");

    private String affinity;

    private Affinity(String affinity)
    {
        this.affinity = affinity;
    }
}
