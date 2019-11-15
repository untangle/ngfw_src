/**
 * $HeadURL$
 */

package com.untangle.uvm.app;

import java.net.InetAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

/**
 * An interface to test for an address.
 */
public class IPMatcher
{
    private static final String MARKER_ANY = "any";
    private static final String MARKER_ALL = "all";
    private static final String MARKER_NONE = "none";
    private static final String MARKER_SEPERATOR = ",";
    private static final String MARKER_RANGE = "-";
    private static final String MARKER_SUBNET = "/";
    private static final String IPADDR_REGEX = "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$";
    public static final Pattern JAVA_IPADDR_REGEX = Pattern.compile("^/(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$");

    private static IPMatcher ANY_MATCHER = new IPMatcher(MARKER_ANY);
    private static IPMatcher NIL_MATCHER = new IPMatcher(MARKER_NONE);

    /* Number of bytes in an IPv4 address */
    private static final int INADDRSZ = 4; /* XXX IPv6 */

    private static Map<String,IPMatcher> MatcherCache;
    static {
        MatcherCache = new ConcurrentHashMap<>();
    }

// THIS IS FOR ECLIPSE - @formatter:off
    
    /* An array of the CIDR values */
    private static final String CIDR_STRINGS[] = 
    {
        "0.0.0.0",         "128.0.0.0",       "192.0.0.0",       "224.0.0.0",
        "240.0.0.0",       "248.0.0.0",       "252.0.0.0",       "254.0.0.0",
        "255.0.0.0",       "255.128.0.0",     "255.192.0.0",     "255.224.0.0",
        "255.240.0.0",     "255.248.0.0",     "255.252.0.0",     "255.254.0.0",
        "255.255.0.0",     "255.255.128.0",   "255.255.192.0",   "255.255.224.0",
        "255.255.240.0",   "255.255.248.0",   "255.255.252.0",   "255.255.254.0",
        "255.255.255.0",   "255.255.255.128", "255.255.255.192", "255.255.255.224",
        "255.255.255.240", "255.255.255.248", "255.255.255.252", "255.255.255.254",
        "255.255.255.255"
    };

// THIS IS FOR ECLIPSE - @formatter:on

    /* Should be an unmodifiable list or vector */
    private static final InetAddress CIDR_CONVERTER[] = new InetAddress[CIDR_STRINGS.length];

    public static enum IPMatcherType
    {
        ANY, NONE, SINGLE, RANGE, SUBNET, LIST
    };

    private final Logger logger = Logger.getLogger(getClass());

    /**
     * The string format of this matcher
     */
    public String matcher;

    /**
     * The type of this matcher
     */
    private IPMatcherType type = IPMatcherType.NONE;

    /**
     * if this port matcher is a list of port matchers, this list stores the
     * children
     */
    private LinkedList<IPMatcher> children = null;

    /**
     * if its a range these two variable store the min and max
     */
    private long rangeMin = -1;
    private long rangeMax = -1;

    /**
     * if its a subnet matcher
     */
    private long subnetNetwork = -1;
    private long subnetNetmask = -1;

    /**
     * if its just an int matcher this stores the number
     */
    private InetAddress single = null;

    /**
     * Make a subnet matcher
     * 
     * @param matcher
     *        The init string
     */
    public IPMatcher(String matcher)
    {
        initialize(matcher);
    }

    /**
     * Make a subnet matcher
     * 
     * @param network
     *        The network
     * @param netmask
     *        The netmask
     */
    public IPMatcher(InetAddress network, InetAddress netmask)
    {
        this.type = IPMatcherType.SUBNET;
        this.subnetNetmask = addrToLong(netmask);
        this.subnetNetwork = addrToLong(network);
    }

    /**
     * Return true if <param>address</param> matches this matcher.
     * 
     * @param address
     *        The address to test
     * @return True if the <param>address</param> matches.
     */
    public boolean isMatch(InetAddress address)
    {
        long tmp;

        switch (this.type)
        {

        case ANY:
            return true;

        case NONE:
            return false;

        case SINGLE:
            return this.single.equals(address);

        case RANGE:
            tmp = addrToLong(address);
            return ((this.rangeMin <= tmp) && (tmp <= this.rangeMax));

        case SUBNET:
            //logger.error("CHECK: " + address + " inside? " + Long.toHexString(this.subnetNetwork) + "/" + Long.toHexString(this.subnetNetmask) );
            tmp = addrToLong(address);
            boolean match = ((tmp & this.subnetNetmask) == (this.subnetNetwork & this.subnetNetmask));
            //logger.error("CHECK: " + address + " inside? " + Long.toHexString(this.subnetNetwork) + "/" + Long.toHexString(this.subnetNetmask) + " = " + match);
            return match;

        case LIST:
            for (IPMatcher child : this.children) {
                if (child.isMatch(address)) return true;
            }
            return false;

        default:
            logger.warn("Unknown IP matcher type: " + this.type);
            return false;
        }

    }

