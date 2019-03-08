/**
 * $Id$
 */
package com.untangle.uvm.app;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.UnknownHostException;
import java.util.LinkedList;

import org.apache.log4j.Logger;

/**
 * The class <code>IPMaskedAddress</code> represents a masked IP address.
 */
@SuppressWarnings("serial")
public class IPMaskedAddress implements Serializable
{
    private static final Logger logger = Logger.getLogger(IPMaskedAddress.class);

    private static final int V4_FULL_PREFIXLENGTH = 32;
    private static final int V6_FULL_PREFIXLENGTH = 128;

    private final InetAddress address;
    private final int prefixLength;

    public static final IPMaskedAddress anyAddr = new IPMaskedAddress("0.0.0.0/0");

    /**
     * Creates a new <code>IPMaskedAddress</code> for a specific host and/or
     * mask. Assumes values are legal. Use parse() if unsure.
     * 
     * @param addrString
     *        a <code>String</code> of the address
     */
    public IPMaskedAddress(String addrString)
    {
        if (addrString.contains("/")) {
            String[] strs = addrString.split("/");
            String addrStr = strs[0];
            String maskStr = strs[1];

            try {
                this.address = InetAddress.getByName(addrStr);
            } catch (UnknownHostException e) {
                throw new RuntimeException("Invalid Address: " + addrStr);
            }
            this.prefixLength = maskStringToPrefixLength(maskStr);
        } else {
            try {
                this.address = InetAddress.getByName(addrString);
            } catch (UnknownHostException e) {
                throw new RuntimeException("Invalid Address: " + addrString);
            }
            this.prefixLength = getFullPrefixLength(this.address);
        }
    }

    /**
     * Creates a new <code>IPMaskedAddress</code> which is a copy of the input
     * IPMaskedAddress.
     * 
     * @param originalIPMaskedAddress
     *        an <code>IPMaskedAddress</code> to be replicated
     */
    public IPMaskedAddress(IPMaskedAddress originalIPMaskedAddress)
    {
        if (originalIPMaskedAddress == null) {
            throw new RuntimeException("Invalid IPMaskedAddress: " + originalIPMaskedAddress);
        } else {
            this.address = originalIPMaskedAddress.getAddress();
            this.prefixLength = originalIPMaskedAddress.getPrefixLength();
        }
    }

    /**
     * Creates a new <code>IPMaskedAddress</code> given an address & number of
     * network bits Assumes values are legal. Use parse() if unsure.
     * 
     * @param addr
     *        a <code>String</code> representation of the address
     * @param prefixLength
     *        an <code>int</code> of the netmask/prefixLength
     */
    public IPMaskedAddress(String addr, int prefixLength)
    {
        try {
            this.address = InetAddress.getByName(addr);
        } catch (Exception e) {
            logger.warn("Invalid Address: " + addr, e);
            throw new RuntimeException("Invalid Address: " + addr);
        }
        this.prefixLength = prefixLength;
    }

    /**
     * Creates a new <code>IPMaskedAddress</code> given the representations for
     * address and mask (3 dot addresses). Assumes values are legal. Use parse()
     * if unsure.
     * 
     * @param addr
     *        a <code>String</code> of the form addr
     * @param mask
     *        a <code>String</code> of the form mask
     */
    public IPMaskedAddress(String addr, String mask)
    {
        try {
            this.address = InetAddress.getByName(addr);
        } catch (Exception e) {
            logger.warn("Invalid Address: " + addr, e);
            throw new RuntimeException("Invalid Address: " + addr);
        }

        try {
            this.prefixLength = maskStringToPrefixLength(mask);
        } catch (Exception e) {
            logger.warn("Invalid Mask: " + mask, e);
            throw new RuntimeException("Invalid Mask: " + mask);
        }
    }

    /**
     * Creates a new <code>IPMaskedAddress</code> given the the address and
     * netmask
     * 
     * @param addr
     *        a <code>String</code> of the form addr
     * @param prefixLength
     *        an <code>int</code> of the form maskbits
     */
    public IPMaskedAddress(InetAddress addr, int prefixLength)
    {
        this.address = addr;
        this.prefixLength = prefixLength;
    }

