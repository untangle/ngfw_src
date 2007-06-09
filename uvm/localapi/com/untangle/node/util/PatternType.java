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
package com.untangle.node.util;

import java.util.*;
import java.util.regex.*;

import com.untangle.uvm.util.*;

public class PatternType
{
    /* constants */

    /* class variables */

    /* instance variables */
    Integer zType;
    Pattern zPattern;

    /* constructors */
    public PatternType(Integer zType, Pattern zPattern)
    {
        this.zType = zType;
        this.zPattern = zPattern;
    }

    /* public methods */
    public int getType()
    {
        return zType.intValue();
    }

    public Pattern getPattern()
    {
        return zPattern;
    }

    public String toString()
    {
        return zType + ": (" + zPattern.flags() + ") \"" + zPattern.pattern() + "\": ";
    }

    /* private methods */
}
