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

import java.lang.Class;
import java.util.*;

public class BinaryOptNode implements ExpNode
{
    /* constants */

    /* class variables */

    /* instance variables */
    ExpNode zLeftNode;
    ExpNode zRightNode;
    int iIdOperator; /* operator = "value type" */

    /* constructors */
    /* builds binary operator node */
    public BinaryOptNode(int iIdOperator, ExpNode zLeftNode, ExpNode zRightNode)
    {
        this.zLeftNode = zLeftNode;
        this.zRightNode = zRightNode;
        this.iIdOperator = iIdOperator;
    }

    /* public methods */
    /* evaluates binary operator node */
    public Object eval(Hashtable zEnv) throws EvalException
    {
        Object zLeftValue = zLeftNode.eval(zEnv);
        Object zRightValue = zRightNode.eval(zEnv);

        /* check that the operand types are compatible
         * regardless of what the types are
         * (if one or both operands are variables,
         *  we only need to know if the operands are compatible for now
         *  - we may more strictly check the operand types later)
         */
        Class zLeftClass = zLeftValue.getClass(); 
        Class zRightClass = zRightValue.getClass();

        if (false == zLeftClass.isAssignableFrom(zRightClass))
        {
            throw new EvalException("Incompatible operand value(s) (L=\"" + 
                                    zLeftNode.toValTypeString(zEnv) + "\" \"" +
                                    toValTypeString(zEnv) + "\" R=\"" +
                                    zRightNode.toValTypeString(zEnv) + "\")");
        }

        boolean bResult;

        if (true == ExpOperators.isCond(iIdOperator))
        {
            if (false == (zLeftValue instanceof Boolean) ||
                false == (zRightValue instanceof Boolean))
            {
                throw new EvalException("Invalid operand value(s) (L=\"" +
                                        zLeftNode.toValTypeString(zEnv) + "\" \"" +
                                        toValTypeString(zEnv) + "\" R=\"" +
                                        zRightNode.toValTypeString(zEnv) +
                                        "\") - conditional operator operation requires boolean operands");
            }

            /* okay - it's safe to cast the object to its true type */
            boolean bLeft = ((Boolean) zLeftValue).booleanValue();
            boolean bRight = ((Boolean) zRightValue).booleanValue();

            switch(iIdOperator)
            {
            case ExpOperators.AND_ID:
                bResult = (true == bLeft && true == bRight) ? true : false;
                break;

            case ExpOperators.OR_ID:
                bResult = (true == bLeft || true == bRight) ? true : false;
                break;

            case ExpOperators.NULL_ID:
            default:
                bResult = false;
            }
        }
        else if (true == ExpOperators.isRel(iIdOperator))
        {
            if (false == (zLeftValue instanceof Integer) ||
                false == (zRightValue instanceof Integer))
            {
                throw new EvalException("Invalid operand value(s) (L=\"" +
                                        zLeftNode.toValTypeString(zEnv) + "\" \"" +
                                        toValTypeString(zEnv) + "\" R=\"" +
                                        zRightNode.toValTypeString(zEnv) +
                                        "\") - relational operator operation requires integer operands");
            }

            /* okay - it's safe to cast the object to its true type */
            int iLeft = ((Integer) zLeftValue).intValue();
            int iRight = ((Integer) zRightValue).intValue();

            switch(iIdOperator)
            {
            case ExpOperators.GRTHAN_ID:
                bResult = (iLeft > iRight) ? true : false;
                break;

            case ExpOperators.GREQUAL_ID:
                bResult = (iLeft >= iRight) ? true : false;
                break;

            case ExpOperators.LSTHAN_ID:
                bResult = (iLeft < iRight) ? true : false;
                break;

            case ExpOperators.LSEQUAL_ID:
                bResult = (iLeft <= iRight) ? true : false;
                break;

            case ExpOperators.NULL_ID:
            default:
                bResult = false;
            }
        }
        else if (true == ExpOperators.isEq(iIdOperator))
        {
            /* we already know that the operands are compatible
             * but we don't need to know their true types
             * because the equals method will deal with it
             */

            /* note that we don't ignore case when we compare string literals
             * because by definition, string literals must be handled "as is"
             */
            switch(iIdOperator)
            {
            case ExpOperators.EQUAL_ID:
                bResult = (true == zLeftValue.equals(zRightValue)) ? true : false;
                break;

            case ExpOperators.NOTEQUAL_ID:
                bResult = (false == zLeftValue.equals(zRightValue)) ? true : false;
                break;

            case ExpOperators.NULL_ID:
            default:
                bResult = false;
            }
        }
        else if (true == ExpOperators.isMembOf(iIdOperator))
        {
            if (false == (zLeftValue instanceof String) ||
                false == (zRightValue instanceof String))
            {
                throw new EvalException("Invalid operand value(s) (L=\"" +
                                        zLeftNode.toValTypeString(zEnv) + "\" \"" +
                                        toValTypeString(zEnv) + "\" R=\"" +
                                        zRightNode.toValTypeString(zEnv) +
                                        "\") - set member operator operation requires string operands");
            }

            String zLeft = (String) zLeftValue;
            String zRight = (String) zRightValue;

            try
            {
                IPAddress zIPAddr = new IPAddress();
                SNAddress zSNAddr = new SNAddress();

                /* memberof operator syntax identifies
                 * left operand as an IP address and
                 * right operand as a subnet (SN) address
                 * (subnet address = base + network prefix)
                 */
                zIPAddr.toAddr(zLeft);
                zSNAddr.toAddr(zRight);

                bResult = zSNAddr.containsAddr(zIPAddr);

                //if (false == bResult)
                //{
                    //System.out.println(zIPAddr + " is not member of " + zSNAddr + " subnet");
                //}
            }
            catch (IPAddrException e)
            {
                throw new EvalException("Unable to evaluate (L=\"" +
                                        zLeftNode.toValTypeString(zEnv) + "\" \"" +
                                        toValTypeString(zEnv) + "\" R=\"" +
                                        zRightNode.toValTypeString(zEnv) + "\") - operands do not evaluate to IP addresses (" + e.toString() + ")");
            }
        }
        else
        {
            bResult = false;
        }

        /* value of a binary operation node is the result of the operation
         * between the left and right operand nodes
         */
        return Boolean.valueOf(bResult);
    }

