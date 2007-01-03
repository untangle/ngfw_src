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
import java.util.regex.*;

import org.apache.log4j.Logger;

/* PatternMap maps a list of pattern-types to a list of actions
 * - each element in these lists are implicitly paired one-to-one,
 *   from beginning to end
 * - actions are expressed as integer objects (e.g., action codes) that
 *   the caller defines and assigns to operational code outside of a PatternMap
 *   - later, if necessary,
 *     actions can be encapsulated as operational code objects and
 *     embedded in a PatternMap
 */
public class PatternMap
{
    /* constants */
    private final Logger zLog = Logger.getLogger(getClass());

    /* pattern to action mapping options (mutually exclusive)
     * (for these options,
     *  the pattern and action lists must be the same size and
     *  each pattern has a one-to-one mapping to an action
     *  from the beginning to the end of each list)
     * FIRST2FIRST = 1st true pattern maps to action of 1st true pattern
     * FIRST2LAST = 1st true pattern maps to action of last true pattern
     * LAST2FIRST = last true pattern maps to action of 1st true pattern
     * LAST2LAST = last true pattern maps to action of last true pattern
     *
     * EE AA -> EE = pattern, AA = action
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
     * if no pattern matches to true, then:
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
    RuleObject zPatRule; /* pattern-types (PatternType objects) */
    RuleObject zActRule; /* action codes (Object objects) */

    /* constructors */
    private PatternMap() {}

    /* public methods */
    /* create pattern map using pre-compiled set of pattern-action pairs */
    public static PatternMap createMap(RuleObject zPatRule, RuleObject zActRule)
       throws BuildException
    {
        if (zPatRule.count() != zActRule.count())
        {
            throw new BuildException("Invalid pattern map - pattern and action sets are not the same size");
        }
        else if (null == zPatRule)
        {
            throw new BuildException("Invalid pattern map - pattern set is null");
        }
        else if (null == zActRule)
        {
            throw new BuildException("Invalid pattern map - action set is null");
        }
        else if (null == zPatRule && null == zActRule)
        {
            throw new BuildException("Invalid pattern map - pattern and action sets are null");
        }

        PatternMap zMap = new PatternMap();
        zMap.zPatRule = zPatRule;
        zMap.zActRule = zActRule;

        return zMap;
    }

    /* initialize pattern map */
    public static PatternMap initMap()
    {
        PatternMap zMap = new PatternMap();
        zMap.zPatRule = new RuleObject();
        zMap.zActRule = new RuleObject();

        return zMap;
    }

    /* initialize pattern map using 1st pattern-action pair */
    public static PatternMap initMap(PatternType zPatternType, Object zAction)
       throws BuildException
    {
        if (null == zPatternType)
        {
            throw new BuildException("Invalid pattern-type - pattern-type is null");
        }
        /* user may define a null action to handle */

        PatternMap zMap = new PatternMap();
        zMap.zPatRule = new RuleObject();
        zMap.zActRule = new RuleObject();

        zMap.zPatRule.appendObject(zPatternType);
        zMap.zActRule.appendObject(zAction);

        return zMap;
    }

    /* prepend pattern-action pair to _this_ pattern map */
    public void prependToMap(PatternType zPatternType, Object zAction)
       throws BuildException
    {
        if (null == zPatternType)
        {
            throw new BuildException("Invalid pattern-type - pattern-type is null");
        }
        /* user may define a null action to handle */

        zPatRule.prependObject(zPatternType);
        zActRule.prependObject(zAction);

        return;
    }

    /* append pattern-action pair to _this_ pattern map */
    public void appendToMap(PatternType zPatternType, Object zAction)
       throws BuildException
    {
        if (null == zPatternType)
        {
            throw new BuildException("Invalid pattern-type - pattern-type is null");
        }
        /* user may define a null action to handle */

        zPatRule.appendObject(zPatternType);
        zActRule.appendObject(zAction);

        return;
    }

    /* remove pattern-action pair from _this_ pattern map
     * using the given pattern as the key
     */
    public void removeFromMap(PatternType zPatternType) throws ClearException
    {
        if (null == zPatternType)
        {
            throw new ClearException("Invalid pattern-type - pattern-type is null");
        }

        int iIdx = zPatRule.removeObject(zPatternType);

        if (-1 != iIdx)
        {
            zActRule.removeObject(iIdx);
        }

        return;
    }

    /* remove pattern-action pair from _this_ pattern map
     * using the given action as the key
     * (not recommended since actions may be user-specific integer codes and
     *  may not be unique)
     */
    public void removeFromMap(Object zAction)
    {
        int iIdx = zActRule.removeObject(zAction);

        if (-1 != iIdx)
        {
            zPatRule.removeObject(iIdx);
        }

        return;
    }

    /* clear out _this_ pattern map - delete all pattern-action pairs */
    public void emptyMap()
    {
        zPatRule.empty();
        zActRule.empty();

        return;
    }

