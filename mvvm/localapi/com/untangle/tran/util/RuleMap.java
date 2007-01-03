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

import java.util.Hashtable;

import org.apache.log4j.Logger;

/* RuleMap maps a list of expressions to a list of actions
 * - each element in these lists are implicitly paired one-to-one,
 *   from beginning to end
 * - actions are expressed as integer objects (e.g., action codes) that
 *   the caller defines and assigns to operational code outside of a RuleMap
 *   - later, if necessary,
 *     actions can be encapsulated as operational code objects and
 *     embedded in a RuleMap
 */
public class RuleMap
{
    /* constants */
    private final Logger zLog = Logger.getLogger(getClass());

    /* expression to action mapping options (mutually exclusive)
     * (for these options,
     *  the expression and action lists must be the same size and
     *  each expression has a one-to-one mapping to an action
     *  from the beginning to the end of each list)
     * FIRST2FIRST = 1st true expression maps to action of 1st true expression
     * FIRST2LAST = 1st true expression maps to action of last true expression
     * LAST2FIRST = last true expression maps to action of 1st true expression
     * LAST2LAST = last true expression maps to action of last true expression
     *
     * EE AA -> EE = expression, AA = action
     * 00 (0) = none (not valid)
     * 01 (1) = 1st
     * 10 (2) = last
     * 11 (3) = all (not valid)
     */
    private static final int NONE = 0; /* 00 */
    private static final int FIRST = 1; /* 01 */
    private static final int LAST = 2; /* 10 */
    private static final int ALL = 3; /* 11 */
    private static final int EXP_SHIFT = 2;
    public static final int FIRST2FIRST = ((FIRST << EXP_SHIFT) + FIRST);
    public static final int FIRST2LAST = ((FIRST << EXP_SHIFT) + LAST);
    public static final int LAST2FIRST = ((LAST << EXP_SHIFT) + FIRST);
    public static final int LAST2LAST = ((LAST << EXP_SHIFT) + LAST);
    private static final int EXP_MASK = ((ALL << EXP_SHIFT) + NONE);
    private static final int ACT_MASK = ((NONE << EXP_SHIFT) + ALL);

    /* default options (mutually exclusive)
     * if no expression evaluates to true, then:
     * DEFNONE = no default action
     * DEFFIRST = default to 1st action
     * DEFLAST = default to last action
     */
    public static final int DEFNONE = 0;
    public static final int DEFFIRST = 1;
    public static final int DEFLAST = 2;

    /* may mean that we found no true action or didn't search for one */
    public static final int NO_TRUE_ACTION = -1;

    /* class variables */

    /* instance variables */
    RuleObject zExp; /* expressions (ExpTree objects) */
    RuleObject zAct; /* action codes (Object objects) */
    int iFirstTrue;
    int iLastTrue;

    /* constructors */
    private RuleMap() {}

    /* public methods */
    /* create rule map using pre-built set of expression-action pairs */
    public static RuleMap createMap(RuleObject zExp, RuleObject zAct)
       throws BuildException
    {
        if (zExp.count() != zAct.count())
        {
            throw new BuildException("Invalid rule map - expression and action sets are not the same size");
        }
        else if (null == zExp)
        {
            throw new BuildException("Invalid rule map - expression set is null");
        }
        else if (null == zAct)
        {
            throw new BuildException("Invalid rule map - action set is null");
        }
        else if (null == zExp && null == zAct)
        {
            throw new BuildException("Invalid rule map - expression and action sets are null");
        }

        RuleMap zMap = new RuleMap();
        zMap.zExp = zExp;
        zMap.zAct = zAct;
        zMap.iFirstTrue = NO_TRUE_ACTION;
        zMap.iLastTrue = NO_TRUE_ACTION;

        return zMap;
    }

