 
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

package com.untangle.uvm.node.firewall;

import com.untangle.uvm.node.ParseException;

/**
 * An interface for a parser.
 *
 * A parser is designed to take a string and return the object that
 * string represents.  This is used by the ParsingFactory.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
public interface Parser<T>
{
    /**
     * Parse a string and create a new object.
     *
     * Attempt to parse a string into an object.  If the object is not
     * parseable, then isParseable should return false.  If the object
     * is parseable, but contains errors, then this should throw a parse
     * exception.  (EG. an IP address with one component greater than 255).
     * This function should never return null.
     *
     * @param value The value to parse.
     * @return Object represented by <param>value</param>.
     */
    public T parse( String value ) throws ParseException;

    /**
     * Determine whether or not this parser is capable of parsing
     * <param>value</param>
     *
     * @param value The value to test.
     * @return True if <param>value</value> can be parsed by this parser.
     */
    public boolean isParseable( String value );

    /**
     * Return the priortity of this parser.  Priority 0 is evaluated first.
     *
     * @return The priority of this parser.  (0 is the highest priority).
     */
    public int priority();
}