    /* find constant value of VarNode operation */
    public Object findConstValue(VarNode zVarNode)
    {
        Object zValue = null;

        if (true == zLeftNode.equals(zVarNode) &&
            true == (zRightNode instanceof ConstNode))
        {
            /* if left node is specified VarNode and
             * right node is ConstNode;
             * retrieve value of ConstNode
             */
            zValue = zRightNode.findConstValue(zVarNode);
        }
        else if (true == zRightNode.equals(zVarNode) &&
                 true == (zLeftNode instanceof ConstNode))
        {
            /* if right node is specified VarNode and
             * left node is ConstNode;
             * retrieve value of ConstNode
             */
            zValue = zLeftNode.findConstValue(zVarNode);
        }
        else /* continue search */
        {
            /* first search left node if it is not ConstNode */
            if (false == (zLeftNode instanceof ConstNode))
            {
                zValue = zLeftNode.findConstValue(zVarNode);
            }
            /* else left node is ConstNode */

            /* if search of left node failed or
             * left node is ConstNode,
             * then search right node if it is not ConstNode
             */
            if (null == zValue &&
                false == (zRightNode instanceof ConstNode))
            {
                zValue = zRightNode.findConstValue(zVarNode);
            }
            /* right node is ConstNode */
        }

        return zValue;
    }

    public int getValType()
    {
        return BOOL_VAL;
    }

    public int getNodeType()
    {
        return BINARYOPT_NODE;
    }

    public String toValTypeString(Hashtable zEnv)
    {
         return ExpOperators.toShort(iIdOperator);
    }

    /* dumps contents of binary operator node */
    public void dump(Hashtable zEnv, int iIndent, char cNodePos)
    {
        int iIdx = 0;

        while(iIdx++ < iIndent)
            System.out.print("-");

        System.out.println(cNodePos + " Operator (\"" + toValTypeString(zEnv) + "\") node");
        zLeftNode.dump(zEnv, iIdx, LEFTNODE_PR);
        zRightNode.dump(zEnv, iIdx, RIGHTNODE_PR);
        return;
    }

    public void dump(Hashtable zEnv, int iNodeFormat)
    {
        switch(iNodeFormat)
        {
        case PREFIX_PR:
        default:
            System.out.print("( " + toValTypeString(zEnv) + " ");
            zLeftNode.dump(zEnv, PREFIX_PR);
            zRightNode.dump(zEnv, PREFIX_PR);
            System.out.print(") ");
            break;

        case INFIX_PR:
            System.out.print("( ");
            zLeftNode.dump(zEnv, INFIX_PR);
            System.out.print(toValTypeString(zEnv) + " ");
            zRightNode.dump(zEnv, INFIX_PR);
            System.out.print(") ");
            break;

        case POSTFIX_PR:
            System.out.print("( ");
            zLeftNode.dump(zEnv, POSTFIX_PR);
            zRightNode.dump(zEnv, POSTFIX_PR);
            System.out.print(toValTypeString(zEnv) + " ) ");
            break;
        }

        return;
    }

    /* private methods */
}
