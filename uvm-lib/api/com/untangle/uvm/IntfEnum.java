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

package com.untangle.uvm;

import java.io.Serializable;
import java.util.HashMap;

/**
 * This singleton enumerates the interfaces present in the UVM.  It
 * is created via a factory method in <code>NetworkingManager</code>.
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
 */
@SuppressWarnings("serial")
public class IntfEnum implements Serializable
{

    public static final byte INTF_NONE_NUM = -1;

    private final byte[] intfs;

    private final HashMap<Byte, String> names;
    private final HashMap<Byte, String> userNames;

    // constructors -----------------------------------------------------------

    // Only used internally by NetworkingManagerImpl
    public IntfEnum(byte[] intfNums, String[] intfNames, String[] intfUserNames)
    {
        this.intfs = new byte[intfNums.length];
        System.arraycopy(intfNums, 0, this.intfs, 0, intfNums.length);

        names = new HashMap<Byte, String>();
        userNames = new HashMap<Byte, String>();
        for (int i = 0; i < intfNums.length; i++) {
            names.put(intfNums[i], intfNames[i]);
            userNames.put(intfNums[i], intfUserNames[i]);
        }
    }

    // Accessors

    public String getIntfName(byte intfNum)
    {
        return names.get(intfNum);
    }

    public String getIntfUserName(byte intfNum)
    {
        return userNames.get(intfNum);
    }

    // Enumerations -----------------------------------------------------------

    public byte[] getIntfNums()
    {
        byte[] result = new byte[intfs.length];
        System.arraycopy(intfs, 0, result, 0, intfs.length);
        return result;
    }

    public String[] getIntfNames()
    {
        String[] result = new String[intfs.length];
        int i = 0;
        for (Byte num : intfs) {
            result[i++] = names.get(num);
        }
        return result;
    }

    public String[] getIntfUserNames()
    {
        String[] result = new String[intfs.length];
        int i = 0;
        for (Byte num : intfs) {
            result[i++] = userNames.get(num);
        }
        return result;
    }

    // Object methods ---------------------------------------------------------

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("[ ");
        for (byte intf : intfs) {
            sb.append(Byte.toString(intf));
            sb.append(":");
            sb.append(names.get(intf));
            sb.append(":");
            sb.append(userNames.get(intf));
            sb.append(" ");
        }
        sb.append("]");

        return sb.toString();
    }

    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof IntfEnum)) {
            return false;
        }

        IntfEnum i = (IntfEnum)o;

        return names.equals(i.names) && userNames.equals(i.userNames);
    }

    @Override
    public int hashCode()
    {
        int result = 17;
        result = (37 * result) + names.hashCode();
        result = (37 * result) + userNames.hashCode();
        return result;
    }
}
