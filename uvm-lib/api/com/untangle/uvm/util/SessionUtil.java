/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */
package com.untangle.uvm.util;

import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

import com.untangle.uvm.vnet.IPSessionDesc;
import com.untangle.uvm.vnet.SessionStats;
import com.untangle.uvm.vnet.TCPSessionDesc;


public class SessionUtil {
    public static String prettyState(byte state) {
        switch (state) {
        case IPSessionDesc.CLOSED:
            return "CLOSED";
        case IPSessionDesc.OPEN:
            return "OPEN";
        case TCPSessionDesc.HALF_OPEN_INPUT:
            return "H_IN";
        case TCPSessionDesc.HALF_OPEN_OUTPUT:
            return "H_OUT";
        default:
            return "UNKNOWN";
        }
    }

    /*
      public static String prettyMode(byte mode) {
      switch (mode) {
      case IPSessionDesc.NORMAL_MODE:
      return "NORMAL";
      case IPSessionDesc.DOUBLE_ENDPOINT_MODE:
      return "DEM";
      case IPSessionDesc.SERVER_MODE:
      return "SERVER";
      case TCPSessionDesc.CLIENT_MODE:
      return "CLIENT";
      default:
      return "UNKNOWN";
      }
      }
    */

    private static final IPSessionDesc[] sdtemp = new IPSessionDesc[0];

    public static IPSessionDesc[] sortDescs(IPSessionDesc[] descs)
    {
        if (descs == null)
            return null;

        Comparator comp = new Comparator() {
                public int compare(Object o1, Object o2) {
                    IPSessionDesc s1 = (IPSessionDesc)o1;
                    IPSessionDesc s2 = (IPSessionDesc)o2;
                    int s1id = s1.id();
                    int s2id = s2.id();
                    if (s1id == s2id) {
                        SessionStats ss1 = s1.stats();
                        SessionStats ss2 = s2.stats();
                        long s1cd = ss1.creationDate().getTime();
                        long s2cd = ss2.creationDate().getTime();
                        if (s1cd == s2cd)
                            return 0;
                        return (s1cd < s2cd) ? -1 : 1;
                    }
                    return (s1id < s2id) ? -1 : 1;
                }
            };

        SortedSet set = new TreeSet(comp);
        for (int i = 0; i < descs.length; i++)
            set.add(descs[i]);

        return (IPSessionDesc[]) set.toArray(sdtemp);
    }
}
