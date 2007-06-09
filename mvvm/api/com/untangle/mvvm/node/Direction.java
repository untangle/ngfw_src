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

package com.untangle.mvvm.tran;

import java.io.Serializable;

/**
 * This class represents <b>Data</b> direction, not session direction!
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
 */
public class Direction implements Serializable
{
    private static final long serialVersionUID = 9115074195671019726L;

    public static final Direction OUTGOING = new Direction("outgoing");
    public static final Direction INCOMING = new Direction("incoming");

    private final String directionName;

    // constructors -----------------------------------------------------------

    public Direction(String directionName)
    {
        this.directionName = directionName;
    }

    // public methods ---------------------------------------------------------

    public String getDirectionName()
    {
        return directionName;
    }

    // Object methods ---------------------------------------------------------

    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof Direction)) {
            return false;
        }

        Direction d = (Direction)o;

        return directionName.equals(d.directionName);
    }

    @Override
    public int hashCode()
    {
        return directionName.hashCode();
    }
}