    /* match pattern within _this_ pattern map
     * using the specified mapping and default options
     * (when iDefOption = DEFNONE, return -1 if no pattern matches to true)
     * and return the action of the matching true pattern
     */
    public MatchAction matchMap(PatternTestObject zPTO, int iMapOption, int iDefOption)
        throws MatchException
    {
        int iCnt = zPatRule.count();
        if (0 == iCnt)
        {
            //throw new MatchException("Empty pattern map - no patterns to match");
            return null;
        }

        ArrayList zTestList;
        CharSequence zTest;
        Iterator zIter;
        Pattern zPattern;
        Matcher zMatcher;
        PatternType zPatternType;
        PatternAction zPatternAction;
        int iType;

        int iPatRule = (iMapOption & EXP_MASK) >> EXP_SHIFT;
        int iActRule = iMapOption & ACT_MASK;

        int iFirstTrue = NO_TRUE_ACTION;
        int iLastTrue = NO_TRUE_ACTION;
        int iIdx;

        if (FIRST == iPatRule || FIRST == iActRule)
        {
            /* match pattern from beginning of list */
            for (iIdx = 0; iIdx < iCnt; iIdx++)
            {
                zPatternType = (PatternType) zPatRule.getObject(iIdx);
                zPattern = zPatternType.getPattern();
                iType = zPatternType.getType();
                zTestList = (ArrayList) zPTO.get(iType);

                if (null == zTestList ||
                    0 == zTestList.size())
                {
                    continue;
                }

                for (zIter = zTestList.iterator(); true == zIter.hasNext(); )
                {
                    zTest = (CharSequence) zIter.next();
                    zMatcher = zPattern.matcher(zTest);
                    zLog.debug("match: " + zMatcher.pattern().pattern() + ", test: " + zTest);
                    if (true == zMatcher.find())
                    {
                        iFirstTrue = iIdx; /* found 1st true pattern */

                        if (FIRST == iPatRule && FIRST == iActRule)
                        {
                            /* done - return action for 1st true pattern */
                            zPatternAction = (PatternAction) zActRule.getObject(iFirstTrue);
                            return new MatchAction(zTest, iType, zMatcher, zPatternAction, iFirstTrue, iLastTrue);
                        } /* else continue search for last true pattern */
                        break;
                    }
                }
            }
        } /* else we only want the last true pattern */

        /* - if we haven't matched any pattern (!FIRST) yet,
         * then match pattern from end of list
         * - if we have already matched pattern (FIRST) and
         * found a true pattern (so we know that at least one exists),
         * then match pattern from end of list
         * - else don't bother matching pattern
         */
        if ((LAST == iPatRule || LAST == iActRule) &&
            ((FIRST != iPatRule && FIRST != iActRule) ||
             NO_TRUE_ACTION != iFirstTrue))
        {
            /* match pattern from end of list */
            for (iIdx = iCnt - 1; 0 <= iIdx; iIdx--)
            {
                zPatternType = (PatternType) zPatRule.getObject(iIdx);
                zPattern = zPatternType.getPattern();
                iType = zPatternType.getType();
                zTestList = (ArrayList) zPTO.get(iType);

                if (null == zTestList ||
                    0 == zTestList.size())
                {
                    continue;
                }

                for (zIter = zTestList.iterator(); true == zIter.hasNext(); )
                {
                    zTest = (CharSequence) zIter.next();
                    zMatcher = zPattern.matcher(zTest);
                    zLog.debug("match: " + zMatcher.pattern().pattern() + ", test: " + zTest);
                    if (true == zMatcher.find())
                    {
                        iLastTrue = iIdx; /* found last true pattern */

                        if (FIRST == iActRule)
                        {
                            /* done - return action for 1st true pattern */
                            zPatternAction = (PatternAction) zActRule.getObject(iFirstTrue);
                        }
                        else
                        {
                            /* done - return action for last true pattern */
                            zPatternAction = (PatternAction) zActRule.getObject(iLastTrue);
                        }
                        return new MatchAction(zTest, iType, zMatcher, zPatternAction, iFirstTrue, iLastTrue);
                    }
                }
            }
        } /* else we don't want the last true pattern or
           * already know that no pattern is true
           */

        MatchAction zMatchAction;

        /* take default action - we found no true pattern */
        switch (iDefOption)
        {
        case DEFNONE:
        default:
            /* fall through */
            zMatchAction = null;
            break;

        case DEFFIRST:
            zPatternAction = (PatternAction) zActRule.getObject(0);
            zMatchAction = new MatchAction(null, MatchAction.NONE, null, zPatternAction, NO_TRUE_ACTION, NO_TRUE_ACTION);
            break;

        case DEFLAST:
            zPatternAction = (PatternAction) zActRule.getObject(iCnt - 1);
            zMatchAction = new MatchAction(null, MatchAction.NONE, null, zPatternAction, NO_TRUE_ACTION, NO_TRUE_ACTION);
            break;
        }

        return zMatchAction;
    }

    public int count()
    {
        /* pattern and action sets are always the same size */
        return zPatRule.count();
    }

    /* for debugging */
    public void dump()
    {
        int iIdx;
        int iCnt = zPatRule.count();
        if (0 == iCnt)
        {
            zLog.debug("Empty pattern map - no patterns to dump");
            return;
        }

        PatternType zPatternType;
        PatternAction zPatternAction;
        for (iIdx = 0; iIdx < iCnt; iIdx++)
        {
            zLog.debug("Pattern map[" + iIdx + "]:");
            zPatternType = (PatternType) zPatRule.getObject(iIdx);
            zLog.debug("pattern-type: " + zPatternType);
            zPatternAction = (PatternAction) zActRule.getObject(iIdx);
            zLog.debug("action: " + zPatternAction);
        }

        return;
    }

    /* private methods */
}
