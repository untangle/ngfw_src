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

package com.untangle.uvm.node.firewall.intf;

import java.util.LinkedList;
import java.util.List;

import com.untangle.uvm.IntfConstants;
import com.untangle.uvm.IntfEnum;
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

            for (byte num : intfEnum.getIntfNums()) {
                matchers.add(IntfSingleMatcher.makeInstance(num));
            }

            matchers.add( IntfRelativeMatcher.getMoreExternalMatcher());
            matchers.add( IntfRelativeMatcher.getMoreInternalMatcher());

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
