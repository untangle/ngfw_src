/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: AbstractTransform.java,v 1.1 2005/01/30 09:20:31 amread Exp $
 */

package com.metavize.mvvm.tapi;

import com.metavize.mvvm.engine.TransformBase;

public abstract class AbstractTransform extends TransformBase
{
    // XXX temporary hack, remove someday !!!
    private final long[] counters = new long[16];

    // constructors -----------------------------------------------------------

    protected AbstractTransform() { }

    // public methods ---------------------------------------------------------

    public long getCount(int i)
    {
        synchronized (counters) {
            return counters[i];
        }
    }

    public long incrementCount(int i)
    {
        return incrementCount(i, 1);
    }

    public long incrementCount(int i, long delta)
    {
        synchronized (counters) {
            return counters[i] += delta;
        }
    }
}