    /**
     * Maintain cache of matchers.
     *
     * @param  value String to match.
     * @return         Return already defined matcher from cache.  If not found, create new matcher intsance and add to cache.
     */
    public static synchronized IPMatcher getMatcher(String value){
        IPMatcher matcher = MatcherCache.get(value);
        if(matcher == null){
            matcher = new IPMatcher(value);
            MatcherCache.put(value, matcher);
        }
        return matcher;
    }

    /**
     * Returns the type of this matcher This is useful outside the class in a
     * few select instances
     * 
     * @return The type
     */
    public IPMatcherType getType()
    {
        return this.type;
    }

    /**
     * Return string representation
     * 
     * @return The string represenation
     */
    public String toString()
    {
        return matcher;
    }

    /**
     * Get a matcher that will match any IPMatcher
     * 
     * @return The matcher
     */
    public static IPMatcher getAnyMatcher()
    {
        return ANY_MATCHER;
    }

    /**
     * Get a matcher that will match nil
     * 
     * @return The matcher
     */
    public static IPMatcher getNilMatcher()
    {
        return NIL_MATCHER;
    }

    /**
     * Make a subnet matcher
     * 
     * @param network
     *        The network
     * @param netmask
     *        The netmask
     * @return The matcher
     */
    public static IPMatcher makeSubnetMatcher(InetAddress network, InetAddress netmask)
    {
        return new IPMatcher(network, netmask);
    }

    /**
     * Initialize all the private variables
     * 
     * @param matcher
     *        The init value
     */
    private void initialize(String matcher)
    {
        matcher = matcher.toLowerCase().trim().replaceAll("\\s", "");
        this.matcher = matcher;

        /**
         * If it contains a comma it must be a list of port matchers if so, go
         * ahead and initialize the children
         */
        if (matcher.contains(MARKER_SEPERATOR)) {
            this.type = IPMatcherType.LIST;

            this.children = new LinkedList<>();

            String[] results = matcher.split(MARKER_SEPERATOR);

            /* check each one */
            for (String childString : results) {
                IPMatcher child = new IPMatcher(childString);
                this.children.add(child);
            }

            return;
        }

        /**
         * Check the common constants
         */
        if (MARKER_ANY.equals(matcher)) {
            this.type = IPMatcherType.ANY;
            return;
        }
        if (MARKER_ALL.equals(matcher)) {
            this.type = IPMatcherType.ANY;
            return;
        }
        if (MARKER_NONE.equals(matcher)) {
            this.type = IPMatcherType.NONE;
            return;
        }

        /**
         * If it contains a dash it must be a range
         */
        if (matcher.contains(MARKER_RANGE)) {
            this.type = IPMatcherType.RANGE;

            String[] results = matcher.split(MARKER_RANGE);

            if (results.length != 2) {
                logger.warn("Invalid IPMatcher: Invalid Range: " + matcher);
                throw new java.lang.IllegalArgumentException("Invalid IPMatcher: Invalid Range: " + matcher);
            }

            try {
                if (!results[0].matches(IPADDR_REGEX)) throw new java.lang.IllegalArgumentException("Unknown IPMatcher format: \"" + matcher + "\" (invalid addr 0)");
                if (!results[1].matches(IPADDR_REGEX)) throw new java.lang.IllegalArgumentException("Unknown IPMatcher format: \"" + matcher + "\" (invalid addr 1)");

                InetAddress addrMin = InetAddress.getByName(results[0]);
                InetAddress addrMax = InetAddress.getByName(results[1]);

                this.rangeMin = addrToLong(addrMin);
                this.rangeMax = addrToLong(addrMax);
            } catch (java.net.UnknownHostException e) {
                logger.warn("Unknown IPMatcher range format: \"" + matcher + "\"", e);
                throw new java.lang.IllegalArgumentException("Unknown IPMatcher format: \"" + matcher + "\" (unknown host)", e);
            } catch (Exception e) {
                logger.warn("Unknown IPMatcher range format: \"" + matcher + "\"", e);
                throw new java.lang.IllegalArgumentException("Unknown IPMatcher format: \"" + matcher + "\" (exception)", e);
            }

            return;
        }

        /**
         * If it contains a slash it must be a subnet matcher
         */
        if (matcher.contains(MARKER_SUBNET)) {
            this.type = IPMatcherType.SUBNET;

            String[] results = matcher.split(MARKER_SUBNET);

            if (results.length != 2) {
                logger.warn("Invalid IPMatcher: Invalid Subnet: " + matcher);
                throw new java.lang.IllegalArgumentException("Invalid IPMatcher: Invalid Subnet: " + matcher);
            }

            try {
                if (!results[0].matches(IPADDR_REGEX)) throw new java.lang.IllegalArgumentException("Unknown IPMatcher format: \"" + matcher + "\" (invalid addr 0)");

                InetAddress addrNetwork = InetAddress.getByName(results[0]);
                this.subnetNetwork = addrToLong(addrNetwork);

                /**
                 * The netmask can be a IP (255.255.0.0) or a number (16)
                 */
                this.subnetNetmask = -1;

                /* First try to see if its a number */
                try {
                    this.subnetNetmask = cidrToLong(Integer.parseInt(results[1]));
                } catch (NumberFormatException e) {
                    /* It must be an IP address */
                }

                /* If that didnt work it must be a IP */
                if (subnetNetmask == -1) {
                    if (!results[1].matches(IPADDR_REGEX)) throw new java.lang.IllegalArgumentException("Unknown IPMatcher format: \"" + matcher + "\" (invalid subnet)");
                    this.subnetNetmask = addrToLong(InetAddress.getByName(results[1]));
                }

            } catch (java.net.UnknownHostException e) {
                logger.warn("Unknown IPMatcher subnet format: \"" + matcher + "\"", e);
                throw new java.lang.IllegalArgumentException("Unknown IPMatcher format: \"" + matcher + "\" (unknown host)", e);
            } catch (Exception e) {
                logger.warn("Unknown IPMatcher subnet format: \"" + matcher + "\"", e);
                throw new java.lang.IllegalArgumentException("Unknown IPMatcher format: \"" + matcher + "\" (exception)", e);
            }

            return;
        }

        /**
         * if it isn't any of these it must be a basic SINGLE matcher
         */
        this.type = IPMatcherType.SINGLE;
        try {
            if (!matcher.matches(IPADDR_REGEX)) throw new java.lang.IllegalArgumentException("Unknown IPMatcher format: \"" + matcher + "\" (invalid host)");

            this.single = InetAddress.getByName(matcher);
        } catch (java.net.UnknownHostException e) {
            logger.warn("Unknown IPMatcher single format: \"" + matcher + "\"", e);
            throw new java.lang.IllegalArgumentException("Unknown IPMatcher format: \"" + matcher + "\" (unknown host)", e);
        } catch (Exception e) {
            logger.warn("Unknown IPMatcher single format: \"" + matcher + "\"", e);
            throw new java.lang.IllegalArgumentException("Unknown IPMatcher format: \"" + matcher + "\" (exception)", e);
        }

        return;
    }