    /**
     * Creates a new <code>IPMaskedAddress</code> given the the address and
     * netmask
     * 
     * @param addr
     *        a <code>String</code> of the form addr
     * @param mask
     *        a <code>String</code> of the
     */
    public IPMaskedAddress(InetAddress addr, InetAddress mask)
    {
        this(addr.getHostAddress(), mask.getHostAddress());
    }

    /**
     * Creates a new <code>IPMaskedAddress</code> given the the address with a
     * mask of 255.255.255.255
     * 
     * @param addr
     *        a <code>String</code> of the form addr
     */
    public IPMaskedAddress(InetAddress addr)
    {
        this(addr.getHostAddress());
    }

    /**
     * Get the address
     * 
     * @return The address
     */
    public InetAddress getAddress()
    {
        return address;
    }

    /**
     * Get the address as a string
     * 
     * @return The string
     */
    public String getAddressString()
    {
        return address.getHostAddress();
    }

    /**
     * Get the prefix length
     * 
     * @return The length
     */
    public int getPrefixLength()
    {
        return this.prefixLength;
    }

    /**
     * Get the netmask as a string
     * 
     * @return The string
     */
    public String getNetmaskString()
    {
        if (!(this.address instanceof Inet4Address)) {
            logger.warn("Can not get netmask of non-IPv4 Address: " + this.address.getClass());
            throw new RuntimeException("Can not get netmask of non-IPv4 Address: " + this.address.getClass());
        }
        return v4PrefixLengthToNetmaskString(this.prefixLength);
    }

    /**
     * Get the netmask
     * 
     * @return The netmask
     */
    public InetAddress getNetmask()
    {
        String netmaskString = this.getNetmaskString();
        InetAddress netmask;

        try {
            netmask = InetAddress.getByName(netmaskString);
        } catch (Exception e) {
            logger.error("Unable to determine netmask for string: " + netmaskString);
            return null;
        }

        return netmask;
    }

    /**
     * This bitwise ANDs the mask with addr and returns the result: Example
     * getMaskedAddress of 192.168.1.1 and 255.255.0.0 returns 192.168.0.0
     * 
     * @return The masked address
     */
    public InetAddress getMaskedAddress()
    {
        byte[] addr = this.getAddress().getAddress();
        byte[] mask = prefixLengthToByteMask(this.getPrefixLength(), addr.length);

        if (addr.length != mask.length) {
            logger.warn("Invalid addr/mask: " + this.address + "/" + this.prefixLength);
            return null;
        }

        byte[] result = addr;
        for (int i = 0; i < addr.length; i++) {
            result[i] = (byte) (addr[i] & mask[i]);
        }

        try {
            InetAddress a = InetAddress.getByAddress(result);
            return a;
        } catch (Exception e) {
            logger.warn("Exception: ", e);
            return null;
        }
    }

    /**
     * This bitwise ANDs the mask with addr and adds one and returns the result:
     * Example getMaskedAddress of 192.168.1.1 and 255.255.0.0 returns
     * 192.168.0.1
     * 
     * @return The first masked address
     */
    public InetAddress getFirstMaskedAddress()
    {
        byte[] addr = this.getAddress().getAddress();
        byte[] mask = prefixLengthToByteMask(this.getPrefixLength(), addr.length);

        if (addr.length != mask.length) {
            logger.warn("Invalid addr/mask: " + this.address + "/" + this.prefixLength);
            return null;
        }

        byte[] result = addr;
        for (int i = 0; i < addr.length; i++) {
            result[i] = (byte) (addr[i] & mask[i]);
            if (i == (addr.length - 1) && (result[i] != (byte) 0xff)) //if last byte
                result[i] += 1;
        }

        try {
            InetAddress a = InetAddress.getByAddress(result);
            return a;
        } catch (Exception e) {
            logger.warn("Exception: ", e);
            return null;
        }
    }

    /**
     * Checks something and returns true or false
     * 
     * @return True or false
     */
    public boolean isApp()
    {
        if (this.address instanceof Inet4Address) return (this.prefixLength == V4_FULL_PREFIXLENGTH);
        else if (this.address instanceof Inet6Address) return (this.prefixLength == V6_FULL_PREFIXLENGTH);
        else return false;
    }

