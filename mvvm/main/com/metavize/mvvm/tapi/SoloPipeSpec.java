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

public class SoloPipeSpec extends PipeSpec
{
    public static final int MIN_STRENGTH = 0;
    public static final int MAX_STRENGTH = 32;

    private final Fitting fitting;
    private final Affinity affinity;
    private final int strength;

    // constructors -----------------------------------------------------------

    public SoloPipeSpec(String name, Set subscriptions, Fitting fitting,
                        Affinity affinity, int strength)
    {
        super(name, subscriptions);

        if (strength < MIN_STRENGTH || strength > MAX_STRENGTH) {
            throw new IllegalArgumentException("bad strength: " + strength);
        }

        this.fitting = fitting;
        this.affinity = affinity;
        this.strength = strength;
    }

    public SoloPipeSpec(String name, Subscription subscription,
                        Fitting fitting, Affinity affinity, int strength)
    {
        super(name, subscription);

        if (strength < MIN_STRENGTH || strength > MAX_STRENGTH) {
            throw new IllegalArgumentException("bad strength: " + strength);
        }

        this.fitting = fitting;
        this.affinity = affinity;
        this.strength = strength;
    }

    public SoloPipeSpec(String name, Fitting fitting, Affinity affinity,
                        int strength)
    {
        super(name);

        if (strength < MIN_STRENGTH || strength > MAX_STRENGTH) {
            throw new IllegalArgumentException("bad strength: " + strength);
        }

        this.fitting = fitting;
        this.affinity = affinity;
        this.strength = strength;
    }

    // accessors --------------------------------------------------------------

    public Fitting getFitting()
    {
        return fitting;
    }

    public Affinity getAffinity()
    {
        return affinity;
    }

    public int getStrength()
    {
        return strength;
    }
}
