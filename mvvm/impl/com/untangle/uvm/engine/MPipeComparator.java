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

package com.untangle.mvvm.engine;

import java.util.Comparator;

import com.untangle.mvvm.tapi.Affinity;
import com.untangle.mvvm.tapi.MPipe;
import com.untangle.mvvm.tapi.SoloPipeSpec;

/**
 * Compares MPipes with SoloPipeSpecs.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
class MPipeComparator implements Comparator
{
    private final boolean incoming;

    MPipeComparator(boolean incoming)
    {
        this.incoming = incoming;
    }

    public int compare(Object o1, Object o2)
    {
        MPipe mp1 = (MPipe)o1;
        MPipe mp2 = (MPipe)o2;

        SoloPipeSpec ps1 = null == mp1 ? null : (SoloPipeSpec)mp1.getPipeSpec();
        SoloPipeSpec ps2 = null == mp2 ? null : (SoloPipeSpec)mp2.getPipeSpec();

        Affinity ra1 = relativeAffinity(ps1);
        Affinity ra2 = relativeAffinity(ps2);

        if (null == ra1) {
            if (null == ra2) {
                return 0;
            } else {
                if (Affinity.CLIENT == ra2) {
                    return 1;
                } else if (Affinity.SERVER == ra2) {
                    return -1;
                } else {
                    throw new RuntimeException("programmer malfunction");
                }
            }
        } else if (null == ra2) {
            if (Affinity.CLIENT == ra1) {
                return -1;
            } else if (Affinity.SERVER == ra2) {
                return 1;
            } else {
                throw new RuntimeException("programmer malfunction");
            }
        } else if (ra1 == ra2) {
            int s1 = ps1.getStrength();
            int s2 = ps2.getStrength();

            if (s1 == s2) {
                if (mp1 == mp2) {
                    return 0;
                } else {
                    int mp1Id = System.identityHashCode(mp1);
                    int mp2Id = System.identityHashCode(mp2);
                    return mp1Id < mp2Id ? -1 : 1;
                }
            } else if (ra1 == Affinity.CLIENT) {
                return s1 < s2 ? 1 : -1;
            } else if (ra1 == Affinity.SERVER) {
                return s1 < s2 ? -1 : 1;
            } else {
                throw new RuntimeException("programmer malfunction");
            }
        } else if (Affinity.CLIENT == ra1) {
            return -1;
        } else if (Affinity.SERVER == ra1) {
            return 1;
        } else {
            throw new RuntimeException("programmer malfunction");
        }
    }

    public boolean equals(Object o)
    {
        if (!(o instanceof MPipeComparator)) {
            return false;
        } else {
            MPipeComparator sc = (MPipeComparator)o;
            return incoming == sc.incoming;
        }
    }

    private Affinity relativeAffinity(SoloPipeSpec ps)
    {
        if (null == ps) {
            return null;
        }

        Affinity a = ps.getAffinity();

        if (Affinity.CLIENT == a || Affinity.SERVER == a) {
            return a;
        } else {
            if (incoming) {
                if (Affinity.INSIDE == a) {
                    return Affinity.SERVER;
                } else if (Affinity.OUTSIDE == a) {
                    return Affinity.CLIENT;
                } else {
                    throw new IllegalArgumentException("unknown: " + a);
                }
            } else {
                if (Affinity.INSIDE == a) {
                    return Affinity.CLIENT;
                } else if (Affinity.OUTSIDE == a) {
                    return Affinity.SERVER;
                } else {
                    throw new IllegalArgumentException("unknown: " + a);
                }
            }
        }
    }
}
