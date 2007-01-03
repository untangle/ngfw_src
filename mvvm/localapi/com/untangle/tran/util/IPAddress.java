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
package com.untangle.tran.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

import org.apache.log4j.Logger;

/* IP address, IPv4
 * - input must be a series of 4 address block values
 *   - represented as 4 numbers in dotted-decimal notation
 *     (D (msblock).D.D.D (lsblock) where D = decimal base number) or
 *     a 4-element array of decimal base numbers (int type)
 *     where element index 0 = msblock and element index IP_BLOCK_MAX = lsblock
 */
public class IPAddress
{
    /* constants */
    private final Logger zLog = Logger.getLogger(getClass());

    protected static final String IP_OCTSEP_STR = ".";
    protected static final String ALLBITS__STR = "255";
    protected static final String ALLBITS_SYM_STR = "*";

    protected static final int IP_BLOCK_MAX = 4; /* IPv4, # of blocks */
    protected static final int IP_BITS_PER_BLOCK = 8; /* IPv4 */

    protected static final int IP_NOBITS_VAL = 0; /* IPv4 */
    protected static final int IP_ALLBITS_VAL = 255; /* IPv4 */
    protected static final int IP_MSBBIT_VAL = 0x80; /* IPv4 */
    protected static final int IP_LSBBIT_VAL = 0x01; /* IPv4 */

    /* ??? need to identify the class of an IP address */
    private static final int IP_CLASSA_BITS = 0; /* msblock = 0XXX XXXX */
    private static final int IP_CLASSB_BITS = 128; /* msblock = 10XX XXXX */
    private static final int IP_CLASSC_BITS = 192; /* msblock = 110X XXXX */
    private static final int IP_CLASSD_BITS = 224; /* msblock = 1110 XXXX */
    private static final int IP_CLASSE_BITS = 240; /* msblock = 1111 XXXX */

    private static final int IP_CLASSA_LOVAL = IP_CLASSA_BITS;
    private static final int IP_CLASSA_HIVAL = IP_CLASSB_BITS - 1;
    private static final int IP_CLASSB_LOVAL = IP_CLASSB_BITS;
    private static final int IP_CLASSB_HIVAL = IP_CLASSC_BITS - 1;
    private static final int IP_CLASSC_LOVAL = IP_CLASSC_BITS;
    private static final int IP_CLASSC_HIVAL = IP_CLASSD_BITS - 1;
    private static final int IP_CLASSD_LOVAL = IP_CLASSD_BITS;
    private static final int IP_CLASSD_HIVAL = IP_CLASSE_BITS - 1;
    private static final int IP_CLASSE_LOVAL = IP_CLASSE_BITS;
    private static final int IP_CLASSE_HIVAL = IP_ALLBITS_VAL;

    /* class variables */

    /* instance variables */
    InetAddress zInetAddr;
    byte asbAddr[]; /* IP address, IPv4 */

    /* constructors */
    public IPAddress()
    {
        asbAddr = new byte[IP_BLOCK_MAX];
        zInetAddr = null;
    }

    /* public methods */
    /* zAddr must be in a string in IPv4 notation
     * (where each address block represents a decimal-based number [0,255])
     */
    public void toAddr(String zAddr) throws IPAddrException
    {
        if (null == zAddr)
        {
            throw new IPAddrException("No IP address string");
        }

        toAddrPriv(asbAddr, zAddr);
        toInetAddrPriv();
        return;
    }

    /* aiAddr must contain IP_BLOCK_MAX elements,
     * aiAddr[0] must contain highest order block and
     * aiAddr[3] must contain lowest order block
     */
    public void toAddr(int aiAddr[]) throws IPAddrException
    {
        toAddrPriv(asbAddr, aiAddr);
        toInetAddrPriv();
        return;
    }

    public byte[] toBytes(String zAddr)
    {
        try
        {
            toAddr(zAddr);
        }
        catch (IPAddrException e)
        {
            zLog.error("Cannot convert " + zAddr + ": " + e.toString());
            return null;
        }

        return asbAddr;
    }

    public byte[] getBytes()
    {
        return asbAddr;
    }

    public InetAddress getIPAddress()
    {
        return zInetAddr;
    }

    public String toString()
    {
        return toAddrStringPriv(asbAddr);
    }

    /* private methods */
    protected void toAddrPriv(byte asbAddr[], String zAddr) throws IPAddrException
    {
        /* decode zAddr into IPv4 blocks */
        StringTokenizer zTokens = new StringTokenizer(zAddr, IP_OCTSEP_STR);
        Integer zTmp;
        int iIdx = 0;
        int iOctet;

        if (IP_BLOCK_MAX != zTokens.countTokens())
        {
            throw new IPAddrException(zAddr + " is not in valid IPv4 dotted-decimal notation");
        }

        while (true == zTokens.hasMoreTokens())
        {
            try
            {
                zTmp = new Integer(zTokens.nextToken());
            }
            catch(NumberFormatException e)
            {
                throw new IPAddrException(zAddr + " contains address block that does not convert to a decimal number");
            }

            if (true == zTmp.equals(ALLBITS_SYM_STR))
            {
                iOctet = IP_ALLBITS_VAL; /* wildcard char = all bits */
            }
            else
            {
                iOctet = zTmp.intValue();
            }

            if (IP_NOBITS_VAL > iOctet ||
                IP_ALLBITS_VAL < iOctet)
            {
                /* component value is not in valid range */
                throw new IPAddrException("IP address (" + zAddr + ") contains block address value (" + iOctet + ") that is out of range [" + IP_NOBITS_VAL + "," + IP_ALLBITS_VAL + "]");
            }

            asbAddr[iIdx] = (byte) iOctet;
            iIdx++;
        }

        for (; IP_BLOCK_MAX > iIdx; iIdx++)
        {
            /* zero out remaining blocks */
            asbAddr[iIdx] = 0;
        }

        return;
    }

    protected void toAddrPriv(byte asbAddr[], int aiAddr[]) throws IPAddrException
    {
        for (int iIdx = 0; IP_BLOCK_MAX > iIdx; iIdx++)
        {
            if (IP_NOBITS_VAL > aiAddr[iIdx] ||
                IP_ALLBITS_VAL < aiAddr[iIdx])
            {
                /* component value is not in valid range */
                throw new IPAddrException("IP address contains address block value (" + aiAddr[iIdx] + ") that is out of range [" + IP_NOBITS_VAL + "," + IP_ALLBITS_VAL + "]");
            }

            asbAddr[iIdx] = (byte) aiAddr[iIdx];
        }

        return;
    }

    protected void toInetAddrPriv() throws IPAddrException
    {
        try
        {
            zInetAddr = InetAddress.getByAddress(asbAddr);
        }
        catch (UnknownHostException e)
        {
            throw new IPAddrException("IP address " + this + " does not specify a valid server");
        }

        return;
    }

    protected String toAddrStringPriv(byte asbAddr[])
    {
        String zAddr = "";

        for (int iIdx = 0; IP_BLOCK_MAX > iIdx; iIdx++)
        {
            if ((IP_BLOCK_MAX - 1) > iIdx)
            {
                zAddr = zAddr + Byte.toString(asbAddr[iIdx]) + IP_OCTSEP_STR;
            }
            else
            {
                zAddr = zAddr + Byte.toString(asbAddr[iIdx]);
            }
        }

        return zAddr;
    }
}
