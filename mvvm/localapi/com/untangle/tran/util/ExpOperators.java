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

/* ExpOperators class stores no state (e.g., has no variables)
 * so all of its methods are static (e.g., class methods) and
 * can be called without reference to a particular object.
 */
public class ExpOperators
{
    /* constants */
    public static final String AND_STR = "AND";
    public static final String OR_STR = "OR";
    public static final String NOT_STR = "NOT";
    public static final String GRTHAN_STR = "GRTHAN";
    public static final String GREQUAL_STR = "GREQUAL";
    public static final String LSTHAN_STR = "LSTHAN";
    public static final String LSEQUAL_STR = "LSEQUAL";
    public static final String EQUAL_STR = "EQUAL";
    public static final String NOTEQUAL_STR = "NOTEQUAL";
    public static final String MEMBEROF_STR = "MEMBEROF";

    public static final String AND_SYMB = "&&";
    public static final String OR_SYMB = "||";
    public static final String NOT_SYMB = "!";
    public static final String GRTHAN_SYMB = ">";
    public static final String GREQUAL_SYMB = ">=";
    public static final String LSTHAN_SYMB = "<";
    public static final String LSEQUAL_SYMB = "<=";
    public static final String EQUAL_SYMB = "==";
    public static final String NOTEQUAL_SYMB = "!=";
    public static final String MEMBEROF_SYMB = ">mof>";

    public static final char POS_CHAR = '+'; /* positive sign */
    public static final char NEG_CHAR = '-'; /* negative sign */

    public static final int NULL_ID = 0;
    public static final int AND_ID = 1;
    public static final int OR_ID = 2;
    public static final int NOT_ID = 3;
    public static final int GRTHAN_ID = 4;
    public static final int GREQUAL_ID = 5;
    public static final int LSTHAN_ID = 6;
    public static final int LSEQUAL_ID = 7;
    public static final int EQUAL_ID = 8;
    public static final int NOTEQUAL_ID = 9;
    public static final int MEMBEROF_ID = 10;

    public static final String TRUE_STR = "TRUE";
    public static final String FALSE_STR = "FALSE";

    public static final char STR_LITERAL_CHR = '\"';

    /* class variables */

    /* instance variables */

    /* constructors */
    private ExpOperators() {}

    /* public methods */
    public static boolean isShortOpt(String zTest)
    {
        if (true == zTest.equals(AND_SYMB) ||
            true == zTest.equals(OR_SYMB) ||
            true == zTest.equals(NOT_SYMB) ||
            true == zTest.equals(GRTHAN_SYMB) ||
            true == zTest.equals(GREQUAL_SYMB) ||
            true == zTest.equals(LSTHAN_SYMB) ||
            true == zTest.equals(LSEQUAL_SYMB) ||
            true == zTest.equals(EQUAL_SYMB) ||
            true == zTest.equals(NOTEQUAL_SYMB) ||
            true == zTest.equals(MEMBEROF_SYMB))
        {
            return true;
        }

        return false;
    }

    public static boolean isCond(int iTest)
    {
        if (AND_ID == iTest ||
            OR_ID == iTest)
        {
            return true; /* is conditional operator */
        }

        return false;
    }

    public static boolean isRel(int iTest)
    {
        if (GRTHAN_ID == iTest ||
            GREQUAL_ID == iTest ||
            LSTHAN_ID == iTest ||
            LSEQUAL_ID == iTest)
        {
            return true; /* is relational operator */
        }

        return false;
    }

    public static boolean isEq(int iTest)
    {
        if (EQUAL_ID == iTest ||
            NOTEQUAL_ID == iTest)
        {
            return true; /* is equality operator */
        }

        return false;
    }

    public static boolean isMembOf(int iTest)
    {
        if (MEMBEROF_ID == iTest)
        {
            return true; /* is set member operator */
        }

        return false;
    }

    /* from STR to SYMB - overloaded */
    public static String toShort(String zSrc)
    {
        if (true == zSrc.equals(AND_STR))
        {
            return AND_SYMB;
        }
        else if (true == zSrc.equals(OR_STR))
        {
            return OR_SYMB;
        }
        else if (true == zSrc.equals(NOT_STR))
        {
            return NOT_SYMB;
        }
        else if (true == zSrc.equals(GRTHAN_STR))
        {
            return GRTHAN_SYMB;
        }
        else if (true == zSrc.equals(GREQUAL_STR))
        {
            return GREQUAL_SYMB;
        }
        else if (true == zSrc.equals(LSTHAN_STR))
        {
            return LSTHAN_SYMB;
        }
        else if (true == zSrc.equals(LSEQUAL_STR))
        {
            return LSEQUAL_SYMB;
        }
        else if (true == zSrc.equals(EQUAL_STR))
        {
            return EQUAL_SYMB;
        }
        else if (true == zSrc.equals(NOTEQUAL_STR))
        {
            return NOTEQUAL_SYMB;
        }
        else if (true == zSrc.equals(MEMBEROF_STR))
        {
            return MEMBEROF_SYMB;
        }
        else
        {
            return null;
        }
    }

    /* from ID to SYMB - overloaded */
    public static String toShort(int iSrc)
    {
        if (AND_ID == iSrc)
        {
            return AND_SYMB;
        }
        else if (OR_ID == iSrc)
        {
            return OR_SYMB;
        }
        else if (NOT_ID == iSrc)
        {
            return NOT_SYMB;
        }
        else if (GRTHAN_ID == iSrc)
        {
            return GRTHAN_SYMB;
        }
        else if (GREQUAL_ID == iSrc)
        {
            return GREQUAL_SYMB;
        }
        else if (LSTHAN_ID == iSrc)
        {
            return LSTHAN_SYMB;
        }
        else if (LSEQUAL_ID == iSrc)
        {
            return LSEQUAL_SYMB;
        }
        else if (EQUAL_ID == iSrc)
        {
            return EQUAL_SYMB;
        }
        else if (NOTEQUAL_ID == iSrc)
        {
            return NOTEQUAL_SYMB;
        }
        else if (MEMBEROF_ID == iSrc)
        {
            return MEMBEROF_SYMB;
        }
        else
        {
            return null;
        }
    }

    /* from SYMB to ID */
    public static int toId(String zSrc)
    {
        if (true == zSrc.equals(AND_SYMB))
        {
            return AND_ID;
        }
        else if (true == zSrc.equals(OR_SYMB))
        {
            return OR_ID;
        }
        else if (true == zSrc.equals(NOT_SYMB))
        {
            return NOT_ID;
        }
        else if (true == zSrc.equals(GRTHAN_SYMB))
        {
            return GRTHAN_ID;
        }
        else if (true == zSrc.equals(GREQUAL_SYMB))
        {
            return GREQUAL_ID;
        }
        else if (true == zSrc.equals(LSTHAN_SYMB))
        {
            return LSTHAN_ID;
        }
        else if (true == zSrc.equals(LSEQUAL_SYMB))
        {
            return LSEQUAL_ID;
        }
        else if (true == zSrc.equals(EQUAL_SYMB))
        {
            return EQUAL_ID;
        }
        else if (true == zSrc.equals(NOTEQUAL_SYMB))
        {
            return NOTEQUAL_ID;
        }
        else if (true == zSrc.equals(MEMBEROF_SYMB))
        {
            return MEMBEROF_ID;
        }
        else
        {
            return NULL_ID;
        }
    }

    /* private methods */
}
