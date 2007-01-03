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

public class EvalException extends Exception
{
    /* constants */

    /* class variables */

    /* instance variables */
    String zMessage;

    /* constructors */
    public EvalException(String zMessage)
    {
        super(); /* construct exception but don't specify a default message */
        this.zMessage = zMessage;
    }

    /* public methods */
    public String toString()
    {
        /* "<zMessage> ExpException" */
        return zMessage + " (" + super.toString() + ")";
    }

    /* private methods */
}
