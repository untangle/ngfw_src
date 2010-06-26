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
package com.untangle.node.mime.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.untangle.node.mime.EmailAddress;
import com.untangle.node.mime.EmailAddressHeaderField;

/**
 * Little test which creates MIME address lines
 * and attempts to parse them.  It induces known
 * flaws (as well as providing known nastynesses).
 */
public class GeneratedAddressTest {

    //Separators between addresses
    private static String[] s_separators;

    //Start of the address(es) line
    private static String[] s_preFixes =
        new String[] {"", ",", ", ", ";", "; ", ",;", ",; ", ";,", ";, "};

    //Endof of the address(es) line
    private static String[] s_suffixes =
        new String[] {"", ",", " ,", " ;", " ;", ",;", " ,;", ";,", " ;,"};

    static {
        List<String> seps = new ArrayList<String>();


        String[] seStrs = spacePad(",");
        for(String s : seStrs) {
            seps.add(s);
        }

        seStrs = spacePad(";");
        for(String s : seStrs) {
            seps.add(s);
        }
        seStrs = spacePad(";,");
        for(String s : seStrs) {
            seps.add(s);
        }
        s_separators =  seps.toArray(new String[seps.size()]);
    }


    private int m_testCount = 0;
    private int m_failureCount = 0;
    private int m_numTests;
    private int m_sampleAt = 100000;
    private TestProgress m_testProgress;


    GeneratedAddressTest() {

        List<String> personalsList = createPersonals();
        List<String> addressesList = createAddresses();

        String[] pre = personalsList.toArray(new String[personalsList.size()]);
        String[] addr = addressesList.toArray(new String[addressesList.size()]);

        System.out.println("Counting Tests...");
        for(List<String> p1 : new CombinationGenerator<String>(pre, addr)) {
            for(List<String> p2 : new CombinationGenerator<String>(pre, addr)) {
                testTwoAddressLine(new AddrBitPair(p1.get(0), p1.get(1)), new AddrBitPair(p2.get(0), p2.get(1)), true);
            }
        }

        m_numTests = m_testCount;
        m_testCount = 0;
        m_testProgress = new TestProgress(m_numTests, m_sampleAt);
        System.out.println("Will execute " + m_numTests + " tests");
        for(List<String> p1 : new CombinationGenerator<String>(pre, addr)) {
            for(List<String> p2 : new CombinationGenerator<String>(pre, addr)) {
                testTwoAddressLine(new AddrBitPair(p1.get(0), p1.get(1)), new AddrBitPair(p2.get(0), p2.get(1)), false);
            }
        }
    }


    boolean incrementTestCount() {

        if(m_testProgress != null) {
            m_testProgress.incrementTest();
        }

        if(++m_testCount%m_sampleAt == 0) {
            return true;
        }
        return false;
    }
    boolean incrementFailureCount() {
        return ++m_failureCount > 100;
    }

    private void testTwoAddressLine(AddrBitPair e1, AddrBitPair e2, boolean audit) {

        String[] emails1 = createFullAddress(e1);
        String[] emails2 = createFullAddress(e2);

        for(List<String> sep : new CombinationGenerator<String>(s_preFixes, s_separators, s_suffixes)) {
            for(String email1 : emails1) {
                for(String email2 : emails2) {
                    if(!executeTestTwoAddressLine(e1, e2, sep.get(0), email1, sep.get(1), email2, sep.get(2), audit)) {
                        if(incrementFailureCount()) {
                            System.exit(100);
                        }
                    }
                }
            }
        }
    }


