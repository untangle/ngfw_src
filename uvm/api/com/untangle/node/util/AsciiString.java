/*
 * $HeadURL: svn://chef/work/src/uvm/api/com/untangle/node/util/AsciiString.java $
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

/**
 * Wraps a byte array to create a <code>CharSequence</code>.
 *
 */
public class AsciiString implements CharSequence
{
    private final byte[] val;

    public AsciiString(byte[] val)
    {
        this.val = new byte[val.length];
        System.arraycopy(val, 0, this.val, 0, val.length);
    }

    // for subsequence
    private AsciiString(byte[] val, int offset, int length)
    {
        this.val = new byte[length];
        System.arraycopy(val, 0, this.val, offset, length);
    }

    public char charAt(int i)
    {
        return (char)val[i];
    }

    public int length()
    {
        return val.length;
    }

    public CharSequence subSequence(int start, int end)
    {
        return new AsciiString(val, start, end - start);
    }

    public String toString()
    {
        return new String(val);
    }
}
