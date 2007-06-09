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

package com.untangle.tran.util;

import static com.untangle.tran.util.Ascii.CR;
import static com.untangle.tran.util.Ascii.LF;
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

    /**
     * Counts the number of new lines in the sequence.  New lines
     * are CR, LF or CRLF.
     * <br><br>
     * If the sequence has no EOL, then 0 is returned.  If the
     * sequence ends with an EOL, then 1 is returned.
     */
    public static int countLines(CharSequence cs) {
        int ret = 0;
        final int len = cs.length();

        for(int i = 0; i<len; i++) {
            if(cs.charAt(i) == CR) {
                if(
                   (i+1<len) &&
                   (cs.charAt(i+1) == LF)) {
                    i++;
                }
                ret++;
                continue;
            }
            if(cs.charAt(i) == LF) {
                ret++;
            }
        }
        return ret;
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
