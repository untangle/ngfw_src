/**
 * $Id$
 */
package com.untangle.node.util;

import com.untangle.uvm.node.IPMaskedAddress;

public class IPSetTest {

    public static void main(String args[])
    {
        Integer result;

        IPSet set = new IPSetTrie();
        IPMaskedAddress tendot    = new IPMaskedAddress("10.0.0.0","255.0.0.0");
        IPMaskedAddress tentendot = new IPMaskedAddress("10.10.0.0","255.255.0.0");
        IPMaskedAddress a         = new IPMaskedAddress("10.10.0.1");
        IPMaskedAddress b         = new IPMaskedAddress("10.9.0.1");
        IPMaskedAddress c         = new IPMaskedAddress("192.168.0.1");
        IPMaskedAddress d         = new IPMaskedAddress("192.168.0.2");
        IPMaskedAddress gator      = new IPMaskedAddress("66.35.248.0","255.255.254.0");

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
        result = (Integer)set.getLeastSpecific(new IPMaskedAddress("255.255.255.255"));
        if (result != null)
            System.err.println("7 Wrong result:" + result);
        result = (Integer)set.getLeastSpecific(new IPMaskedAddress("0.0.0.0"));
        if (result != null)
            System.err.println("8 Wrong result:" + result);
    }

}
