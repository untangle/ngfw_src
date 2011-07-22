/*
 * $Id$
 */
package com.untangle.uvm.node;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;

import org.apache.log4j.Logger;

/**
 * The class <code>IPMAddr</code> represents an (optionally) masked IP address.
 *
 * TODO: XXX
 * deal with address aliasing eventually (address bits non-zero in mask 0 area)
 * deal with masks only of form 1*0*
 * This implementation sux and should be refactored.
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
 */
@SuppressWarnings("serial")
public class IPMaskedAddress implements Serializable, Comparable<IPMaskedAddress>
{
    private static final Logger logger = Logger.getLogger(IPMaskedAddress.class);

    public static final String ANY = "any";
    public static final String SPECIFIC_NODE_MASK = "255.255.255.255";
    public static final String ANY_ADDRESS = "0.0.0.0";

    public static IPMaskedAddress anyAddr = new IPMaskedAddress(ANY_ADDRESS,ANY_ADDRESS);

    private final String addr;
    private final String mask;

    /**
     * Creates a new <code>IPMaskedAddress</code> for a specific host and/or mask.
     * Assumes values are legal.  Use parse() if unsure.
     *
     * addr     = host-num "." host-num [ "." host-num [ "." host-num ] ]
     * host-num = digit [ digit [ digit ] ]
     *
     * @param addr a <code>String</code> of the form addr
     */
    public IPMaskedAddress(String addr)
    {
        if  (addr.contains("/")) {
            String[] strs = addr.split("/");
            String mask = strs[1];

            /* check if the mask is an int */
            try {
                int numbits = Integer.parseInt(mask);
                mask = longToMask(numbits == 0 ? 0 : 0xffffffff << (32 - numbits));;
            } catch (NumberFormatException x) {
                // guess it wasn't an INT, so just assume it wask an IP mask
            }

            this.addr = strs[0];
            this.mask = mask;
        } else {
            this.addr = addr;
            this.mask = SPECIFIC_NODE_MASK;
        }
    }

    /**
     * Creates a new <code>IPMaskedAddress</code> which is a copy of the input IPMaskedAddress.
     *
     * @param originalIPMaskedAddress an <code>IPMAddr</code> to be replicated
     */
    public IPMaskedAddress(IPMaskedAddress originalIPMaskedAddress)
    {
        if(originalIPMaskedAddress == null) {
            this.addr = null;
            this.mask = null;
        } else {
            this.addr = originalIPMaskedAddress.getAddr();
            this.mask = originalIPMaskedAddress.getMask();
        }
    }


    /**
     * Creates a new <code>IPMaskedAddress</code> given an address & number of network bits
     * Assumes values are legal.  Use parse() if unsure.
     *
     * addr     = host-num "." host-num [ "." host-num [ "." host-num ] ]
     * host-num = digit [ digit [ digit ] ]
     * maskbits  = 0 | 1 | ... | 32
     *
     * @param addr a <code>String</code> of the form addr
     * @param numbits an <code>int</code> of the form maskbits
     */
    public IPMaskedAddress(String addr, int numbits)
    {
        this.addr = addr;
        this.mask = longToMask(numbits == 0 ? 0 : 0xffffffff << (32 - numbits));;
    }

    /**
     * Creates a new <code>IPMaskedAddress</code> given the canonical representations for
     * address and mask (3 dot addresses).
     * Assumes values are legal.  Use parse() if unsure.
     *
     * addr = mask = host-num "." host-num [ "." host-num [ "." host-num ] ]
     * host-num    = digit [ digit [ digit ] ]
     *
     * @param addr a <code>String</code> of the form addr
     * @param mask a <code>String</code> of the form mask
     */
    public IPMaskedAddress(String addr, String mask)
    {
        this.addr = addr;
        this.mask = mask;
    }

    /**
     * Creates a new <code>IPMaskedAddress</code> given the the address and netmask
     *
     * @param addr a <code>String</code> of the form addr
     * @param mask a <code>String</code> of the form mask
     */
    public IPMaskedAddress(InetAddress addr, InetAddress mask)
    {
        this(addr.getHostAddress(),mask.getHostAddress());
    }

