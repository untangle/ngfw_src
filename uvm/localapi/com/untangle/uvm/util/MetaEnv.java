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

package com.untangle.uvm.util;

import java.util.Random;

public class MetaEnv {

    private static Random rng;

    public static long currentTimeMillis()
    {   
        // if in testing mode, return fake time, else:
        return System.currentTimeMillis();
    }

    public static void initRNG(long seed)
    {
        rng = new Random(seed);
    }

    public static void initRNG()
    {
        rng = new Random();
    }

    public static Random rng()
    {
        // Could lock here, but it's one-time kinda stuff so not very unsafe. XX
        if (rng == null) {
            initRNG();
        }
        return rng;
    }
}
