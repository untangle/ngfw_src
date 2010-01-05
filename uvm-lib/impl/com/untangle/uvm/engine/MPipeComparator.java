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

import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.MPipe;
import com.untangle.uvm.vnet.SoloPipeSpec;

/**
 * Compares MPipes with SoloPipeSpecs.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
class MPipeComparator implements Comparator<MPipe>
{
    static final MPipeComparator COMPARATOR = new MPipeComparator();

    private MPipeComparator() { }

    public int compare(MPipe mp1, MPipe mp2)
    {
        SoloPipeSpec ps1 = null == mp1 ? null : (SoloPipeSpec)mp1.getPipeSpec();
        SoloPipeSpec ps2 = null == mp2 ? null : (SoloPipeSpec)mp2.getPipeSpec();

        Affinity ra1 = null == ps1 ? null : ps1.getAffinity();
        Affinity ra2 = null == ps2 ? null : ps2.getAffinity();

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
}
