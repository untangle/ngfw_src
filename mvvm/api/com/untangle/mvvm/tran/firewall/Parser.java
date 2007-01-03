 
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

package com.untangle.mvvm.tran.firewall;

import com.untangle.mvvm.tran.ParseException;

public interface Parser<T>
{
    /* Attempt to parse a string into an object.  If the object is not
     * parseable, then isParseable should return false.  If the object
     * is parseable, but contains errors, then this should throw a parse
     * exception.  (EG. an IP address with one component greater than 255).
     * This function should NOT return null.  If a value is not parseable
     * that should be indicated in isParseable.
     */
    public T parse( String value ) throws ParseException;

    /* Return true if the string value is parseable */
    public boolean isParseable( String value );

    public int priority();
}