    /**
     * Creates a new <code>IPMaskedAddress</code> given the the address with a
     * mask of 255.255.255.255
     *
     * @param addr a <code>String</code> of the form addr
     * @param mask a <code>String</code> of the form mask
     */
    public IPMaskedAddress(InetAddress addr)
    {
        this(addr.getHostAddress());
    }

    public String getAddr()
    {
        return addr;
    }

    public String getMask()
    {
        return mask;
    }

    public int maskNumBits()
    {
        return maskToNumbits(mask);
    }

    public boolean isAny()
    {
        return (addr.equals(ANY_ADDRESS) && mask.equals(ANY_ADDRESS));
    }

    public boolean isNode()
    {
        return (mask.equals(SPECIFIC_NODE_MASK));
    }

    public InetAddress inetAddress()
    {
        try {
            return InetAddress.getByAddress(textToNumericFormat(addr));
        } catch (UnknownHostException x) {
            // impossible.
            throw new Error();
        }
    }

    public LinkedList<Boolean> bitString()
    {
        byte[] addrb = textToNumericFormat(addr);
        LinkedList<Boolean> result = new LinkedList<Boolean>();
        int i,j, numbits;
        int sum;

        /* build a linked list of bits that represents addr */
        for (i=addrb.length-1 ; i>=0 ; i--) {
            sum = addrb[i];
            if (sum < 0) sum = 256 + sum;

            for (j=0 ; j<8 ; j++, sum = sum/2) {
                if ((sum % 2) == 0)
                    result.addFirst(Boolean.FALSE);
                else
                    result.addFirst(Boolean.TRUE);
            }
        }

        /* truncate to appropriate length according to mask */
        numbits = maskToNumbits(mask);
        while (result.size() > numbits)
            result.removeLast();

        return result;
    }

    /**
     * <code>isIntersecting</code> returns true if the current IPMaskedAddress has at least one address
     * that is also present in the provided otherMaddr argument.
     *
     * @param otherMaddr an <code>IPMaskedAddress</code> to see if the current IPMaskedAddress has address(es) in common with
     * @return a <code>boolean</code> value
     */
    public boolean isIntersecting(IPMaskedAddress otherMaddr)
    {
        // shortcuts
        if (addr.equals(otherMaddr.addr) && mask.equals(otherMaddr.mask) ||
            isAny() ||
            otherMaddr.isAny())
            return true;

        return intersects(textToNumericFormat(addr), maskNumBits(),
                          textToNumericFormat(otherMaddr.addr), otherMaddr.maskNumBits());
    }

    /**
     * <code>isIntersecting</code> returns true if the given address falls inside the
     * IPMAddr.
     *
     * @param testAddr an <code>InetAddress</code> giving the address to test for membership in this IPMaskedAddress
     * @return a <code>boolean</code> true if the given testAddr falls inside us
     */
    public boolean contains(InetAddress testAddr)
    {
        String saddr = testAddr.getHostAddress();
        if (addr.equals(saddr) || isAny())
            return true;
        if (isNode())
            return false;
        return intersects(textToNumericFormat(addr), maskNumBits(),
                          testAddr.getAddress(), 32);
    }

    private static boolean intersects(byte[] addr1, int maskBits1,
                                      byte[] addr2, int maskBits2)
    {
        // Choose the shortest way
        int minBits = (maskBits1 < maskBits2 ? maskBits1 : maskBits2);

        int wholeBytes = minBits / 8;
        int rebits = minBits % 8;
        int curByte;
        for (curByte = 0; curByte < wholeBytes; curByte++) {
            if (addr1[curByte] != addr2[curByte])
                return false;
        }

        switch (rebits) {
        case 0:
            // Next line keeps the compiler happy but has no other effect.
        default:
            return true;
        case 1:
            return ((addr1[curByte] & 0x80) == (addr2[curByte] & 0x80));
        case 2:
            return ((addr1[curByte] & 0xc0) == (addr2[curByte] & 0xc0));
        case 3:
            return ((addr1[curByte] & 0xe0) == (addr2[curByte] & 0xe0));
        case 4:
            return ((addr1[curByte] & 0xf0) == (addr2[curByte] & 0xf0));
        case 5:
            return ((addr1[curByte] & 0xf8) == (addr2[curByte] & 0xf8));
        case 6:
            return ((addr1[curByte] & 0xfc) == (addr2[curByte] & 0xfc));
        case 7:
            return ((addr1[curByte] & 0xfe) == (addr2[curByte] & 0xfe));
        }
    }