    /* initialize rule map */
    public static RuleMap initMap()
    {
        RuleMap zMap = new RuleMap();
        zMap.zExp = new RuleObject();
        zMap.zAct = new RuleObject();
        zMap.iFirstTrue = NO_TRUE_ACTION;
        zMap.iLastTrue = NO_TRUE_ACTION;

        return zMap;
    }

    /* initialize rule map using 1st expression-action pair */
    public static RuleMap initMap(ExpTree zExpTree, Object zAction)
       throws BuildException
    {
        if (null == zExpTree)
        {
            throw new BuildException("Invalid rule - expression is null");
        }
        /* user may define a null action to handle */

        RuleMap zMap = new RuleMap();
        zMap.zExp = new RuleObject();
        zMap.zAct = new RuleObject();

        zMap.zExp.appendObject(zExpTree);
        zMap.zAct.appendObject(zAction);
        zMap.iFirstTrue = NO_TRUE_ACTION;
        zMap.iLastTrue = NO_TRUE_ACTION;

        return zMap;
    }

    /* prepend expression-action pair to _this_ rule map */
    public void prependToMap(ExpTree zExpTree, Object zAction)
       throws BuildException
    {
        if (null == zExpTree)
        {
            throw new BuildException("Invalid rule - expression is null");
        }
        /* user may define a null action to handle */

        zExp.prependObject(zExpTree);
        zAct.prependObject(zAction);
        iFirstTrue = NO_TRUE_ACTION;
        iLastTrue = NO_TRUE_ACTION;

        return;
    }

    /* append expression-action pair to _this_ rule map */
    public void appendToMap(ExpTree zExpTree, Object zAction)
       throws BuildException
    {
        if (null == zExpTree)
        {
            throw new BuildException("Invalid rule - expression is null");
        }
        /* user may define a null action to handle */

        zExp.appendObject(zExpTree);
        zAct.appendObject(zAction);
        iFirstTrue = NO_TRUE_ACTION;
        iLastTrue = NO_TRUE_ACTION;

        return;
    }

    /* remove expression-action pair from _this_ rule map
     * using the given expression as the key
     */
    public void removeFromMap(ExpTree zExpTree) throws ClearException
    {
        if (null == zExpTree)
        {
            throw new ClearException("Invalid rule - expression is null");
        }

        int iIdx = zExp.removeObject(zExpTree);

        if (-1 != iIdx)
        {
            zAct.removeObject(iIdx);
        }
        iFirstTrue = NO_TRUE_ACTION;
        iLastTrue = NO_TRUE_ACTION;

        return;
    }

    /* remove expression-action pair from _this_ rule map
     * using the given action as the key
     * (not recommended since actions may be user-specific integer codes and
     *  may not be unique)
     */
    public void removeFromMap(Object zAction)
    {
        int iIdx = zAct.removeObject(zAction);

        if (-1 != iIdx)
        {
            zExp.removeObject(iIdx);
        }
        iFirstTrue = NO_TRUE_ACTION;
        iLastTrue = NO_TRUE_ACTION;

        return;
    }

    /* clear out _this_ rule map - delete all expression-action pairs */
    public void emptyMap()
    {
        zExp.empty();
        zAct.empty();
        iFirstTrue = NO_TRUE_ACTION;
        iLastTrue = NO_TRUE_ACTION;

        return;
    }

