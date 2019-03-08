/**
 * $Id$
 */
package com.untangle.app.smtp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.untangle.uvm.util.Pair;

/**
 * Maps one email address to another. Understands limited wildcarding, where "*" maps (in regex land) to ".*". <br>
 * Threadsafe
 */
public class GlobEmailAddressMapper
{

    private final List<EmailAddressMatcher> m_list;
    private final List<Pair<String, String>> m_origList;

    /**
     * Construct an EmailAddressMapper, mapping the given addresses
     * 
     * @param list
     *            a list of "pairings", where Pair.a is the address to be matched, and Pair.b is the mapping. For
     *            example, to map "sales@foo.com" to "jdoe@foo.com" the following pair should be used
     *            <code>new Pair&lt;String, String>("sales@foo.com", "jdoe@foo.com")</code>
     */
    public GlobEmailAddressMapper(List<Pair<String, String>> list) {

        m_origList = list;

        m_list = new ArrayList<EmailAddressMatcher>();

        List<Pair<String, String>> plainAddresses = new ArrayList<Pair<String, String>>();

        for (Pair<String, String> p : list) {
            if (isWildcarded(p.a)) {
                if (plainAddresses.size() > 0) {
                    m_list.add(new SimpleEmailAddressMatcher(plainAddresses));
                    plainAddresses.clear();
                }
                m_list.add(new RegexEmailAddressMatcher(p));
            } else {
                plainAddresses.add(p);
            }
        }

        // Catch any leftovers
        if (plainAddresses.size() > 0) {
            m_list.add(new SimpleEmailAddressMatcher(plainAddresses));
        }
    }

    /**
     * Convert list to a string.
     *
     * @return List of addresses separated by newlines.
     */
    @Override
    public String toString()
    {
        String newLine = System.getProperty("line.separator", "\n");
        StringBuilder sb = new StringBuilder();
        for (Pair<String, String> p : m_origList) {
            sb.append(p.a).append(" ->").append(p.b).append(newLine);
        }
        return sb.toString();
    }

    /**
     * Warning - shared reference.
     * @return list of mapping.
     */
    public List<Pair<String, String>> getRawMappings()
    {
        return m_origList;
    }

    /**
     * Remove a mapping. For thread-safety reasons, this returns a new mapper (i.e. the mapper is immutable). 
     * @param mapping Pair to remove.
     * @return null if the mapping was not found
     */
    public GlobEmailAddressMapper removeMapping(Pair<String, String> mapping)
    {

        boolean removedOne = false;

        boolean isWildcard = isWildcarded(mapping.a);

        List<Pair<String, String>> newList = new ArrayList<Pair<String, String>>(m_origList);

        for (Pair<String, String> pair : m_origList) {
            if (!(pair.b.equalsIgnoreCase(mapping.b))) {
                continue;
            }
            boolean thisPairWildcard = isWildcarded(pair.a);
            if (isWildcard && thisPairWildcard && pair.a.equals(mapping.a)) {
                newList.remove(pair);
                removedOne = true;
            } else {
                if (!(thisPairWildcard) && pair.a.equalsIgnoreCase(mapping.a)) {
                    newList.remove(pair);
                    removedOne = true;
                }
            }
        }
        if (removedOne) {
            return new GlobEmailAddressMapper(newList);
        }
        return null;
    }

    /**
     * List all addresses which will route to the given address.
     *
     * @param rightSide String of right side email addresses.
     * @return String array of left side addresses.
     */
    public String[] getReverseMapping(String rightSide)
    {

        rightSide = rightSide.toLowerCase().trim();
        HashSet<String> set = new HashSet<String>();
        for (Pair<String, String> pair : m_origList) {
            if (pair.b.equalsIgnoreCase(rightSide)) {
                set.add(pair.a.toLowerCase());
            }
        }

        return set.toArray(new String[set.size()]);
    }

    /**
     * Get the address to-which the given argument address is mapped.
     * 
     * @param addr
     *            the address
     * 
     * @return the mapped address, or null if there is no such mapping.
     */
    public String getAddressMapping(String addr)
    {
        addr = addr.toLowerCase();

        for (EmailAddressMatcher matcher : m_list) {
            String ret = matcher.matchAddress(addr);
            if (ret != null) {
                return ret;
            }
        }
        return null;
    }

    /**
     * Test if this email address contains wildcarding
     * 
     * @param s
     *            the address
     * 
     * @return true if it contains a "*"
     */
    public static boolean isWildcarded(String s)
    {
        return s.indexOf('*') != -1;
    }

    /**
     * Convert this {@link #isWildcarded wildcarded} address into a regex.
     * 
     * @param str
     *            the wildcarded address
     * 
     * @return the regular expression to select addresses logicaly matching the original "pseudo-glob" syntax via
     *         regular expressions.
     */
    public static String fixupWildcardAddress(String str)
    {
        str = str.toLowerCase();
        str = str.replace(".", "\\.");
        str = str.replace("*", ".*");// Perhaps replace with {0,100} to prevent
                                     // too greedy matching?
        return str;
    }

