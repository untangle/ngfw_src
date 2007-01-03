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

import java.util.StringTokenizer;
import java.util.LinkedList;

public class ExpTokens
{
    /* constants */
    private static final String EXP_DELIM_STR = " ()";
    private static final String LIT_DELIM_STR = "\"";

    /* class variables */

    /* instance variables */

    /* constructors */
    private ExpTokens() {}

    /* public methods */
    public static LinkedList toTokenList(String zArgs) throws TokenException
    {
        if (null == zArgs)
        {
            throw new TokenException("No expression string");
        }

        LinkedList zList = new LinkedList(); /* operator, operand list */
        String zOptorand;
        StringTokenizer zTokens = new StringTokenizer(zArgs, EXP_DELIM_STR);
        int iCnt = 0;
        boolean bVoid;

        /* copies of tokens are persistent */
        while (true == zTokens.hasMoreTokens())
        {
            zOptorand = zTokens.nextToken();

            /* if operator, get short version of operator */
            String zTmp = toShortToken(zOptorand);

            bVoid = zList.add(zTmp); /* add token to end of list */
            iCnt++;
        }

        if (zList.size() != iCnt)
        {
            throw new TokenException("String to token conversion error - linked list corrupted");
        }

        return zList;
    }

    public static String toShortToken(String zToken)
    {
        String zNewToken;

        zNewToken = ExpOperators.toShort(zToken.toUpperCase());
        if (null != zNewToken)
        {
            /* token is an operator - return short version */
        }
        else
        {
            /* token is not an operator - return original */
            zNewToken = zToken;
        }

        return zNewToken;
    }

    /* private methods */
}