    /**
     * <code>toString</code> emits the display version of the Maddr.  This is the
     * form "1.2.3.4/12", or "2.3.4.5" if the netmask is SPECIFIC_NODE_MASK or "any"?
     *
     * @return a <code>String</code> value
     */
    public String toString()
    {
        if (isAny())
            return ANY;
        else if (isNode()) {
            return addr;
        } else {
            // Is a network
            StringBuffer result = new StringBuffer(addr);
            result.append("/");
            result.append(maskToNumbits(mask));
            return result.toString();
        }
    }

    /**
     * The <code>parse</code> method takes an addrString, with BNF of: <p><code>
     * addr      = "any" | nummask | ipaddr [ " mask " ipaddr | " mask " hexnumber ]
     * nummask   = ipaddr [ "/" maskbits ]
     * ipaddr    = host-num "." host-num [ "." host-num [ "." host-num ] ]
     * host-num  = digit [ digit [ digit ] ]
     * maskbits  = 0 | 1 | ... | 32
     * hexnumber = "0" "x" hexstring
     * </code>
     *
     * and returns an IPMaskedAddress having a numeric address in dotted form and a numeric
     * netmask in dotted form, both as Strings.
     *
     * No IPV6 handling here.  XX
     *
     * @param addrString a <code>String</code> giving an host, address, host/mask, or address/mask
     * @return an <code>IPMaskedAddress</code> value
     * @exception IllegalArgumentException if the addrString is illegal in any way
     */
    public static IPMaskedAddress parse(String addrString)
        throws IllegalArgumentException
    {
        String addr;

        if (addrString == null)
            throw new IllegalArgumentException("IPMaskedAddress.parse(null)");
        if (addrString.equalsIgnoreCase("any"))
            return new IPMaskedAddress(ANY_ADDRESS, ANY_ADDRESS);

        addrString = addrString.trim();

        logger.debug("got addr '" + addrString + "'");

        int sl = addrString.indexOf('/');
        if (sl > 0) {
            // Looks like ipaddr/maskbits
            if (sl == (addrString.length() - 1))
                throw new IllegalArgumentException("IPMaskedAddress.parse(): empty maskbits");
            String maskbits = addrString.substring(sl + 1).trim();
            try {
                int numbits = Integer.parseInt(maskbits);
                if (numbits < 0 || numbits > 32)
                    throw new IllegalArgumentException("IPMaskedAddress.parse(): out of range maskbits: " + maskbits);
                long lmask = 0xffffffff << (32 - numbits);

                String ipaddr = addrString.substring(0, sl).trim();
                addr = canonicalizeHostAddress(ipaddr);

                return new IPMaskedAddress(addr, longToMask(lmask));
            } catch (NumberFormatException x) {
                throw new IllegalArgumentException("IPMaskedAddress.parse(): non-decimal maskbits: " + maskbits);
            }
        }

        int ma = addrString.indexOf(" mask ");
        if (ma > 0) {
            String maskstr = addrString.substring(ma + 6).trim();
            if (maskstr.startsWith("0x") || maskstr.startsWith("0X")) {
                // Hex mask
                try {
                    /* Parse long doesn't want the 0x */
                    long lmask = Long.parseLong(maskstr.substring( 2 ), 16);
                    if (lmask > 0xffffffffl)
                        throw new IllegalArgumentException("IPMaskedAddress.parse(): out of range mask: " + maskstr);
                    String ipaddr = addrString.substring(0, ma).trim();
                    addr = canonicalizeHostAddress(ipaddr);
                    return new IPMaskedAddress(addr, longToMask(lmask));
                } catch (NumberFormatException x) {
                    throw new IllegalArgumentException("IPMaskedAddress.parse(): non-hex mask: " + maskstr);
                }
            } else {
                // Dotted mask.  Currently must have three dots. XXX
                String mask = canonicalizeHostAddress(maskstr);
                String ipaddr = addrString.substring(0, ma).trim();
                addr = canonicalizeHostAddress(ipaddr);
                return new IPMaskedAddress(addr, mask);
            }
        }

        int ra = addrString.indexOf("-");
        if (ra > 0) {
            String ipArray[] = addrString.split("\\s*-\\s*");
            if ( ipArray.length != 2 )
                throw new IllegalArgumentException( "IPMaskedAddress.parse(): illegal range does not contain two components: " + addrString );

            IPMaskedAddress ip1 = new IPMaskedAddress(canonicalizeHostAddress(ipArray[0]));
            IPMaskedAddress ip2 = new IPMaskedAddress(canonicalizeHostAddress(ipArray[1]));
            long hosts = ip2.toLong() - ip1.toLong();
            int mask = 32 - (int)(Math.floor(Math.log(hosts) / Math.log(2)));
            logger.debug(ip1 + " -> " + ip2 + " (" + hosts + " hosts -> mask=" + mask + ")");
            return parse(ipArray[0] + "/" + mask); // re-use code above handling '/' notation
        } else {
            // Just an address, no netmask.
            addr = canonicalizeHostAddress(addrString);
            return new IPMaskedAddress(addr, SPECIFIC_NODE_MASK);
        }
    }

