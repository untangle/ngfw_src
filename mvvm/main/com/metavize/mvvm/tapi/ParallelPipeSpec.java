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

package com.metavize.mvvm.tapi;

import java.util.Set;

public class ParallelPipeSpec extends PipeSpec
{
    private final int parallelSid;
    private final MPipe parallelMPipe;

    // constructors -----------------------------------------------------------

    public ParallelPipeSpec(String name, Set subscriptions, int parallelSid,
                            MPipe parallelMPipe)
    {
        super(name, subscriptions);
        this.parallelSid = parallelSid;
        this.parallelMPipe = parallelMPipe;
    }

    // accessors --------------------------------------------------------------

    public int getParallelSid()
    {
        return parallelSid;
    }

    public MPipe getParallelMPipe()
    {
        return parallelMPipe;
    }
}
