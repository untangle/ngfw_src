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

import java.lang.Number;
import java.util.*;

/* subnet address defines the address range of a subnet
 * - input must be a series of two IP addresses
 *   - represented as "base IP address / network prefix"
 *     where network prefix = CIDR or subnet mask
 */
public class SNAddress extends IPAddress
{
    /* constants */
    private static final String NP_SLASHSEP_STR = "/";
    private static final String NP_ALLONES_STR = IP_ALLBITS_VAL + IP_OCTSEP_STR + IP_ALLBITS_VAL + IP_OCTSEP_STR + IP_ALLBITS_VAL + IP_OCTSEP_STR + IP_ALLBITS_VAL; /* "255.255.255.255" */

    private static final int SN_ADDR_CHUNK_MAX = 2; /* # of addr components */

    /* CIDR values of < /0 and > /32 are invalid network prefixes
     * - CIDR < /0 = invalid (negative) value
     * - CIDR /0 = network address (no host on subnet) (valid but meaningless)
     * - CIDR > /32 = exceeds 32 bit limit (of 4 blocks of 8-bits per block)
     * - CIDR equivalents of classes:
     *    - CIDR /8 = Class A (and no subnet)
     *    - CIDR /16 = Class B (and no subnet)
     *    - CIDR /32 = Class C (and no subnet)
     */
    private static final int NP_CIDR_BITS_MIN = 0;
    private static final int NP_CIDR_BITS_MAX = 32;

    /* class variables */

    /* instance variables */
    byte asbNPAddr[]; /* network prefix, IPv4 */
    byte asbSNAddr[]; /* subnet address, IPv4 */

    /* constructors */
    public SNAddress()
    {
        super(); /* construct subnet base */
        asbNPAddr = new byte[IP_BLOCK_MAX];
        asbSNAddr = new byte[IP_BLOCK_MAX];
    }

    /* public methods */
    /* zSNAddr must be in a string in IPv4 notation
     * (where each address block represents a decimal-based number [0,255],
     *  specifically, "D.D.D.D", "D.D.D.D/D.D.D.D" (subnet mask),
     *  or "D.D.D.D/D2" (CIDR) notation
     * (where D = decimal digit ([0-255]), D2 = decimal digit ([13-27]))
     *
     * SNAddress does not support these valid notations for a subnet mask:
     * "D.D.D.D/D", "D.D.D.D/D.D", and "D.D.D.D/D.D.D"
     *
     * subnet address = IP address/network prefix
     */
    public void toAddr(String zSNAddr) throws IPAddrException
    {
        if (null == zSNAddr)
        {
            throw new IPAddrException("Subnet address string is null");
        }

        StringTokenizer zSNTokens = new StringTokenizer(zSNAddr, NP_SLASHSEP_STR);
        int iCnt = zSNTokens.countTokens();

        if (0 == iCnt ||
            SN_ADDR_CHUNK_MAX < iCnt)
        {
            throw new IPAddrException("Subnet address has too few or many address components");
        }

        /* set base IP address */
        super.toAddrPriv(super.asbAddr, zSNTokens.nextToken());

        /* set network prefix */
        if (1 == iCnt)
        {
            /* use default prefix
             * - if not specified, then network prefix is implicitly defined
             */
            super.toAddrPriv(asbNPAddr, NP_ALLONES_STR);
        }
        else /* SN_ADDR_CHUNK_MAX */
        {
            String zNPTmp = zSNTokens.nextToken();

            /* reset delim str to process network prefix (CIDR/subnet mask) */
            StringTokenizer zMTokens = new StringTokenizer(zNPTmp, IP_OCTSEP_STR);
            iCnt = zMTokens.countTokens();

            if (0 == iCnt ||
                IP_BLOCK_MAX < iCnt)
            {
                throw new IPAddrException("Network prefix has too few or too many address blocks");
            }

            String zOctet = zMTokens.nextToken();

            if (1 == iCnt)
            {
               Integer zTmp;

                /* CIDR notation
                 * convert CIDR notation to IPv4 notation
                 * - build IPv4 address blocks
                 */
                try
                {
                    zTmp = new Integer(zOctet);
                }
                catch (NumberFormatException e)
                {
                    throw new IPAddrException("CIDR network prefix " + zOctet + " is not a decimal number");
                }

                int iBitCnt = zTmp.intValue();

                if (NP_CIDR_BITS_MIN > iBitCnt ||
                    NP_CIDR_BITS_MAX < iBitCnt)
                {
                    throw new IPAddrException("CIDR network prefix " + zOctet + " is out of range [" + NP_CIDR_BITS_MIN + "," + NP_CIDR_BITS_MAX + "]");
                }

                int iBlockCnt = iBitCnt / IP_BITS_PER_BLOCK;
                int iBitRem = iBitCnt % IP_BITS_PER_BLOCK;
                int iIdx;

                int aiTmp[] = new int[IP_BLOCK_MAX];

                /* initialize blocks starting from msblock to lsblock */
                for (iIdx = 0; IP_BLOCK_MAX > iIdx; iIdx++)
                {
                    if (iBlockCnt > iIdx)
                    {
                        /* use default subnet address */
                        aiTmp[iIdx] = IP_ALLBITS_VAL;
                    }
                    else
                    {
                        /* will reset remaining bits later (if necessary) */
                        aiTmp[iIdx] = 0;
                    }
                }

                if (IP_BLOCK_MAX > iBlockCnt)
                {
                    /* initialize remaining bits starting from msbit to lsbit */
                    int iOctet = IP_MSBBIT_VAL;

                    /* this for-loop could be modified to a switch stmt
                     * if knowledge about IP_BITS_PER_BLOCK is hard-coded
                     * into the switch stmt
                     */
                    for (iIdx = 0; iBitRem > iIdx; iIdx++)
                    {
                        iOctet |= (0x80 >> iIdx);
                    }

                    aiTmp[iBlockCnt] = iOctet;
                }

                super.toAddrPriv(asbNPAddr, aiTmp);
            }
            else /* IP_BLOCK_MAX */
            {
                /* subnet mask notation
                 * (network prefix is already in IPv4 notation)
                 */
                super.toAddrPriv(asbNPAddr, zNPTmp);
            }
        }

        buildSubnetAddr();
        return;
    }