    public int hashCode()
    {
        return (37 * mask.hashCode() + addr.hashCode());
    }

    public boolean equals(Object o)
    {
        if (!(o instanceof IPMaskedAddress)) {
            return false;
        }
        IPMaskedAddress m = (IPMaskedAddress)o;

        if (!(null != m.addr && null != addr && m.addr.equals(addr))) {
            return false;
        }

        if (!(null != m.mask && null != mask && m.mask.equals(mask))) {
            return false;
        }

        return true;
    }

    public int compareTo(IPMaskedAddress other)
    {
        long oper1 = toLong();
        long oper2 = other.toLong();

        if (oper1 < oper2)
            return -1;
        else if (oper1 > oper2)
            return 1;
        else{
            if( maskToNumbits(mask) > maskToNumbits(other.mask) )
                return 1;
            else if( maskToNumbits(mask) < maskToNumbits(other.mask) )
                return -1;
            else
                return 0;
        }
    }

    /** Convert an IPMaskedAddress to a long */
    private long toLong( )
    {
        long val = 0;
        byte valArray[] = textToNumericFormat(addr);

        for ( int c = 0 ; c < INADDRSZ ; c++ ) {
            val += ((long)byteToInt(valArray[c])) << ( 8 * ( INADDRSZ - c - 1 ));
        }

        return val;
    }

    static int byteToInt ( byte val )
    {
        int num = val;
        if ( num < 0 ) num = num & 0x7F + 0x80;
        return num;
    }

    static final int INADDRSZ = 4;

    // IPV4 only right now.  Get rid of this. XXX
    static byte[] textToNumericFormat(String src)
    {
        if (src.length() == 0) {
            return null;
        }

        int octets;
        char ch;
        byte[] dst = new byte[INADDRSZ];
        char[] srcb = src.toCharArray();
        boolean saw_digit = false;

        octets = 0;
        int i = 0;
        int cur = 0;
        while (i < srcb.length) {
            ch = srcb[i++];
            if (Character.isDigit(ch)) {
                // note that Java byte is signed, so need to convert to int
                int sum = (dst[cur] & 0xff)*10 + (Character.digit(ch, 10) & 0xff);

                if (sum > 255) {
                    return null;
                }

                dst[cur] = (byte)(sum & 0xff);
                if (! saw_digit) {
                    if (++octets > INADDRSZ) {
                        return null;
                    }
                    saw_digit = true;
                }
            } else if (ch == '.' && saw_digit) {
                if (octets == INADDRSZ) {
                    return null;
                }
                cur++;
                dst[cur] = 0;
                saw_digit = false;
            } else
                return null;
        }
        if (octets < INADDRSZ) {
            return null;
        }
        return dst;
    }

