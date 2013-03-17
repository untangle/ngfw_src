/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.smtp.quarantine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.untangle.node.util.Pair;



/**
 * Maps one email address to another.  Understands limited
 * wildcarding, where "*" maps (in regex land) to ".*".
 * <br>
 * Threadsafe
 */
public class EmailAddressMapper {

    private final List<EmailAddressMatcher> m_list;


    /**
     * Construct an EmailAddressMapper, mapping the given
     * addresses
     *
     * @param list a list of "pairings", where Pair.a is the
     *        address to be matched, and Pair.b is the mapping.
     *        For example, to map "sales@foo.com" to "jdoe@foo.com"
     *        the following pair should be used
     *        <code>new Pair&lt;String, String>("sales@foo.com", "jdoe@foo.com")</code>
     */
    public EmailAddressMapper(List<Pair<String, String>> list) {

        m_list = new ArrayList<EmailAddressMatcher>();

        List<Pair<String, String>> plainAddresses =
            new ArrayList<Pair<String, String>>();

        for(Pair<String, String> p : list) {
            if(isWildcarded(p.a)) {
                if(plainAddresses.size() > 0) {
                    m_list.add(new SimpleEmailAddressMatcher(plainAddresses));
                    plainAddresses.clear();
                }
                m_list.add(new RegexEmailAddressMatcher(p));
            }
            else {
                plainAddresses.add(p);
            }
        }

        //Catch any leftovers
        if(plainAddresses.size() > 0) {
            m_list.add(new SimpleEmailAddressMatcher(plainAddresses));
        }

    }

    /**
     * Get the address to-which the given argument address
     * is mapped.
     *
     * @param addr the address
     *
     * @return the mapped address, or null if there
     *         is no such mapping.
     */
    public String getAddressMapping(String addr) {
        addr = addr.toLowerCase();

        for(EmailAddressMatcher matcher : m_list) {
            String ret = matcher.matchAddress(addr);
            if(ret != null) {
                return ret;
            }
        }
        return null;
    }

    /**
     * Test if this email address contains wildcarding
     *
     * @param s the address
     *
     * @return true if it contains a "*"
     */
    public static boolean isWildcarded(String s) {
        return s.indexOf('*') != -1;
    }


    /**
     * Convert this {@link #isWildcarded wildcarded} address
     * into a regex.
     *
     * @param str the wildcarded address
     *
     * @return the regular expression to select addresses logicaly
     *         matching the original "pseudo-glob" syntax via
     *         regular expressions.
     */
    public static String fixupWildcardAddress(String str) {
        str = str.toLowerCase();
        str = str.replace(".", "\\.");
        str = str.replace("*", ".*");//Perhaps replace with {0,100} to prevent too greedy matching?
        return str;
    }



    abstract class EmailAddressMatcher {

        abstract String matchAddress(String strLowerCase);

    }


    /**
     * Performs exact match on a set of
     * from/to pairings.
     */
    class SimpleEmailAddressMatcher
        extends EmailAddressMatcher {

        private final HashMap<String, String> m_map;

        SimpleEmailAddressMatcher(List<Pair<String, String>> pairings) {

            m_map = new HashMap<String, String>();

            for(Pair<String, String> p : pairings) {
                m_map.put(p.a.toLowerCase(), p.b);
            }

        }
        String matchAddress(String strLowerCase) {
            return m_map.get(strLowerCase);
        }
    }

    class RegexEmailAddressMatcher
        extends EmailAddressMatcher {

        private Pattern m_pattern;
        private String m_mapTo;

        RegexEmailAddressMatcher(Pair<String, String> pairing) {

            m_pattern = Pattern.compile(fixupWildcardAddress(pairing.a));
            m_mapTo = pairing.b;
        }
        String matchAddress(String strLowerCase) {
            Matcher m = m_pattern.matcher(strLowerCase);
            return m.matches()?
                m_mapTo:
            null;
        }
    }


    public static void main(String[] args) {

        List<Pair<String, String>> mappings =
            new ArrayList<Pair<String, String>>();

        mappings.add(new Pair<String, String>("foo@foo.com", "foo2@foo.com"));
        mappings.add(new Pair<String, String>("moo@foo.com", "moo2@foo.com"));
        mappings.add(new Pair<String, String>("*@doo.com", "dooAll@doo.com"));
        mappings.add(new Pair<String, String>("foo@goo.com", "foo2@foo.com"));
        mappings.add(new Pair<String, String>("foo@*", "foo@fooAll"));
        mappings.add(new Pair<String, String>("foo@hoo.com", "shouldNeverSeeMe"));


        EmailAddressMapper mapper = new EmailAddressMapper(mappings);

        String[] tests = new String[] {
            "foo@foo.com",
            "foo@doo.com",
            "foo@boo.com",
            "x@doo.com",
            "foo@hoo.com"
        };

        for(String test : tests) {
            System.out.println(test + ": " + mapper.getAddressMapping(test));
        }

    }


}