    /**
     * Get a linked list of boolean flags that match the address bits
     * 
     * @return The linked list
     */
    public LinkedList<Boolean> bitString()
    {
        byte[] addrb = address.getAddress();
        LinkedList<Boolean> result = new LinkedList<Boolean>();
        int i, j, numbits;
        int sum;

        /* build a linked list of bits that represents addr */
        for (i = addrb.length - 1; i >= 0; i--) {
            sum = addrb[i];
            if (sum < 0) sum = 256 + sum;

            for (j = 0; j < 8; j++, sum = sum / 2) {
                if ((sum % 2) == 0) result.addFirst(Boolean.FALSE);
                else result.addFirst(Boolean.TRUE);
            }
        }

        /* truncate to appropriate length according to mask */
        while (result.size() > prefixLength)
            result.removeLast();

        return result;
    }

    /**
     * <code>isIntersecting</code> returns true if the current IPMaskedAddress
     * has at least one address that is also present in the provided otherMaddr
     * argument.
     * 
     * @param other
     *        an <code>IPMaskedAddress</code> to see if the current
     *        IPMaskedAddress has address(es) in common with
     * @return a <code>boolean</code> value
     */
    public boolean isIntersecting(IPMaskedAddress other)
    {
        // shortcut (test equals)
        if (address.equals(other.address) && prefixLength == other.prefixLength) return true;

        return intersects(this.address.getAddress(), this.prefixLength, other.getAddress().getAddress(), other.getPrefixLength());
    }

    /**
     * <code>isIntersecting</code> returns true if the given address falls
     * inside the IPMAddr.
     * 
     * @param testAddr
     *        an <code>InetAddress</code> giving the address to test for
     *        membership in this IPMaskedAddress
     * @return a <code>boolean</code> true if the given testAddr falls inside us
     */
    public boolean contains(InetAddress testAddr)
    {
        // shortcut (test equals)
        if (address.equals(testAddr)) return true;

        return intersects(this.address.getAddress(), this.prefixLength, testAddr.getAddress(), getFullPrefixLength(testAddr));
    }

    /**
     * Returns a string representation of this masked address If the
     * prefixLength is "full" it is excluded
     * 
     * Example: 1.2.3.4/32 will return "1.2.3.4" Example: 1.2.3.4/24 will return
     * "1.2.3.4/24"
     * 
     * @return a <code>String</code> value
     */
    public String toString()
    {
        if (isApp()) {
            return this.address.getHostAddress();
        } else {
            return this.address.getHostAddress() + "/" + this.prefixLength;
        }
    }

    /**
     * Creates an IPMaskedAddress from the String representation
     * 
     * The <code>parse</code> method takes an addrString, with BNF of:
     * <p>
     * <code>
     * IPv4 "1.2.3.4"
     * IPv4/prefixLength -  "1.2.3.4/24"
     * IPv6 -  "fe00::0"
     * IPv6/prefixLength -  "fe00::0/64"
     * 
     * @param addrString
     *        a <code>String</code> giving an host, address, host/mask, or
     *        address/mask
     * @return an <code>IPMaskedAddress</code> value
     * @exception IllegalArgumentException
     *            if the addrString is illegal in any way
     */
    public static IPMaskedAddress parse(String addrString) throws IllegalArgumentException
    {
        InetAddress address;
        int prefixLength;

        if (addrString == null) throw new IllegalArgumentException("IPMaskedAddress.parse(null)");

        if (addrString.contains("/")) {
            String[] strs = addrString.split("/");
            String addrStr = strs[0];
            String maskStr = strs[1];

            try {
                address = InetAddress.getByName(addrStr);
            } catch (UnknownHostException e) {
                throw new RuntimeException("Invalid Address: " + addrStr);
            }
            prefixLength = maskStringToPrefixLength(maskStr);
        } else {
            try {
                address = InetAddress.getByName(addrString);
            } catch (UnknownHostException e) {
                throw new RuntimeException("Invalid Address: " + addrString);
            }
            prefixLength = getFullPrefixLength(address);
        }

        return new IPMaskedAddress(address, prefixLength);
    }