    static String numericToTextFormat(byte[] src)
    {
        return (src[0] & 0xff) + "." + (src[1] & 0xff) + "." + (src[2] & 0xff) + "." + (src[3] & 0xff);
    }

    /**
     * <code>canonicalizeHostAddress</code> takes a dotted address (0-3 dots)
     * and canonicalizes it into a three dot dotted address.
     *
     * @param ipaddr a <code>String</code> address in dotted form
     * @return a <code>String</code> value
     */
    private static String canonicalizeHostAddress(String ipaddr)
        throws IllegalArgumentException
    {
        byte[] result = textToNumericFormat(ipaddr);
        if (result == null)
            throw new IllegalArgumentException("IPMaskedAddress.parse(): ipaddr not legal: " +
                                               ipaddr);
        else
            return numericToTextFormat(result);
    }

    // Utility function
    private static String longToMask(long lmask)
    {
        StringBuffer m = new StringBuffer(16);
        m.append((lmask >> 24) & 0xff);
        m.append('.');
        m.append((lmask >> 16) & 0xff);
        m.append('.');
        m.append((lmask >> 8) & 0xff);
        m.append('.');
        m.append(lmask & 0xff);
        return m.toString();
    }

    private static int maskToNumbits(String mask) {
        // Assumes mask is already legal
        if (mask.equals("255.255.255.255"))
            return 32;
        else if (mask.equals("0.0.0.0"))
            return 0;
        else if (mask.equals("255.255.255.0"))
            return 24;
        else if (mask.equals("255.255.0.0"))
            return 16;
        else if (mask.equals("255.0.0.0"))
            return 8;
        else if (mask.equals("255.255.255.254"))
            return 31;
        else if (mask.equals("255.255.255.252"))
            return 30;
        else if (mask.equals("255.255.255.248"))
            return 29;
        else if (mask.equals("255.255.255.240"))
            return 28;
        else if (mask.equals("255.255.255.224"))
            return 27;
        else if (mask.equals("255.255.255.192"))
            return 26;
        else if (mask.equals("255.255.255.128"))
            return 25;
        else if (mask.equals("255.255.254.0"))
            return 23;
        else if (mask.equals("255.255.252.0"))
            return 22;
        else if (mask.equals("255.255.248.0"))
            return 21;
        else if (mask.equals("255.255.240.0"))
            return 20;
        else if (mask.equals("255.255.224.0"))
            return 19;
        else if (mask.equals("255.255.192.0"))
            return 18;
        else if (mask.equals("255.255.128.0"))
            return 17;
        else if (mask.equals("255.254.0.0"))
            return 15;
        else if (mask.equals("255.252.0.0"))
            return 14;
        else if (mask.equals("255.248.0.0"))
            return 13;
        else if (mask.equals("255.240.0.0"))
            return 12;
        else if (mask.equals("255.224.0.0"))
            return 11;
        else if (mask.equals("255.192.0.0"))
            return 10;
        else if (mask.equals("255.128.0.0"))
            return 9;
        else if (mask.equals("254.0.0.0"))
            return 7;
        else if (mask.equals("252.0.0.0"))
            return 6;
        else if (mask.equals("248.0.0.0"))
            return 5;
        else if (mask.equals("240.0.0.0"))
            return 4;
        else if (mask.equals("224.0.0.0"))
            return 3;
        else if (mask.equals("192.0.0.0"))
            return 2;
        else if (mask.equals("128.0.0.0"))
            return 1;
        else
            throw new IllegalArgumentException("bad mask " + mask);
    }
}
