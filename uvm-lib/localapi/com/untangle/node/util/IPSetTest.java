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
package com.untangle.node.util;

import com.untangle.uvm.node.IPMaddr;

public class IPSetTest {

    public static void main(String args[])
    {
        Integer result;

        IPSet set = new IPSetTrie();
        IPMaddr tendot    = new IPMaddr("10.0.0.0","255.0.0.0");
        IPMaddr tentendot = new IPMaddr("10.10.0.0","255.255.0.0");
        IPMaddr a         = new IPMaddr("10.10.0.1");
        IPMaddr b         = new IPMaddr("10.9.0.1");
        IPMaddr c         = new IPMaddr("192.168.0.1");
        IPMaddr d         = new IPMaddr("192.168.0.2");
        IPMaddr gator      = new IPMaddr("66.35.248.0","255.255.254.0");

        Integer uno = new Integer(1);
        Integer dos = new Integer(2);
        Integer tres = new Integer(3);

        set.add(tendot,uno);
        set.add(tentendot,dos);
        set.add(c,tres);
        set.add(gator,uno);


        result = (Integer)set.getLeastSpecific(a);
        if (!uno.equals(result))
            System.err.println("1 Wrong result:" + result);
        result = (Integer)set.getMostSpecific(a);
        if (!dos.equals(result))
            System.err.println("2 Wrong result:" + result);
        result = (Integer)set.getMostSpecific(b);
        if (!uno.equals(result))
            System.err.println("3 Wrong result:" + result);
        result = (Integer)set.getMostSpecific(c);
        if (!tres.equals(result))
            System.err.println("4 Wrong result:" + result);
        result = (Integer)set.getLeastSpecific(c);
        if (!tres.equals(result))
            System.err.println("5 Wrong result:" + result);
        result = (Integer)set.getLeastSpecific(d);
        if (result != null)
            System.err.println("6 Wrong result:" + result);
        result = (Integer)set.getLeastSpecific(new IPMaddr("255.255.255.255"));
        if (result != null)
            System.err.println("7 Wrong result:" + result);
        result = (Integer)set.getLeastSpecific(new IPMaddr("0.0.0.0"));
        if (result != null)
            System.err.println("8 Wrong result:" + result);
    }

}
