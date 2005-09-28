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

package com.metavize.tran.util;

import java.util.Comparator;

public class CharSequenceUtil
{
    public static final Comparator<CharSequence> COMPARATOR
        = new CharSequenceComparator();

    // public static functions ------------------------------------------------

    public static boolean startsWith(CharSequence cs1, CharSequence cs2)
    {
        if (cs1.length() < cs2.length()) { return false; }

        for (int i = 0; i < cs2.length(); i++) {
            if (cs1.charAt(i) != cs2.charAt(i)) {
                return false;
            }
        }

        return true;
    }

    // private classes --------------------------------------------------------

    private static class CharSequenceComparator
        implements Comparator<CharSequence>
    {
        public int compare(CharSequence cs1, CharSequence cs2)
        {
            int l1 = cs1.length();
            int l2 = cs2.length();

            for (int i = 0; i < Math.min(l1, l2); i++) {
                char c1 = cs1.charAt(i);
                char c2 = cs2.charAt(i);
                if (c1 < c2) {
                    return -1;
                } else if (c1 > c2) {
                    return 1;
                }
            }

            if (l1 < l2) {
                return -1;
            } else if (l1 > l2) {
                return 1;
            } else {
                return 0;
            }
        }

        public boolean equals(Object o)
        {
            return o instanceof CharSequenceComparator;
        }
    }
}