    /**
     * Email Address matcher
     */
    abstract class EmailAddressMatcher
    {

        /**
         * Return right hand side matching address.
         * @param  strLowerCase String of email address to match.
         * @return              String of match.
         */
        abstract String matchAddress(String strLowerCase);

        /**
         * Store left hand mtch match in specified Set.
         * @param rightSideLowerCase Address to find.
         * @param addInto            Set of strings to add match into.
         */
        abstract void reverseMatch(String rightSideLowerCase, Set<String> addInto);

    }

    /**
     * Performs exact match on a set of from/to pairings.
     */
    class SimpleEmailAddressMatcher extends EmailAddressMatcher
    {

        private final HashMap<String, String> m_map;

        /**
         * Initialize SimpleEmailAddressMatcher instance.
         * @param pairings List of String Pairs for email addresses.
         * @return SimpleEmailAddressMatcher instance.
         */
        SimpleEmailAddressMatcher(List<Pair<String, String>> pairings) {

            m_map = new HashMap<String, String>();

            for (Pair<String, String> p : pairings) {
                m_map.put(p.a.toLowerCase(), p.b);
            }

        }

        /**
         * Look for email address and return matching string.
         * @param  strLowerCase Address to find.
         * @return              matchAddress object.
         */
        String matchAddress(String strLowerCase)
        {
            return m_map.get(strLowerCase);
        }

        /**
         * Look for right hand side and put matches into Set.
         * @param rightLower Right hand email address to lookup.
         * @param writeInto  Destination to write left hand side match.
         */
        void reverseMatch(String rightLower, Set<String> writeInto)
        {
            for (Map.Entry<String, String> entry : m_map.entrySet()) {
                if (entry.getValue().equalsIgnoreCase(rightLower)) {
                    writeInto.add(entry.getKey().toLowerCase());
                }
            }
        }
    }

    /**
     * Performs regex match on a set of from/to pairings.
     */
    class RegexEmailAddressMatcher extends EmailAddressMatcher
    {

        private Pattern m_pattern;
        private String m_mapTo;

        /**
         * Initialize RegexEmailAddressMatcher instance.
         * @param pairing List of String Pairs for email addresses.
         * @return RegexEmailAddressMatcher instance.
         */
        RegexEmailAddressMatcher(Pair<String, String> pairing) {

            m_pattern = Pattern.compile(fixupWildcardAddress(pairing.a));
            m_mapTo = pairing.b;
        }

        /**
         * Look for email address and return matching string.
         * @param  strLowerCase Regex of address to find.
         * @return              matchAddress object.
         */
        String matchAddress(String strLowerCase)
        {
            Matcher m = m_pattern.matcher(strLowerCase);
            return m.matches() ? m_mapTo : null;
        }

        /**
         * Look for right hand side and put matches into Set.
         * @param rightLower Right hand email regex match address to lookup.
         * @param writeInto  Destination to write left hand side match.
         */
        void reverseMatch(String rightLower, Set<String> writeInto)
        {
            if (rightLower.equalsIgnoreCase(rightLower)) {
                writeInto.add(m_pattern.pattern());
            }
        }
    }

    /************** Tests ******************/

    /**
     * Run tests.
     * 
     * @param  args Unused
     * @return      String of result.
     */
    public static String runTest(String[] args)
    {
        String result = "";
        List<Pair<String, String>> mappings = new ArrayList<Pair<String, String>>();

        mappings.add(new Pair<String, String>("foo@foo.com", "foo2@foo.com"));
        mappings.add(new Pair<String, String>("moo@foo.com", "moo2@foo.com"));
        mappings.add(new Pair<String, String>("*@doo.com", "dooAll@doo.com"));
        mappings.add(new Pair<String, String>("foo@goo.com", "foo2@foo.com"));
        mappings.add(new Pair<String, String>("foo@*", "foo@fooAll"));
        mappings.add(new Pair<String, String>("foo@hoo.com", "shouldNeverSeeMe"));

        GlobEmailAddressMapper mapper = new GlobEmailAddressMapper(mappings);

        String[] tests = new String[] { "foo@foo.com", "foo@doo.com", "foo@boo.com", "x@doo.com", "foo@hoo.com" };

        for (String test : tests) {
            result += test + ": " + mapper.getAddressMapping(test) + "\n";
        }

        String[] revMappings = mapper.getReverseMapping("foo2@foo.com");
        result += "BEGIN mappings for foo2 \n";
        for (String m : revMappings) {
            result += m + "\n";
        }
        return result + "ENDOF mappings for foo2\n";

    }

}
