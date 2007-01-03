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

public class IntfMatcherFactory
{
    private static final IntfMatcherFactory INSTANCE = new IntfMatcherFactory();

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

    public void updateEnumeration( IntfEnum intfEnum )
    {
        IntfMatcherEnumeration.getInstance().updateEnumeration( intfEnum );
    }
    
    public IntfDBMatcher[] getEnumeration()
    {
        return IntfMatcherEnumeration.getInstance().getEnumeration();
    }

    public IntfDBMatcher getDefault()
    {
        return IntfMatcherEnumeration.getInstance().getDefault();
    }

    public IntfDBMatcher makeSingleMatcher( byte intf ) throws ParseException
    {
        return IntfSingleMatcher.makeInstance( intf );
    }

    public IntfDBMatcher makeSetMatcher( byte ... intfArray ) throws ParseException
    {
        switch ( intfArray.length ) {
        case 0: return IntfSimpleMatcher.getNilMatcher();
        case 1: return makeSingleMatcher( intfArray[0] );
        default: return IntfSetMatcher.makeInstance( intfArray );
        }
    }

    /** Matches all but the interfaces listed */
    public IntfDBMatcher makeInverseMatcher( byte ... intfArray ) throws ParseException
    {
        switch ( intfArray.length ) {
        case 0:  return IntfSimpleMatcher.getAllMatcher();
        default: return IntfInverseMatcher.makeInstance( intfArray );
        }
    }

    public static IntfDBMatcher parse( String value ) throws ParseException
    {
        return INSTANCE.parser.parse( value );
    }

    public static IntfMatcherFactory getInstance()
    {
        return INSTANCE;
    }
}

