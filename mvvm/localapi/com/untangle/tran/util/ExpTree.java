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

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;

import org.apache.log4j.Logger;

/* expression keywords include
 * - operators (see ExpOperators)
 * - true, false
 * - integer values
 * all other words are treated as variables
 */
public class ExpTree
{
    /* constants */
    private final Logger zLog = Logger.getLogger(getClass());

    private static final int INIT_SZ = 5;

    /* class variables */

    /* instance variables */
    LinkedList zList; /* for convenience */
    Hashtable zVarNodes; /* hashtable of variable nodes */
    ExpNode zTopNode = null;

    /* constructors */
    private ExpTree() {}

    /* public methods */
    public static ExpTree build(String zExp) throws BuildException
    {
        LinkedList zExpList = null;

        try
        {
            zExpList = ExpTokens.toTokenList(zExp);
        }
        catch (TokenException e)
        {
            throw new BuildException("Unable to build expression; cannot parse expression string");
        }

        return buildTree(zExpList);
    }

    public static ExpTree build(LinkedList zExpList) throws BuildException
    {
        return buildTree(zExpList);
    }

    /* evaluate an expression that contains constants and variables
     * - the zEnv hashtable tracks a set of variable values
     *   (where key = variable name, value = object containing variable value)
     *   - if the zEnv hashtable is empty and
     *     the expression doesn't reference any variables,
     *     then continue without error
     */
    public Boolean eval(Hashtable zEnv) throws EvalException
    {
        return (Boolean) zTopNode.eval(zEnv);
    }

//    /* evaluate an expression that contains constants and variables
//     * - the zEnv hashtable tracks a set of variable values
//     *   (where key = variable name, value = object containing variable value)
//     *   - if the zEnv hashtable is empty and
//     *     the expression doesn't reference any variables,
//     *     then continue without error
//     * - the variable values for an expression can be reset
//     *   by re-evaluating the expression with a new zEnv hashtable
//     */
//     public Boolean eval(Hashtable zEnv) throws SetVarException, EvalException
//     {
//         Enumeration zVarKeys = zEnv.keys();
//         VarNode zVarNode;
//         Object zValue;
//         String zName;

//         /* - step through each key (variable name) in the zEnv hashtable and
//          *   retrieve the corresponding value (variable value) for each key
//          *   - if a corresponding variable node for the given variable name
//          *     exists in the zVarNode hashtable:
//          *     - does not exist, then error
//          *     - exists, then set value of the variable node
//          *   - we assume that the user uses the same name (spelling and case)
//          *     for the variable name in both the expression and
//          *     list of variable values
//          */
//         while (true == zVarKeys.hasMoreElements())
//         {
//             zName = (String) zVarKeys.nextElement();

//             zVarNode = (VarNode) zVarNodes.get(zName);
//             if (null == zVarNode)
//             {
//                 throw new SetVarException("Cannot set " + zName + " variable value - variable does not exist within expression");
//             }

//             zValue = zEnv.get(zName);
//             zVarNode.setVarNode(zValue);
//         }

//         return (Boolean) zTopNode.eval();
//     }

    /* find constant value of variable operation
     * - constant value refers to "rvalue" of variable operation and
     *   not to "lvalue" of variable operation
     *   (e.g., "lvalue" is value assigned from zEnv hashtable)
     *   -> if operation expression is "== protocol \"udp\"",
     *      then protocol is "lvalue" and "udp" is "rvalue"
     * - returns 1st constant value occurance of variable and
     *   will not find additional constant value occurances of variable
     */
    public Object findConstValue(String zVarName)
    {
        VarNode zVarNode = (VarNode) zVarNodes.get(zVarName);
        if (null == zVarNode)
        {
            return null;
        }
        /* reminder - we don't want value from this VarNode;
         * we want to find BinaryOptNode that
         * contains VarNode and ConstNode pair and
         * value from this ConstNode
         * (and only use ConstNode from first BinaryOptNode that we find)
         * - note that UnaryOptNode may contain VarNode too but
         *   if so, it does not have ConstNode and
         *   thus, we don't need it
         * - note that we don't evaluate ExpTree here
         */

        return zTopNode.findConstValue(zVarNode);
    }

