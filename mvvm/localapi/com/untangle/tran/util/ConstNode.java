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

import java.util.*;

public class ConstNode implements ExpNode
{
    /* constants */

    /* class variables */

    /* instance variables */
    Object zValue;
    String zValType; /* value type */
    int iValType;
    int iNodeType;

    /* constructors */
    public ConstNode()
    {
        this.zValue = null; /* no value - this is not a constant/fixed value */
        this.zValType = null;
        this.iValType = UNKNOWN_VAL;
    }

    public ConstNode(Boolean zValue)
    {
        this.zValue = (Object) zValue;
        this.zValType = BOOL_STR;
        this.iValType = BOOL_VAL;
    }

    public ConstNode(Integer zValue)
    {
        this.zValue = (Object) zValue;
        this.zValType = INT_STR;
        this.iValType = INT_VAL;
    }

    public ConstNode(String zValue)
    {
        this.zValue = (Object) zValue;
        this.zValType = STRLIT_STR;
        this.iValType = STRLIT_VAL;
    }

    public ConstNode(Object zValue)
    {
        this.zValue = zValue;
        if (true == (zValue instanceof Boolean))
        {
            this.zValType = BOOL_STR;
            this.iValType = BOOL_VAL;
        }
        else if (true == (zValue instanceof Integer))
        {
            this.zValType = INT_STR;
            this.iValType = INT_VAL;
        }
        else if (true == (zValue instanceof String))
        {
            this.zValType = STRLIT_STR;
            this.iValType = STRLIT_VAL;
        }
        else
        {
            this.zValType = UNKNOWN_STR;
            this.iValType = UNKNOWN_VAL;
        }
    }

    /* public methods */
    public Object eval(Hashtable zEnv) throws EvalException
    {
        return zValue; /* value of a constant node is its value */
    }

    public Object findConstValue(VarNode zVarNode)
    {
        return zValue; /* value of a constant node is its value */
    }

    public int getValType()
    {
        return iValType;
    }

    public int getNodeType()
    {
        return CONST_NODE;
    }

    public String toValTypeString(Hashtable zEnv)
    {
        return zValType;
    }

    public void dump(Hashtable zEnv, int iIndent, char cNodePos)
    {
        int iIdx = 0;

        while(iIdx++ < iIndent)
        {
            System.out.print("-");
        }

        if (STRLIT_VAL == iValType)
        {
            System.out.println(cNodePos + " Leaf (\"" + zValue + "\") node");
        }
        else
        {
            System.out.println(cNodePos + " Leaf (" + zValue + ") node");
        }

        return;
    }

    public void dump(Hashtable zEnv, int iNodeFormat)
    {
        if (STRLIT_VAL == iValType)
        {
            System.out.print("\"" + zValue + "\" ");
        }
        else
        {
            System.out.print(zValue + " ");
        }

        return;
    }

    /* private methods */
}
