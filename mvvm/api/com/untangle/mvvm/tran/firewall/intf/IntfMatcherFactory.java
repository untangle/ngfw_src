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

package com.untangle.mvvm.tran.firewall.intf;

import com.untangle.mvvm.IntfEnum;

import com.untangle.mvvm.tran.ParseException;

import com.untangle.mvvm.tran.firewall.ParsingFactory;


/**
 * A factory for interface matchers.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
public class IntfMatcherFactory
{
    private static final IntfMatcherFactory INSTANCE = new IntfMatcherFactory();

    /** The parser used to translate strings into IntfDBMatchers. */
    private final ParsingFactory<IntfDBMatcher> parser;

    private IntfMatcherFactory()
    {
        this.parser = new ParsingFactory<IntfDBMatcher>( "intf matcher" );
        this.parser.registerParsers( IntfSimpleMatcher.PARSER, IntfSingleMatcher.PARSER, 
                                     IntfInverseMatcher.PARSER, IntfSetMatcher.PARSER );
    }

    public IntfDBMatcher getAllMatcher() 
    {
        return IntfSimpleMatcher.getAllMatcher();
    }

    public IntfDBMatcher getNilMatcher() 
    {
        return IntfSimpleMatcher.getNilMatcher();
    }

    public IntfDBMatcher getExternalMatcher()
    {
        return IntfSingleMatcher.getExternalMatcher();
    }
    
    public IntfDBMatcher getInternalMatcher()
    {
        return IntfSingleMatcher.getInternalMatcher();
    }

    public IntfDBMatcher getDmzMatcher()
    {
        return IntfSingleMatcher.getDmzMatcher();
    }

    public IntfDBMatcher getVpnMatcher()
    {
        return IntfSingleMatcher.getVpnMatcher();
    }
    
    /**
     * Update the enumeration of IntfMatchers.
     *
     * @param intfEnum The new interface enumeration.
     */
    public void updateEnumeration( IntfEnum intfEnum )
    {
        IntfMatcherEnumeration.getInstance().updateEnumeration( intfEnum );
    }

    /**
     * Retrieve the enumeration of possible IntfMatchers.
     *
     * @return An array of valid IntfMatchers.
     */    
    public IntfDBMatcher[] getEnumeration()
    {
        return IntfMatcherEnumeration.getInstance().getEnumeration();
    }

    /**
     * Retrieve the default IntfMatcher.
     *
     * @return The default IntfMatcher
     */
    public IntfDBMatcher getDefault()
    {
        return IntfMatcherEnumeration.getInstance().getDefault();
    }

    /**
     * Retrieve an intf matcher that matches <param>intf</param>
     *
     * @param intf The interface to match.
     */
    public IntfDBMatcher makeSingleMatcher( byte intf ) throws ParseException
    {
        return IntfSingleMatcher.makeInstance( intf );
    }

    /**
     * Retrieve an intf matcher that matches any of the interfaces in
     * <param>intfArray</param>
     *
     * @param intfArray An array of interfaces to match.
     */
    public IntfDBMatcher makeSetMatcher( byte ... intfArray ) throws ParseException
    {
        switch ( intfArray.length ) {
        case 0: return IntfSimpleMatcher.getNilMatcher();
        case 1: return makeSingleMatcher( intfArray[0] );
        default: return IntfSetMatcher.makeInstance( intfArray );
        }
    }

    /**
     * Retrieve an intf matcher that doesn't match any of the
     * interfaces in <param>intfArray</param>
     *
     * @param intfArray Array of interfaces that shouldn't match.
     */
    public IntfDBMatcher makeInverseMatcher( byte ... intfArray ) throws ParseException
    {
        switch ( intfArray.length ) {
        case 0:  return IntfSimpleMatcher.getAllMatcher();
        default: return IntfInverseMatcher.makeInstance( intfArray );
        }
    }

    /**
     * Convert <param>value</param> to an IntfDBMatcher.
     *
     * @param value The string to parse.
     */
    public static IntfDBMatcher parse( String value ) throws ParseException
    {
        return INSTANCE.parser.parse( value );
    }

    public static IntfMatcherFactory getInstance()
    {
        return INSTANCE;
    }
}

