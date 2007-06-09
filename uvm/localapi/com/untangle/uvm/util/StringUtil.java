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
package com.untangle.uvm.util;

public class StringUtil
{
    private static final StringUtil INSTANCE = new StringUtil();

    /* Done this way to add magic like yes, no, etc */
    private final String TRUTH_CONSTANTS[] = { "true" };
    private final String FALSE_CONSTANTS[] = { "false" };

    private StringUtil()
    {
    }

    public int parseInt( String value, int defaultValue )
    {
        if ( null == value ) return defaultValue;

        try {
            return Integer.parseInt( value.trim());
        }  catch ( NumberFormatException e ) {
            return defaultValue;
        }
    }

    public boolean parseBoolean( String value, boolean defaultValue )
    {
        if ( null == value ) return defaultValue;

        value = value.trim();
        for ( String truth : TRUTH_CONSTANTS ) if ( truth.equals( value )) return true;
        for ( String falseness : FALSE_CONSTANTS ) if ( falseness.equals( value )) return false;

        return defaultValue;
    }

    public static StringUtil getInstance()
    {
        return INSTANCE;
    }
}