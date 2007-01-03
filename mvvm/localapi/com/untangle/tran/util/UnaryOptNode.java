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

public class UnaryOptNode implements ExpNode
{
    /* constants */

    /* class variables */

    /* instance variables */
    ExpNode zNode;
    int iIdOperator; /* operator = "value type" */

    /* constructors */
    /* builds unary operator node */
    public UnaryOptNode(int iIdOperator, ExpNode zNode)
    {
        this.iIdOperator = iIdOperator;
        this.zNode = zNode;
    }

    /* public methods */
    /* evaluates unary operator node */
    public Object eval(Hashtable zEnv) throws EvalException
    {
        Object zValue = zNode.eval(zEnv);

        if (false == (zValue instanceof Boolean))
        {
            throw new EvalException("Invalid operand value (\"" +
                                    toValTypeString(zEnv) + "\" \"" +
                                    zNode.toValTypeString(zEnv) +
                                    "\") - logical operator operation requires boolean operand");
        }

        /* okay - it's safe to cast the object to its true type */
        boolean bNodeValue = ((Boolean) zValue).booleanValue();

        boolean bResult;

        switch(iIdOperator)
        {
        case ExpOperators.NOT_ID:
            bResult = (false == bNodeValue) ? true : false;
            break;

        case ExpOperators.NULL_ID:
        default:
            bResult = false;
            break;
        }

        /* value of an unary operation node is the result of the operation
         * on the operand node
         */
        return Boolean.valueOf(bResult);
    }

    public Object findConstValue(VarNode zVarNode)
    {
        return null;
    }

    public int getValType()
    {
        return BOOL_VAL;
    }

    public int getNodeType()
    {
        return UNARYOPT_NODE;
    }

    public String toValTypeString(Hashtable zEnv)
    {
        return ExpOperators.toShort(iIdOperator);
    }

    /* dumps contents of unary operator node */
    public void dump(Hashtable zEnv, int iIndent, char cNodePos)
    {
        int iIdx = 0;

        while(iIdx++ < iIndent)
            System.out.print("-");

        System.out.println("-" + cNodePos + " Operator (\"" + ExpOperators.toShort(iIdOperator) + "\") node");
        zNode.dump(zEnv, iIdx, ' ');
        return;
    }

    public void dump(Hashtable zEnv, int iNodeFormat)
    {
        switch(iNodeFormat)
        {
        case PREFIX_PR:
        case INFIX_PR:
        default:
            System.out.print("( " + ExpOperators.toShort(iIdOperator) + " ");
            zNode.dump(zEnv, iNodeFormat);
            System.out.print(") ");
            break;

        case POSTFIX_PR:
            System.out.print("( ");
            zNode.dump(zEnv, POSTFIX_PR);
            System.out.print(ExpOperators.toShort(iIdOperator) + " ) ");
            break;
        }

        return;
    }

    /* private methods */
}