    /* evaluate expressions within _this_ rule map
     * using the supplied environment and specified mapping and default options
     * (when iDefOption = DEFNONE, return -1 if no rule evaluates to true)
     * and return the action of the matching true expression
     */
    public Object evalMap(Hashtable zEnv, int iMapOption, int iDefOption)
        throws EvalException
    {
        int iCnt = zExp.count();
        if (0 == iCnt)
        {
            //throw new EvalException("Empty rule map - no rules to evaluate");
            return null;
        }

        iFirstTrue = NO_TRUE_ACTION;
        iLastTrue = NO_TRUE_ACTION;

        ExpTree zExpTree;
        Boolean zResult = Boolean.FALSE;

        int iExpRule = (iMapOption & EXP_MASK) >> EXP_SHIFT;
        int iActRule = iMapOption & ACT_MASK;

        int iIdx;

        if (FIRST == iExpRule || FIRST == iActRule)
        {
            /* evaluate expressions from beginning of list */
            for (iIdx = 0; iIdx < iCnt; iIdx++)
            {
                zExpTree = (ExpTree) zExp.getObject(iIdx);

                try
                {
                    zResult = zExpTree.eval(zEnv);
                    if (Boolean.TRUE == zResult)
                    {
                        iFirstTrue = iIdx; /* found 1st true expression */

                        if (FIRST == iExpRule && FIRST == iActRule)
                        {
                            /* done - return action for 1st true expression */
                            return zAct.getObject(iFirstTrue);
                        } /* else continue search for last true expression */
                        break;
                    }
                }
                catch (EvalException e)
                {
                    zLog.error("Unable to evaluate expression for rule[" + iIdx + "] " + e.toString());
                    continue;
                }
            }
        } /* else we only want the last true expression */

        /* - if we haven't evaluated any expressions (!FIRST) yet,
         * then evaluate expressions from end of list
         * - if we have already evaluated expressions (FIRST) and
         * found a true expression (so we know that at least one exists),
         * then evaluate expressions from end of list
         * - else don't bother evaluating expressions
         */
        if ((LAST == iExpRule || LAST == iActRule) &&
            ((FIRST != iExpRule && FIRST != iActRule) ||
             NO_TRUE_ACTION != iFirstTrue))
        {
            /* evaluate expressions from end of list */
            for (iIdx = iCnt - 1; 0 <= iIdx; iIdx--)
            {
                zExpTree = (ExpTree) zExp.getObject(iIdx);
                zResult = zExpTree.eval(zEnv);
                if (Boolean.TRUE == zResult)
                {
                    iLastTrue = iIdx; /* found last true expression */

                    if (FIRST == iActRule)
                    {
                        /* done - return action for 1st true expression */
                        return zAct.getObject(iFirstTrue);
                    }
                    else
                    {
                        /* done - return action for last true expression */
                        return zAct.getObject(iLastTrue);
                    }
                }
            }
        } /* else we don't want the last true expression or
           * already know that no expression is true
           */

        /* take default action - we found no true expression */
        switch (iDefOption)
        {
        case DEFNONE:
        default:
            /* fall through */
            break;

        case DEFFIRST:
            return zAct.getObject(0);

        case DEFLAST:
            return zAct.getObject(iCnt - 1);
        }

        return null;
    }

    public void unevalMap()
    {
        iFirstTrue = NO_TRUE_ACTION;
        iLastTrue = NO_TRUE_ACTION;

        return;
    }

    /* get index of 1st expression that evaluated as true - for debugging */
    public int getFirstTrueDbg()
    {
        return iFirstTrue; /* index or NO_TRUE_ACTION */
    }

    /* get index of last expression that evaluated as true - for debugging */
    public int getLastTrueDbg()
    {
        return iLastTrue; /* index or NO_TRUE_ACTION */
    }

    public int count()
    {
        /* expression and action sets are always the same size */
        return zExp.count();
    }

    /* for debugging */
    public void dump(Hashtable zEnv)
    {
        int iIdx;
        int iCnt = zExp.count();
        if (0 == iCnt)
        {
            zLog.info("Empty rule map - no rules to dump");
            return;
        }

        ExpTree zExpTree;
        Object zAction;
        for (iIdx = 0; iIdx < iCnt; iIdx++)
        {
            zLog.info("Rule map[" + iIdx + "]:");
            zExpTree = (ExpTree) zExp.getObject(iIdx);
            try
            {
                zLog.info("expression: ");
                zExpTree.dumpInfixStr(zEnv);
            }
            catch (DumpException e)
            {
                 zLog.info("<null>");
            }
            zAction = zAct.getObject(iIdx);
            zLog.info("action: " + zAction);
        }

        return;
    }

    /* private methods */
}