    /**
     * Convert a 4-byte address to a long
     * 
     * @param address
     *        The addres
     * @return the long value
     */
    private static long addrToLong(InetAddress address)
    {
        long val = 0;

        byte valArray[] = address.getAddress();

        for (int c = 0; c < INADDRSZ; c++) {
            val += ((long) byteToInt(valArray[c])) << (8 * (INADDRSZ - c - 1));
        }

        return val;
    }

    /**
     * Convert a CIDR index to an InetAddress.
     * 
     * @param cidr
     *        CIDR index to convert.
     * @return the InetAddress that corresponds to <param>cidr</param>.
     */
    private static InetAddress cidrToInetAddress(int cidr)
    {
        if (cidr < 0 || cidr > CIDR_CONVERTER.length) {
            throw new RuntimeException("CIDR notation[" + cidr + "] should end with a number between 0 and " + CIDR_CONVERTER.length);
        }

        return CIDR_CONVERTER[cidr];
    }

    /**
     * Convert a CIDR index to a long.
     * 
     * @param cidr
     *        CIDR index to convert.
     * @return the long that corresponds to <param>cidr</param>.
     */
    private static long cidrToLong(int cidr)
    {
        return addrToLong(cidrToInetAddress(cidr));
    }

    /**
     * convert a byte (unsigned) to int
     * 
     * @param val
     *        The byte value
     * @return The int
     */
    private static int byteToInt(byte val)
    {
        int num = val;
        if (num < 0) num = num & 0x7F + 0x80;
        return num;
    }

    static {
        int c = 0;
        for (String cidr : CIDR_STRINGS) {
            try {
                CIDR_CONVERTER[c++] = InetAddress.getByName(cidr);
            } catch (java.net.UnknownHostException e) {
                System.err.println("Invalid CIDR String at index: " + c);
            }
        }
    }
}