    public void dumpTree(Hashtable zEnv) throws DumpException
    {
        if (null == zTopNode)
        {
            throw new DumpException("Expression tree contains no expressions");
        }

        zTopNode.dump(zEnv, 0, ExpNode.TOPNODE_PR);
        return;
    }

    public void dumpPrefixStr(Hashtable zEnv) throws DumpException
    {
        if (null == zTopNode)
        {
            throw new DumpException("Expression tree contains no expressions");
        }

        zTopNode.dump(zEnv, ExpNode.PREFIX_PR);
        System.out.println();
        return;
    }

    public void dumpInfixStr(Hashtable zEnv) throws DumpException
    {
        if (null == zTopNode)
        {
            throw new DumpException("Expression tree contains no expressions");
        }

        zTopNode.dump(zEnv, ExpNode.INFIX_PR);
        System.out.println();
        return;
    }

    public void dumpPostfixStr(Hashtable zEnv) throws DumpException
    {
        if (null == zTopNode)
        {
            throw new DumpException("Expression tree contains no expressions");
        }

        zTopNode.dump(zEnv, ExpNode.POSTFIX_PR);
        System.out.println();
        return;
    }

    /* private methods */
    private static ExpTree buildTree(LinkedList zExpList) throws BuildException
    {
        ExpTree zExpTree = new ExpTree();
        zExpTree.zList = zExpList;

        /* a expression tree may contain a set of variable nodes
         * - the zVarNodes hashtable tracks a set of variable nodes
         *   (where key = variable name, value = variable node)
         * - we don't know if a variable node is present in an expression
         *   until we build the expression tree
         *   - for simplicity, we'll create the hashtable now
         *     (alternatively, if we need this hashtable,
         *      we can check if the hashtable exists and
         *      if not, create it as we go)
         */
        zExpTree.zVarNodes = new Hashtable(INIT_SZ);
        zExpTree.zTopNode = zExpTree.buildOptNode();
        return zExpTree;
    }

    private ExpNode buildOptNode() throws BuildException
    {
        String zToken;
        ExpNode zOptNode = null;
        ExpNode zLeftNode;
        ExpNode zRightNode;
        int iId;

        while (0 != zList.size())
        {
            zToken = String.valueOf(zList.removeFirst()); /* pop token */
            iId = ExpOperators.toId(zToken);

            if (ExpOperators.NULL_ID == iId)
            {
                /* if expression doesn't begin with an operator,
                 * then this expression is not in prefix notation
                 */
                throw new BuildException("Invalid syntax - expression does not begin with an operator");
            }
            /* else found an operator */

            zLeftNode = buildNode();

            /* for simplicity, we apply some knowledge of operators here
             * - if we had additional unary operators to handle,
             *   we should declare a new ExpOperators method
             *   to indicate if an operator is unary or binary
             * - for flexibility,
             *   we implemented the methods of the UnaryOptNode class
             *   as if additional unary operators existed
             */
            if (ExpOperators.NOT_ID == iId)
            {
                zOptNode = new UnaryOptNode(iId, zLeftNode);
            }
            else
            {
                zRightNode = buildNode();
                zOptNode = new BinaryOptNode(iId, zLeftNode, zRightNode);
            }

            break;
        }

        return zOptNode;
    }

