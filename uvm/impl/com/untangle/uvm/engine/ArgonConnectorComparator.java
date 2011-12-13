/**
 * $Id$
 */
package com.untangle.uvm.engine;

import java.util.Comparator;

import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.ArgonConnector;
import com.untangle.uvm.vnet.SoloPipeSpec;

/**
 * Compares ArgonConnectors with SoloPipeSpecs.
 */
class ArgonConnectorComparator implements Comparator<ArgonConnector>
{
    static final ArgonConnectorComparator COMPARATOR = new ArgonConnectorComparator();

    private ArgonConnectorComparator() { }

    public int compare(ArgonConnector mp1, ArgonConnector mp2)
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
