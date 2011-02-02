/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
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