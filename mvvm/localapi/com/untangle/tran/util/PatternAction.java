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

import com.untangle.mvvm.util.*;

public class PatternAction
{
    /* constants */
    public final static PatternAction NOTRUEACTION = PatternAction.dummyAction(new Integer(RuleMap.NO_TRUE_ACTION));

    /* class variables */

    /* instance variables */
    Integer zAction;
    String zValue;

    /* constructors */
    private PatternAction(Integer zAction)
    {
        this.zAction = zAction;
        this.zValue = null;
    }

    public PatternAction(Integer zAction, String zValue)
    {
        this.zAction = zAction;
        this.zValue = zValue;
    }

    /* public methods */
    public static PatternAction dummyAction(Integer zAction)
    {
        return new PatternAction(zAction);
    }

    public int getAction()
    {
        return zAction.intValue();
    }

    public String getValue()
    {
        return zValue;
    }

    public String toString()
    {
        return zAction + ": \"" + zValue + "\"";
    }

    /* private methods */
}
