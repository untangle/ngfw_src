/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.uvm.engine;

import java.util.Comparator;

import com.untangle.uvm.tapi.Affinity;
import com.untangle.uvm.tapi.MPipe;
import com.untangle.uvm.tapi.SoloPipeSpec;

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