    private ExpNode buildNode() throws BuildException
    {
        String zToken = null;
        Object zVoid;
        ExpNode zNode = null;

        if (0 != zList.size())
        {
            zToken = String.valueOf(zList.getFirst());
        }
        else
        {
            throw new BuildException("Invalid syntax - expression prematurely terminates");
        }

        if (true == ExpOperators.isShortOpt(zToken))
        {
            /* detected a new operator - build branch for nested operation */
            zNode = buildOptNode();
        }
        else
        {
            zVoid = zList.removeFirst(); /* pop boolean value */

            Object zValue = toObject(zToken);

            if (null == zValue)
            {
                throw new BuildException("Invalid operand (\"" + zToken + "\")");
            }
            else if (zValue instanceof String)
            {
                /* refine string into string literal or variable name */

                StringBuffer zTokenBuffer = new StringBuffer(zToken);

                int iLastChrIdx = zToken.length() - 1;

                char cLastChar = zTokenBuffer.charAt(iLastChrIdx);
                char cFirstChar = zTokenBuffer.charAt(0);

                if (ExpOperators.STR_LITERAL_CHR == cFirstChar &&
                    ExpOperators.STR_LITERAL_CHR == cLastChar)
                {
                    /* detected string literal value
                     * - build constant node for value
                     */

                    /* strip leading and trailing quote chars */
                    zTokenBuffer = zTokenBuffer.deleteCharAt(iLastChrIdx);
                    zTokenBuffer = zTokenBuffer.deleteCharAt(0);

                    zNode = new ConstNode(zTokenBuffer.toString());
                }
                else if (ExpOperators.STR_LITERAL_CHR == cFirstChar ||
                         ExpOperators.STR_LITERAL_CHR == cLastChar)
                {
                    throw new BuildException("Invalid string literal operand (\"" + zToken + "\")");
                }
                else
                {
                    /* detected variable name
                     * - if variable node for this variable name doesn't exists,
                     *   build variable node for variable name
                     * - else reuse existing variable node
                     */

                    zNode = (ExpNode) zVarNodes.get(zToken);
                    if (null == zNode)
                    {
                        /* haven't previously detected this variable name
                         * so build a variable node for it and
                         * add this variable node to the variable node hashtable
                         */
                        zNode = (ExpNode) new VarNode(zToken);

                        zVoid = zVarNodes.put(zToken, zNode);
                    }
                    /* else reuse the variable node that we previously built */
                }
            }
            else
            {
              /* detected boolean or integer value
               * - build constant node for value
               * - ConstNode recognizes boolean and integer values
               */
              zNode = new ConstNode(zValue);
            }
        }

        return zNode;
    }

    /* convert string to boolean, integer or string object
     * - string may be string literal or variable name
     */
    private Object toObject(String zToken)
    {
        Object zRetObject;

        if (true == zToken.equalsIgnoreCase(ExpOperators.TRUE_STR) ||
            true == zToken.equalsIgnoreCase(ExpOperators.FALSE_STR))
        {
            /* boolean - return Boolean object */
            zRetObject = Boolean.valueOf(zToken);
        }
        else
        {
            /* this token may be an integer (-,+,0-9), string literal, or
             * variable name
             *
             * we assume that variable names do not exist in these forms:
             * 1234a, 1234", -abc, +abc, +123, etc
             * - if such names become legal,
             *   then strip out the isDigit and POS_CHAR code and
             *   always call Integer.valueOf() on the token
             *   - if Integer.valueOf() throws an exception
             *     (e.g., NumberFormatException)
             *     then token is not a digit and
             *     must be a string literal or variable name
             */

            StringBuffer zTokenBuffer = new StringBuffer(zToken);
            int iIdx;
            boolean bHasPos = false;

            zRetObject = null;

            for (iIdx = 0; 2 > iIdx; iIdx++)
            {
                char cTestChar = zTokenBuffer.charAt(iIdx);

                if (true == Character.isDigit(cTestChar))
                {
                    if (true == bHasPos)
                    {
                        /* delete positive sign from number because
                         * none of the "string to number" conversion methods
                         * allow a string to begin with a positive sign
                         */
                        zTokenBuffer = zTokenBuffer.deleteCharAt(0);
                    }

                    /* integer - return Integer object */
                    try
                    {
                        zRetObject = Integer.valueOf(zTokenBuffer.toString());
                    }
                    catch(NumberFormatException e)
                    {
                        zLog.error("Unable to convert \"" + zTokenBuffer + "\" to an integer value (" + e.toString() + ")");
                        zRetObject = null;
                    }

                    break; /* iIdx */
                }
                else if (true == Character.isLetter(cTestChar) ||
                         ExpOperators.STR_LITERAL_CHR == cTestChar)
                {
                    /* string literal or variable name - return itself */
                    zRetObject = zToken;
                    break; /* iIdx */
                }
                /* else check next char */

                if (2 > zTokenBuffer.length() ||
                    (ExpOperators.POS_CHAR != cTestChar &&
                     ExpOperators.NEG_CHAR != cTestChar))
                {
                    /* string does not represent an acceptable integer */
                    zRetObject = null;
                    break; /* iIdx */
                }
                /* else this may be a negative or postive signed number
                 * - we're only testing 1st two characters (2 > iIdx)
                 *   so we're not handling expressions like "---2"
                 */

                if (ExpOperators.POS_CHAR == cTestChar)
                    bHasPos = true; /* need this info in next iteration */
            }
        }

        return zRetObject;
    }

    /* private methods */
}
