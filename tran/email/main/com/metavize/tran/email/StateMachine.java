/*
 * Copyright (c) 2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: StateMachine.java,v 1.1 2005/01/22 05:34:24 jdi Exp $
 */
package com.metavize.tran.email;

import java.lang.IndexOutOfBoundsException;
import java.util.*;
import java.util.regex.*;

import com.metavize.tran.util.*;

public class StateMachine
{
    /* constants */
    private final static int STATEPAIRSZ = 10;

    /* class variables */

    /* instance variables */
    private ArrayList zStatePairs; /* pairs of cmd-reply lines */
    private ArrayList zReservedPairs; /* pairs of cmd-reply lines */

    /* constructors */
    public StateMachine()
    {
        zStatePairs = new ArrayList(STATEPAIRSZ);
        zReservedPairs = new ArrayList(STATEPAIRSZ);

        StatePair zStatePair;

        for (int idx = 0; STATEPAIRSZ > idx; idx++)
        {
            zStatePair = new StatePair();
            zReservedPairs.add(zStatePair);
        }
    }

    /* public methods */
    public void set(StatePair zStatePair)
    {
        zStatePairs.add(zStatePair);
        return;
    }

    public void set(Integer zCmd, Integer zReply)
    {
        StatePair zStatePair;
        try
        {
            zStatePair = (StatePair) zReservedPairs.remove(0);
            if (null == zStatePair)
            {
                zStatePair = new StatePair();
            }
        }
        catch (IndexOutOfBoundsException e)
        {
            zStatePair = new StatePair();
        }

        zStatePair.renew(zCmd, zReply);
        zStatePairs.add(zStatePair);
        return;
    }

    public void reset(StatePair zStatePair)
    {
        flush();
        set(zStatePair);
    }

    public void reset(Integer zCmd, Integer zReply)
    {
        flush();
        set(zCmd, zReply);
    }

    public ArrayList get()
    {
        return zStatePairs;
    }

    public void flush()
    {
        StatePair zStatePair;

        for (Iterator zIter = zStatePairs.iterator(); true == zIter.hasNext(); )
        {
            zStatePair = (StatePair) zIter.next();
            zIter.remove();

            if (true == zStatePair.isRenewable())
            {
                zReservedPairs.add(zStatePair);
            }
        }

        return;
    }

    /* private methods */
}
