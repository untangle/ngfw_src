/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: EvalException.java,v 1.1.1.1 2004/12/01 23:32:24 amread Exp $
 */
package com.metavize.tran.util;

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
