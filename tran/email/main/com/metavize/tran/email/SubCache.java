/*
 * Copyright (c) 2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: SubCache.java,v 1.1 2005/01/22 05:34:24 jdi Exp $
 */
package com.metavize.tran.email;

import java.util.*;
import java.util.regex.*;

import com.metavize.tran.util.*;

public class SubCache
{
    /* constants */

    /* class variables */

    /* instance variables */
    private PatternMap zPatternMap;
    private Hashtable zHashtable;

    /* constructors */
    public SubCache(PatternMap zPatternMap, Hashtable zHashtable)
    {
        this.zPatternMap = zPatternMap;
        this.zHashtable = zHashtable;
    }
                                                                            
    /* public methods */
    public PatternMap getPatternMap()
    {
        return zPatternMap;
    }
                                                                            
    public Hashtable getHashtable()
    {
        return zHashtable;
    }

    /* private methods */
}
