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

import java.util.*;

import com.untangle.uvm.tapi.IPSessionDesc;
import com.untangle.uvm.tapi.SessionStats;
import com.untangle.uvm.tapi.TCPSessionDesc;


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
