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
    // XXX BEGIN and END are ambiguous, relative to client? inside vs outside?
    public static final Affinity BEGIN = new Affinity("begin");
    public static final Affinity END = new Affinity("end");

    private String affinity;

    private Affinity(String affinity)
    {
        this.affinity = affinity;
    }
}
