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

package com.untangle.uvm.node.firewall;

/**
 * A matcher for matching traffic direction.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
public class DirectionMatcher
{
    /* True if this triggers for inbound traffic */
    private final boolean inbound;
    
    /* True if this triggers for outbound traffic */
    private final boolean outbound;

    /* Enumeration of all of the possible direction matchers */
    static final DirectionMatcher ENUMERATION[] =  {
        new DirectionMatcher( false, false ),
        new DirectionMatcher( true, false ),
        new DirectionMatcher( false, true ),
        new DirectionMatcher( true, true )
    };

    private DirectionMatcher( boolean inbound, boolean outbound )
    {
        this.inbound  = inbound;
        this.outbound = outbound;
    }

    /**
     * Test if the direction <param>isInbound</param> matches this matcher.
     *
     * @param inbound The direction to test.
     */
    public boolean isMatch( boolean isInbound )
    {
        if ( isInbound && this.inbound ) return true;
        if ( !isInbound && this.outbound ) return true;
        return false;
    }

    /**
     * Retrieve a direction matcher.
     *
     * @param inbound True if the matcher should match inbound traffic.
     * @param outbound True if the matcher should match outbound traffic.
     */
    public static DirectionMatcher getInstance( boolean inbound, boolean outbound )
    {
        if ( inbound && outbound ) {
            return ENUMERATION[3];
        } else if ( inbound ) {
            return ENUMERATION[1];
        } else if ( outbound ) {
            return ENUMERATION[2];
        }

        return ENUMERATION[0];
    }
}