    /* aiAddr may contain IP_BLOCK_MAX (implicit subnet) or
     * (IP_BLOCK_MAX + IP_BLOCK_MAX) (explicit subnet) elements,
     * aiAddr[0] must contain highest order block,
     * aiAddr[3] must contain lowest order block,
     * if present, aiAddr[4] must contain highest order subnet mask block,
     * if present, aiAddr[7] must contain lowest order subnet mask block,
     * - aiAddr[4-7] is subnet mask and never CIDR
     */
    public void toAddr(int[] aiAddr) throws IPAddrException
    {
        /* set base IP address */
        super.toAddrPriv(super.asbAddr, aiAddr);

        int aiNPTmp[] = new int[IP_BLOCK_MAX];

        for (int iIdx = IP_BLOCK_MAX; (IP_BLOCK_MAX + IP_BLOCK_MAX) > iIdx; iIdx++)
        {
            try
            {
                aiNPTmp[iIdx] = aiAddr[iIdx - IP_BLOCK_MAX];
            }
            catch(ArrayIndexOutOfBoundsException e)
            {
                /* subnet mask not explicitly specified
                 * - build default network prefix
                 */
                aiNPTmp[iIdx] = IP_ALLBITS_VAL;
            }
        }

        /* set network prefix */
        super.toAddrPriv(asbNPAddr, aiNPTmp);

        buildSubnetAddr();
        return;
    }

    /* _this_ SNAddr contains zIPAddr */
    public boolean containsAddr(IPAddress zIPAddr)
    {
        boolean bResult = false;

        for (int iIdx = 0; IP_BLOCK_MAX > iIdx; iIdx++)
        {
            if (asbSNAddr[iIdx] == (zIPAddr.asbAddr[iIdx] & asbNPAddr[iIdx]))
            {
                bResult = true;
            }
            else
            {
                bResult = false;
                break;
            }
        }

        return bResult;
    }

    public String toString()
    {
        return super.toAddrStringPriv(super.asbAddr) + NP_SLASHSEP_STR + super.toAddrStringPriv(asbNPAddr);
    }

    /* private methods */
    private void buildSubnetAddr()
    {
        for (int iIdx = 0; IP_BLOCK_MAX > iIdx; iIdx++)
        {
            asbSNAddr[iIdx] = (byte) (super.asbAddr[iIdx] & asbNPAddr[iIdx]);
        }

        return;
    }
}
