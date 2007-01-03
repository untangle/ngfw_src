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

import java.util.LinkedList;

/* RuleObject provides a cover for managing a list of rule objects */
public class RuleObject
{
    /* constants */

    /* class variables */

    /* instance variables */
    LinkedList zList;

    /* constructors */
    public RuleObject()
    {
        zList = new LinkedList();
    }

    public RuleObject(LinkedList zList)
    {
        this.zList = zList;
    }

    /* public methods */
    public void prependObject(Object zObject)
    {
        zList.addFirst(zObject);
        return;
    }

    public void appendObject(Object zObject)
    {
        zList.addLast(zObject);
        return;
    }

    public int removeObject(Object zObject)
    {
        int iIdx = zList.indexOf(zObject);

        if (-1 != iIdx)
        {
            Object zVoid = zList.remove(iIdx);
        }

        return iIdx;
    }

    public void removeObject(int iIdx)
    {
        Object zVoid = zList.remove(iIdx);
        return;
    }

    public void empty()
    {
        zList.clear();
        return;
    }

    public int count()
    {
        /* fyi: index range = [0, size() - 1] */
        return zList.size();
    }

    public Object getObject(int iIdx)
    {
        return zList.get(iIdx);
    }

    /* private methods */
}
