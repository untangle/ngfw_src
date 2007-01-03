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

/* one instance of the VarNode object exists for each unique variable
 * within an expression
 * - when an expression references a single variable multiple times,
 *   the expression tree will use a single instance of the VarNode object and
 *   refer to this instance in every leaf that references the variable
 *
 * currently, for thread safety,
 * each thread must use its own instance of an ExpTree object so that
 * when a thread evaluates an expression,
 * the evaluation of (e.g., the assignment of a value to) a variable
 * within the expression tree
 * doesn't cause collisions/conflicts with another thread
 * that is simultaneously evaluating the same expression
 * (e.g., if threads use separate instances of the same ExpTree object,
 *  then we have no problem)
 * - see super usage and comments in eval, toValTypeString, and dump
 */
public class VarNode extends ConstNode
{
    /* constants */

    /* class variables */

    /* instance variables */
    String zName;

    /* constructors */
    /* implements placeholder when building an expression tree */
    public VarNode(String zName)
    {
        /* explicitly use ConstNode() constructor for VarNode constructor */
        super();
        this.zName = zName;
    }

    /* public methods */
//     public Object eval() throws EvalException
//     {
//         if (null != super.zValue)
//         {
//             return super.zValue; /* value of a variable node is its value */
//         }
//         else
//         {
//             throw new EvalException(zName + " variable has no assigned value - cannot evaluate variable");
//         }
//     }

    /* value of a variable node is in zEnv (e.g., is a look up value)
     * - this run-time evaluation uses just-in-time look ups
     *   - if an expression references the same variable multiple times,
     *     we have to look up the same value multiple times
     *     - if we saved the result of the look up for each variable and
     *       checked that we already have a result,
     *       we can avoid repetitive look ups (plus)
     *       but we can't reset the result and
     *       reuse the node with a new zEnv (minus)
     *     - also, we have to wait until we evaluate each variable
     *       before we can identify a variable's type (minus)
     *       (see BinaryOptNode.eval and UnaryOptNode.eval)
     *       - this means that if we create an equals method that
     *         compares two nodes,
     *         it would require that we evaluate the nodes
     *         (because the value of a variable node doesn't exist
     *          within the node)
     *         in order to compare them
     * - with compile-time evaluation,
     *   we can attempt to identify a variable's Object type as
     *   we set the value of each variable,
     *   create an equals method that compares two nodes, and
     *   check if the user specifies a variable (and value) that
     *   isn't present in an expression
     *   (and warn the user of this problem)
     * - we require that each variable value exists in zEnv
     *   as a corresponding reference object
     *   (boolean value exists as Boolean object,
     *    integer value exists as Integer object,
     *    string literal value exists as String object)
     */
    public Object eval(Hashtable zEnv) throws EvalException
    {
        if (null == zEnv)
            throw new EvalException(zName + " variable has no value - environment does not exist");

        Object zTmp = zEnv.get(zName);

        if (null == zTmp)
            throw new EvalException(zName + " variable has no value - variable does not exist in environment");

        return zTmp;
    }

    public Object findConstValue(VarNode zVarNode)
    {
        return null;
    }

    public int getValType()
    {
        /* with run-time evaluation,
         * we don't know the variable type
         * until we evaluate (look up) the variable's value
         * so we have to hard-code a type for now
         */
        return VAR_VAL;
    }

    public int getNodeType()
    {
        return VAR_NODE;
    }

    public String toValTypeString(Hashtable zEnv)
    {
        if (null == super.zValType)
        {
            Object zTmp = zEnv.get(zName);

            if (null == zTmp)
            {
                super.zValType = UNKNOWN_STR;
            }
            else if (true == (zTmp instanceof Boolean))
            {
                super.zValType = BOOL_STR;
            }
            else if (true == (zTmp instanceof Integer))
            {
                super.zValType = INT_STR;
            }
            else if (true == (zTmp instanceof String))
            {
                super.zValType = STRLIT_STR;
            }
            else
            {
                super.zValType = UNKNOWN_STR;
            }
        }
        /* else we already identified and saved the type of this value */

        return super.zValType;
    }

    public void dump(Hashtable zEnv, int iIndent, char cNodePos)
    {
        int iIdx = 0;

        while(iIdx++ < iIndent)
        {
            System.out.print("-");
        }

        Object zTmp = zEnv.get(zName);

        if (null != zTmp)
        {
            if (true == (zTmp instanceof String))
            {
                System.out.println(cNodePos + " Leaf (" + zName + ":\"" + zTmp + "\") node");
            }
            else
            {
                System.out.println(cNodePos + " Leaf (" + zName + ":" + zTmp + ") node");
            }
        }
        else
        {
            System.out.println(cNodePos + " Leaf (" + zName + ":<null>) node");
        }

        return;
    }

    public void dump(Hashtable zEnv, int iNodeFormat)
    {
        Object zTmp = zEnv.get(zName);

        if (null != zTmp)
        {
            if (true == (zTmp instanceof String))
            {
                System.out.print("(" + zName + ":\"" + zTmp + "\") ");
            }
            else
            {
                System.out.print("(" + zName + ":" + zTmp + ") ");
            }
        }
        else
        {
            System.out.print("(" + zName + ":<null>) ");
        }

        return;
    }

//    /* for compile-time evaluation,
//     * setVarNode mirrors ConstNode constructors
//     */
//    /* ??? not defined in ExpNode interface */
//    public void setVarNode(Boolean zValue)
//    {
//        /* value of variable must be set before variable can be evaluated */
//        super.zValue = (Object) zValue;
//        super.zValType = BOOL_STR;
//        super.iValType = BOOL_VAL;
//        return;
//    }

//    /* ??? not defined in ExpNode interface */
//    public void setVarNode(Integer zValue)
//    {
//        /* value of variable must be set before variable can be evaluated */
//        super.zValue = (Object) zValue;
//        super.zValType = INT_STR;
//        super.iValType = INT_VAL;
//        return;
//    }

//    /* ??? not defined in ExpNode interface */
//    public void setVarNode(String zValue)
//    {
//        /* value of variable must be set before variable can be evaluated */
//        super.zValue = (Object) zValue;
//        super.zValType = STRLIT_STR;
//        super.iValType = STRLIT_VAL;
//        return;
//    }

    /* private methods */
}
