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

public class Test
{
    /* constants */

    /* class variables */
    static LinkedList zList;

    /* instance variables */

    /* constructors */

    /* public methods */
    public static void main(String zArgs[])
    {
        ExpTree zExpTree;
        Hashtable zVarValues = new Hashtable();
        Boolean zResult;
        Object zVoid;

        if (zArgs.length < 1)
        {
            System.err.println("Error: No argument to evaluate");
            System.exit(-1);
        }

        System.out.println("Evaluating: \"" + zArgs[0] + "\"");

        try
        {
            zList = ExpTokens.toTokenList(zArgs[0]);

            zExpTree = ExpTree.build(zList);

            zExpTree.dumpTree(null);
            zExpTree.dumpPrefixStr(null);
            zExpTree.dumpInfixStr(null);
            zExpTree.dumpPostfixStr(null);

            //zResult = zExpTree.eval();
            //System.out.println("Expression result: " + zResult.booleanValue());

            zVoid = zVarValues.put("ClntPort", new String("3"));
            //zVoid = zVarValues.put("SrvrPort", new String("10"));

            zResult = zExpTree.eval(zVarValues);

            zExpTree.dumpTree(null);
            zExpTree.dumpPrefixStr(null);
            zExpTree.dumpInfixStr(null);
            zExpTree.dumpPostfixStr(null);

            System.out.println("Expression result: " + zResult.booleanValue());
        }
        catch (TokenException e)
        {
            System.err.println("Expression parse error: " + e.toString());
            System.exit(1);
        }
        catch (BuildException e)
        {
            System.err.println("Build expression tree error: \"" + zArgs[0] + "\" " + e.toString());
            System.exit(1);
        }
        catch (DumpException e)
        {
            System.err.println("Dump expression tree error: \"" + zArgs[0] + "\" " + e.toString());
            System.exit(1);
        }
        catch (EvalException e)
        {
            System.err.println("Expression evaluation error: " + e.toString());
            System.exit(1);
        }
        catch (Exception e)
        {
            System.err.println("Unknown error: " + e.toString());
            System.exit(1);
        }
        finally
        {
        }

        System.exit(0);
    }

    /* private methods */
}
