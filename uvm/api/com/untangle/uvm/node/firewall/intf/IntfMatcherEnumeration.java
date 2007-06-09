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

package com.untangle.uvm.node.firewall.intf;

import java.util.LinkedList;
import java.util.List;

import com.untangle.uvm.IntfEnum;
import com.untangle.uvm.IntfConstants;

import com.untangle.uvm.node.ParseException;

/**
 * An enumeration of all of the IntfMatchers that should be available to the GUI.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
public final class IntfMatcherEnumeration
{
    private static IntfMatcherEnumeration INSTANCE = new IntfMatcherEnumeration();

    /* Just in case it is not initialized */
    private IntfDBMatcher enumeration[] = new IntfDBMatcher[] { IntfSimpleMatcher.getAllMatcher() };
    private IntfEnum intfEnumCache = null;
    
    private IntfMatcherEnumeration()
    {
    }

    /**
     * Update the current enumeration.  Used when interfaces changes, such as when VPN or
     * USB interfaces are created.  All, Internal and External are always available.
     * If there is a DMZ interface, then DMZ and DMZ & External are created.  A single
     * matcher for VPN is created when the VPN interface is registered.
     *
     * @param intfEnum An enumeration of all of the current interfaces.
     */
    /* XXX This must be extended when we support more than three interfaces */
    synchronized void updateEnumeration( IntfEnum intfEnum )
    {
        /* If the cache is up to date, there is nothing to do */
        if ( this.intfEnumCache == intfEnum ) return;

        List<IntfDBMatcher> matchers = new LinkedList<IntfDBMatcher>();
        
        try {
            matchers.add( IntfSimpleMatcher.getAllMatcher());

            matchers.add( IntfSingleMatcher.makeInstance( IntfConstants.INTERNAL_INTF ));
            matchers.add( IntfSingleMatcher.makeInstance( IntfConstants.EXTERNAL_INTF ));

            if ( intfEnum.getIntfName( IntfConstants.VPN_INTF ) != null ) {
                matchers.add( IntfSingleMatcher.makeInstance( IntfConstants.VPN_INTF ));
                /* ??? Possibly add VPN and Internal */
            }
            
            if ( intfEnum.getIntfName( IntfConstants.DMZ_INTF ) != null ) {
                matchers.add( IntfSingleMatcher.makeInstance( IntfConstants.DMZ_INTF ));
                matchers.add( IntfSetMatcher.makeInstance( IntfConstants.EXTERNAL_INTF, 
                                                           IntfConstants.DMZ_INTF ));
            }            
        } catch ( ParseException e ) {
            /* Use System.err because log4j shouldn't be used inside of API */
            System.err.println( "Unable to initialize the interface matcher enumeration" );
        }
            
        /* Convert to an immutable list */
        this.enumeration = matchers.toArray( new IntfDBMatcher[matchers.size()]);
        
        /* Set the cache */
        this.intfEnumCache = intfEnum;
    }

    /**
     * Retrieve the enumeration of possible IntfMatchers.
     *
     * @return An array of valid IntfMatchers.
     */
    IntfDBMatcher[] getEnumeration()
    {
        return enumeration;
    }

    /**
     * Retrieve the default IntfMatcher.
     *
     * @return The default IntfMatcher
     */
    IntfDBMatcher getDefault()
    {
        return enumeration[0];
    }

    static IntfMatcherEnumeration getInstance()
    {
        return INSTANCE;
    }
}