    private boolean executeTestTwoAddressLine(AddrBitPair e1,
                                              AddrBitPair e2,
                                              String prefix,
                                              String a1,
                                              String sep,
                                              String a2,
                                              String suffix, boolean audit) {

        if(audit) {
            incrementTestCount();
            return true;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(prefix).append(a1).append(sep).append(a2).append(suffix);
        boolean print = incrementTestCount();
        if(print) {
            System.out.println("========== BEGIN " + m_testCount + " ==============");
            System.out.println("Testing: " + sb.toString());
        }
        try {
            List<EmailAddress> list = EmailAddressHeaderField.parseHeaderLine(sb.toString(), false);

            if(print) {
                System.out.println("Parsed into " + list.size() + " addresses");
                for(EmailAddress addr : list) {
                    System.out.print(addr.toMIMEString() + ", ");
                }
                System.out.println("");
                System.out.println("========== ENDOF " + m_testCount + " ==============");
            }
            return true;
        }
        catch(Exception ex) {
            System.out.println("====================");
            System.out.println("(" + m_testCount + ") Exception with: " + sb.toString());
            ex.printStackTrace();
            return false;
        }
    }


    private String[] createFullAddress(AddrBitPair p) {
        if("".equals(p.personal.trim())) {
            if("".equals(p.address.trim())) {
                return new String[] {""};
            }
            return new String[] {
                p.address,
                "<" + p.address,
                "<" + p.address + ">"};
        }
        if(isQuoted(p.personal)) {
            return new String[] {
                p.personal + " " + p.address,
                p.personal + "<" + p.address,
                p.personal + "<" + p.address + ">",
                p.personal + " <" + p.address,
                p.personal + " <" + p.address + ">"};
        }
        else {
            return new String[] {
                p.personal + "<" + p.address,
                p.personal + "<" + p.address + ">",
                p.personal + " <" + p.address,
                p.personal + " <" + p.address + ">"};
        }
    }

    private boolean isQuoted(String str) {
        str = str.trim();
        return str.startsWith("\"") && str.endsWith("\"");
    }

    private List<String> createAddresses() {
        List<String> ret = new ArrayList<String>();

        ret.add("");
        ret.add("foo");
        ret.add("foo moo");
        ret.add("foo@moo");
        ret.add("@moo");//Legal?
        ret.add("foo@");//Legal?

        return ret;
    }

    private List<String> createPersonals() {
        List<String> ret = new ArrayList<String>();

        //Nothing
        ret.add("");
        ret.add(quote(""));

        //Basics
        ret.add("foo");
        ret.add(quote("foo"));

        //Two tokens
        ret.add("foo goo");
        ret.add(quote("foo goo"));

        //Two tokens, comma sep
        ret.add(quote("foo, goo"));

        //Two tokens, semi sep
        ret.add(quote("foo; goo"));

        //Comment
        ret.add("foo(doo)moo");
        ret.add(quote("foo(doo)moo"));
        ret.add("foo (doo) moo");
        ret.add(quote("foo (doo) moo"));

        //Escaped comment
        ret.add(quote("foo\\(doo\\)moo"));
        ret.add(quote("foo \\(doo\\) moo"));

        return ret;

    }

    private static String quote(String str) {
        return "\"" + str + "\"";
    }

    private static String[] spacePad(String str) {
        String[] ret = new String[4];
        ret[0] = str;
        ret[1] = " " + str;
        ret[2] = " " + str + " ";
        ret[3] = str + "  ";
        return ret;
    }

    public static void main(final String[] args) throws Exception {
        new GeneratedAddressTest();
    }

    static class TestProgress {
        private final int m_numTests;
        private int m_testNum;
        private int m_notifyEvery;
        private long m_timestamp = 0;

        TestProgress(int numTests,
                     int notifyEvery) {
            m_numTests = numTests;
            m_notifyEvery = notifyEvery;
            m_timestamp = System.currentTimeMillis();
        }
        boolean incrementTest() {
            if((++m_testNum%m_notifyEvery) == 0) {
                long now = System.currentTimeMillis();
                long diff = now - m_timestamp;
                m_timestamp = now;
                
                double periodsRemaining = ((double) m_numTests- (double) m_testNum) / m_notifyEvery;

                long millisRemaining = (long) ((diff) * periodsRemaining);

                //        long millisPerSample = (long) (((double)diff/(double)m_notifyEvery) * (double)m_testNum);
                //        long millisRemaining = (long)
                //          (((double)diff/(double)m_notifyEvery) * (double) (m_numTests-m_testNum));
                //        long millisRemaining = (long) ((((double)m_numTests - (double)m_testNum)/((double)m_numTests/(double)m_notifyEvery)) * (double)diff);
                System.out.println(diff);
                System.out.println(millisRemaining);
                System.out.println(((int) (millisRemaining/(1000*60))) + " minutes remaing");
            }
            return true;
        }
    }


    //================ Inner Class =====================

    static class CombinationGenerator<E>
        implements Iterable<List<E>>, Iterator<List<E>> {

        private final E[][] m_arrays;
        private final int[] m_positions;
        private final int m_numArrays;
        private boolean m_hasNext = true;

        CombinationGenerator(E[]...arrays) {
            m_arrays = arrays;
            m_numArrays = m_arrays.length;
            m_positions = new int[m_numArrays];
            Arrays.fill(m_positions, 0);
            m_hasNext = false;
            for(E[] array : m_arrays) {
                if(array.length > 0) {
                    m_hasNext = true;
                }
            }
        }

        public Iterator<List<E>> iterator() {
            return this;
        }

        public boolean hasNext() {
            return m_hasNext;
        }

        public List<E> next() {
            if(!m_hasNext) {
                return null;
            }
            //Prepare the return
            ArrayList<E> ret = new ArrayList<E>();
            for(int i = 0; i<m_numArrays; i++) {
                ret.add(m_arrays[i][m_positions[i]]);
            }

            //Increment
            for(int i = m_numArrays-1; i>=0; i--) {
                m_positions[i]++;
                if(m_positions[i] >= m_arrays[i].length) {
                    if(i == 0) {
                        m_hasNext = false;
                        return ret;
                    }
                    m_positions[i] = 0;
                }
                else {
                    m_hasNext = true;
                    return ret;
                }
            }
            m_hasNext = false;
            return ret;

        }
        public void remove() {
            //
        }

    }

    //================ Inner Class =====================

    private class AddrBitPair {
        final String personal;
        final String address;

        AddrBitPair(String p,
                    String a) {
            this.personal = p;
            this.address = a;
        }
        public String toString() {
            return personal + " " + address;
        }
    }


}
