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

import java.util.LinkedList;
import java.util.List;

import com.untangle.mvvm.IntfEnum;
import com.untangle.mvvm.IntfConstants;

import com.untangle.mvvm.tran.ParseException;


public final class IntfMatcherEnumeration
{
    private static IntfMatcherEnumeration INSTANCE = new IntfMatcherEnumeration();

    /* Just in case it is not initialized */
    private IntfDBMatcher enumeration[] = new IntfDBMatcher[] { IntfSimpleMatcher.getAllMatcher() };
    private IntfEnum intfEnumCache = null;
    
    private IntfMatcherEnumeration()
    {
    }

    synchronized void updateEnumeration( IntfEnum intfEnum )
    {
        /* XXX Probably should be equals */
        if ( this.intfEnumCache == intfEnum ) return;

        List<IntfDBMatcher> matchers = new LinkedList<IntfDBMatcher>();
        
        try {
            matchers.add( IntfSimpleMatcher.getAllMatcher());

            matchers.add( IntfSingleMatcher.makeInstance( IntfConstants.INTERNAL_INTF ));
            matchers.add( IntfSingleMatcher.makeInstance( IntfConstants.EXTERNAL_INTF ));

            if ( intfEnum.getIntfName( IntfConstants.VPN_INTF ) != null ) {
                matchers.add( IntfSingleMatcher.makeInstance( IntfConstants.VPN_INTF ));
                /* XXX Possibly add VPN and Internal */
            }
            
            if ( intfEnum.getIntfName( IntfConstants.DMZ_INTF ) != null ) {
                matchers.add( IntfSingleMatcher.makeInstance( IntfConstants.DMZ_INTF ));
                matchers.add( IntfSetMatcher.makeInstance( IntfConstants.EXTERNAL_INTF, 
                                                           IntfConstants.DMZ_INTF ));
            }            
        } catch ( ParseException e ) {
            /* XXX Done this way because this may be executed from the GUI */
            System.err.println( "Unable to initialize the interface matcher enumeration" );
        }
            
        /* Convert to an immutable list */
        this.enumeration = matchers.toArray( new IntfDBMatcher[matchers.size()]);
        
        /* Set the cache */
        this.intfEnumCache = intfEnum;
    }

    IntfDBMatcher[] getEnumeration()
    {
        return enumeration;
    }

    IntfDBMatcher getDefault()
    {
        return enumeration[0];
    }

    static IntfMatcherEnumeration getInstance()
    {
        return INSTANCE;
    }
}
