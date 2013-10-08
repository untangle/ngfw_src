/*
 * $HeadURL: svn://chef/work/src/uvm/api/com/untangle/node/util/CharSequenceUtil.java $
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

package com.untangle.node.util;

import static com.untangle.node.util.Ascii.CR;
import static com.untangle.node.util.Ascii.LF;

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

    public static boolean contains(CharSequence cs1, CharSequence cs2)
    {
        if (cs1.length() < cs2.length()) { return false; }

    int j = 0;

        for (int i = 0; i < cs1.length(); i++) {
            if (cs1.charAt(i) != cs2.charAt(j))
        j = 0;
            else
        if (j++ == cs2.length() -1)
            return true;
    }

        return false;
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