    /**
     * Compare to the argumented object
     * 
     * @param o
     *        The object for comparison
     * 
     * @return True if equal, otherwise false
     */
    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof IPMaskedAddress)) {
            return false;
        }
        IPMaskedAddress m = (IPMaskedAddress) o;

        if (!(null != m.address && address != null && m.address.equals(address))) {
            return false;
        }
        if (m.prefixLength != this.prefixLength) {
            return false;
        }

        return true;
    }

    /**
     * Returns the prefixLength for a specific host based on the type of address
     * If IPv4, return 32, if IPv6 return 128, otherwise return 32
     * 
     * @param addr
     *        The address
     * @return The previx length
     */
    private static int getFullPrefixLength(InetAddress addr)
    {
        if (addr instanceof Inet4Address) return V4_FULL_PREFIXLENGTH;
        else if (addr instanceof Inet6Address) return V6_FULL_PREFIXLENGTH;
        else {
            logger.warn("Unknown InetAddress Type: " + addr.getClass());
            return V4_FULL_PREFIXLENGTH;
        }
    }

    /**
     * Takes a string reperesting the netmask/prefixLength and returns the
     * prefixLength
     * 
     * Example: "255.255.255.0" = 24 Example: "64" = 64 Example: "24" = 24
     * 
     * @param maskString
     *        The mask string
     * @return The prefix length
     */
    private static int maskStringToPrefixLength(String maskString)
    {
        /* check if the mask is an int */
        try {
            int numbits = Integer.parseInt(maskString);
            return numbits;
        } catch (Exception x) {
            // not an int
        }

        try {
            int numbits = v4MaskToPrefixLength(maskString);
            return numbits;
        } catch (Exception x) {
            // not a v4mask
        }

        logger.warn("Invalid netmask/prefixLength: " + maskString);
        return 32; //XXX
    }

    /**
     * Convert an IPv4 mask to a prefix length
     * 
     * @param mask
     *        The mask
     * @return The prefix length
     */
    private static int v4MaskToPrefixLength(String mask)
    {
        // Assumes mask is already legal
        if (mask.equals("255.255.255.255")) return 32;
        else if (mask.equals("0.0.0.0")) return 0;
        else if (mask.equals("255.255.255.0")) return 24;
        else if (mask.equals("255.255.0.0")) return 16;
        else if (mask.equals("255.0.0.0")) return 8;
        else if (mask.equals("255.255.255.254")) return 31;
        else if (mask.equals("255.255.255.252")) return 30;
        else if (mask.equals("255.255.255.248")) return 29;
        else if (mask.equals("255.255.255.240")) return 28;
        else if (mask.equals("255.255.255.224")) return 27;
        else if (mask.equals("255.255.255.192")) return 26;
        else if (mask.equals("255.255.255.128")) return 25;
        else if (mask.equals("255.255.254.0")) return 23;
        else if (mask.equals("255.255.252.0")) return 22;
        else if (mask.equals("255.255.248.0")) return 21;
        else if (mask.equals("255.255.240.0")) return 20;
        else if (mask.equals("255.255.224.0")) return 19;
        else if (mask.equals("255.255.192.0")) return 18;
        else if (mask.equals("255.255.128.0")) return 17;
        else if (mask.equals("255.254.0.0")) return 15;
        else if (mask.equals("255.252.0.0")) return 14;
        else if (mask.equals("255.248.0.0")) return 13;
        else if (mask.equals("255.240.0.0")) return 12;
        else if (mask.equals("255.224.0.0")) return 11;
        else if (mask.equals("255.192.0.0")) return 10;
        else if (mask.equals("255.128.0.0")) return 9;
        else if (mask.equals("254.0.0.0")) return 7;
        else if (mask.equals("252.0.0.0")) return 6;
        else if (mask.equals("248.0.0.0")) return 5;
        else if (mask.equals("240.0.0.0")) return 4;
        else if (mask.equals("224.0.0.0")) return 3;
        else if (mask.equals("192.0.0.0")) return 2;
        else if (mask.equals("128.0.0.0")) return 1;
        else throw new IllegalArgumentException("bad mask " + mask);
    }

    /**
     * Convert a prefix length to a netmask string
     * 
     * @param prefixLength
     *        The prefix length
     * @return The netmask string
     */
    private static String v4PrefixLengthToNetmaskString(int prefixLength)
    {
        // Assumes mask is already legal
        if (prefixLength == 32) return "255.255.255.255";
        else if (prefixLength == 0) return "0.0.0.0";
        else if (prefixLength == 24) return "255.255.255.0";
        else if (prefixLength == 16) return "255.255.0.0";
        else if (prefixLength == 8) return "255.0.0.0";
        else if (prefixLength == 31) return "255.255.255.254";
        else if (prefixLength == 30) return "255.255.255.252";
        else if (prefixLength == 29) return "255.255.255.248";
        else if (prefixLength == 28) return "255.255.255.240";
        else if (prefixLength == 27) return "255.255.255.224";
        else if (prefixLength == 26) return "255.255.255.192";
        else if (prefixLength == 25) return "255.255.255.128";
        else if (prefixLength == 23) return "255.255.254.0";
        else if (prefixLength == 22) return "255.255.252.0";
        else if (prefixLength == 21) return "255.255.248.0";
        else if (prefixLength == 20) return "255.255.240.0";
        else if (prefixLength == 19) return "255.255.224.0";
        else if (prefixLength == 18) return "255.255.192.0";
        else if (prefixLength == 17) return "255.255.128.0";
        else if (prefixLength == 15) return "255.254.0.0";
        else if (prefixLength == 14) return "255.252.0.0";
        else if (prefixLength == 13) return "255.248.0.0";
        else if (prefixLength == 12) return "255.240.0.0";
        else if (prefixLength == 11) return "255.224.0.0";
        else if (prefixLength == 10) return "255.192.0.0";
        else if (prefixLength == 9) return "255.128.0.0";
        else if (prefixLength == 7) return "254.0.0.0";
        else if (prefixLength == 6) return "252.0.0.0";
        else if (prefixLength == 5) return "248.0.0.0";
        else if (prefixLength == 4) return "240.0.0.0";
        else if (prefixLength == 3) return "224.0.0.0";
        else if (prefixLength == 2) return "192.0.0.0";
        else if (prefixLength == 1) return "128.0.0.0";
        else throw new IllegalArgumentException("bad prefixLength " + prefixLength);
    }

    /**
     * Return true if the two masked addresses intersect
     * 
     * @param addr1
     *        First address
     * @param prefixLength1
     *        First prefix length
     * @param addr2
     *        Seond address
     * @param prefixLength2
     *        Second prefix length
     * @return True if they intersect, otherwise false
     */
    private static boolean intersects(byte[] addr1, int prefixLength1, byte[] addr2, int prefixLength2)
    {
        // Choose the shortest way
        int minBits = (prefixLength1 < prefixLength2 ? prefixLength1 : prefixLength2);

        int wholeBytes = minBits / 8;
        int rebits = minBits % 8;
        int curByte;
        for (curByte = 0; curByte < wholeBytes; curByte++) {
            if (addr1[curByte] != addr2[curByte]) return false;
        }

        switch (rebits)
        {
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
     * Return the prefix length as a bitmask Example: 24, 4 returns 0xffffff00
     * 
     * @param prefixLength
     *        The prefix length
     * @param maskLengthBytes
     *        The mask length
     * @return The prefix length as a bitmask
     */
    private static byte[] prefixLengthToByteMask(int prefixLength, int maskLengthBytes)
    {
        byte[] mask = new byte[maskLengthBytes];

        int wholeBytes = prefixLength / 8;
        int rebits = prefixLength % 8;
        int curByte = 0;

        // zero out mask
        for (curByte = 0; curByte < mask.length; curByte++)
            mask[curByte] = (byte) 0x00;

        for (curByte = 0; curByte < wholeBytes && curByte < maskLengthBytes; curByte++) {
            mask[curByte] = (byte) 0xff;
        }

        if (curByte == maskLengthBytes) {
            return mask;
        }

        switch (rebits)
        {
        case 1:
            mask[curByte] = (byte) 0x80;
            break;
        case 2:
            mask[curByte] = (byte) 0xc0;
            break;
        case 3:
            mask[curByte] = (byte) 0xe0;
            break;
        case 4:
            mask[curByte] = (byte) 0xf0;
            break;
        case 5:
            mask[curByte] = (byte) 0xf8;
            break;
        case 6:
            mask[curByte] = (byte) 0xfc;
            break;
        case 7:
            mask[curByte] = (byte) 0xfe;
            break;
        case 0:
            mask[curByte] = (byte) 0x00;
            break;
        default:
            // Nothing
        }

        return mask;
    }

    /**
     * Run some tests
     * 
     * @return The test result
     */
    public static boolean runTests()
    {
        try {
            InetAddress inetaddr = InetAddress.getByName("1.2.3.4");
            InetAddress inetaddr1 = InetAddress.getByName("1.2.3.1");

            IPMaskedAddress addr1 = new IPMaskedAddress("1.2.3.4");
            if (!addr1.toString().equals("1.2.3.4")) {
                logger.warn("FAIL TEST", new Exception());
                return false;
            }

            IPMaskedAddress addr2 = new IPMaskedAddress("1.2.3.0/24");
            if (!addr2.toString().equals("1.2.3.0/24")) {
                logger.warn("FAIL TEST", new Exception());
                return false;
            }

            IPMaskedAddress addr3 = new IPMaskedAddress("1.2.4.0/24");
            if (!addr3.toString().equals("1.2.4.0/24")) {
                logger.warn("FAIL TEST", new Exception());
                return false;
            }

            IPMaskedAddress addr4 = new IPMaskedAddress("1.2.3.4/24");
            if (!addr4.getMaskedAddress().getHostAddress().equals("1.2.3.0")) {
                logger.warn("FAIL TEST", new Exception());
                return false;
            }

            IPMaskedAddress addr5 = new IPMaskedAddress("1.2.3.4", "255.255.255.0");
            IPMaskedAddress addr6 = new IPMaskedAddress("1.2.3.4", 24);
            IPMaskedAddress addr7 = new IPMaskedAddress("1.2.3.4", 25);
            if (!addr4.equals(addr5)) {
                logger.warn("FAIL TEST", new Exception());
                return false;
            }
            if (!addr4.equals(addr6)) {
                logger.warn("FAIL TEST", new Exception());
                return false;
            }
            if (addr4.equals(addr7)) {
                logger.warn("FAIL TEST", new Exception());
                return false;
            }

            if (!addr4.getNetmaskString().equals("255.255.255.0")) {
                logger.warn("FAIL TEST", new Exception());
                return false;
            }
            if (!addr1.getNetmaskString().equals("255.255.255.255")) {
                logger.warn("FAIL TEST", new Exception());
                return false;
            }

            if (!addr2.contains(inetaddr)) {
                logger.warn("FAIL TEST", new Exception());
                return false;
            }
            if (addr3.contains(inetaddr)) {
                logger.warn("FAIL TEST", new Exception());
                return false;
            }

            if (addr2.isIntersecting(addr3)) {
                logger.warn("FAIL TEST", new Exception());
                return false;
            }
            if (!addr2.isIntersecting(addr1)) {
                logger.warn("FAIL TEST", new Exception());
                return false;
            }
            if (addr3.isIntersecting(addr1)) {
                logger.warn("FAIL TEST", new Exception());
                return false;
            }

            if (!addr1.isApp()) {
                logger.warn("FAIL TEST", new Exception());
                return false;
            }
            if (addr2.isApp()) {
                logger.warn("FAIL TEST", new Exception());
                return false;
            }

            // test getFirstMaskedAddress
            if (!addr2.getFirstMaskedAddress().equals(inetaddr1)) {
                logger.warn("FAIL TEST", new Exception());
                return false;
            }
        } catch (Exception e) {
            logger.warn("TEST FAILED:", e);
            return false;
        }

        return true;
    }

    /**
     * Get our hashcode
     * 
     * @return Our hashcode or zero if our address is null
     */
    public int hashCode()
    {
        if (address != null) return address.hashCode() * prefixLength;
        else return 0;
    }
}
