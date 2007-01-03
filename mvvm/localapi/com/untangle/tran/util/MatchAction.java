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

import java.nio.*;
import java.util.*;
import java.util.regex.*;

import com.untangle.mvvm.util.*;

public class MatchAction
{
    /* constants */
    public final static MatchAction NOTRUEACTION = MatchAction.dummyAction(PatternAction.NOTRUEACTION);

    public final static int NONE = 0;

    /* class variables */

    /* instance variables */
    CharSequence zMatchLine;
    int iType;
    Matcher zMatcher;
    PatternAction zPatternAction;
    int iFirstTrue;
    int iLastTrue;

    /* constructors */
    private MatchAction(PatternAction zPatternAction)
    {
        this.zMatchLine = null;
        this.iType = NONE;
        this.zMatcher = null;
        this.zPatternAction = zPatternAction;
        this.iFirstTrue = PatternMap.NO_TRUE_ACTION;
        this.iLastTrue = PatternMap.NO_TRUE_ACTION;
    }

    public MatchAction(CharSequence zMatchLine, int iType, Matcher zMatcher, PatternAction zPatternAction, int iFirstTrue, int iLastTrue)
    {
        this.zMatchLine = zMatchLine;
        this.iType = iType;
        this.zMatcher = zMatcher;
        this.zPatternAction = zPatternAction;
        this.iFirstTrue = iFirstTrue;
        this.iLastTrue = iLastTrue;
    }

    /* public methods */
    public static MatchAction dummyAction(PatternAction zPatternAction)
    {
        return new MatchAction(zPatternAction);
    }

    public CharSequence getMatchLine()
    {
        return zMatchLine;
    }

    public int getType()
    {
        return iType;
    }

    public Matcher getMatcher()
    {
        return zMatcher;
    }

    public PatternAction getPatternAction()
    {
        return zPatternAction;
    }

    /* get index of 1st pattern that matched as true - for debugging */
    public int getFirstTrueDbg()
    {
        return iFirstTrue; /* index or NO_TRUE_ACTION */
    }

    /* get index of last pattern that matched as true - for debugging */
    public int getLastTrueDbg()
    {
        return iLastTrue; /* index or NO_TRUE_ACTION */
    }

    public String toString()
    {
        return zPatternAction + ", \"" + zMatchLine + "\": " + iType + ": " + zMatcher;
    }

    /* private methods */
}
