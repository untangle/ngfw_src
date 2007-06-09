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
package com.untangle.node.util;

import java.util.*;

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
        IPMaddr slashdot   = new IPMaddr("66.35.250.150");

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
