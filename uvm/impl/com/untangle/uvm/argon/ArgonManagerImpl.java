/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.uvm.argon;

import com.untangle.uvm.ArgonManager;
import com.untangle.uvm.localapi.SessionMatcher;

/**
 * Argon manager.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
public class ArgonManagerImpl implements ArgonManager
{
    private static final ArgonManagerImpl INSTANCE = new ArgonManagerImpl();
            
    private ArgonManagerImpl()
    {
    }
    
    /** Get the number of sessions from the ArgonSessionTable */
    public int getSessionCount()
    {
        return ArgonSessionTable.getInstance().count();
    }

    public int getSessionCount(short protocol)
    {
        return ArgonSessionTable.getInstance().count(protocol);
    }
    
    /** Shutdown all of the sessions that match <code>matcher</code> */
    public void shutdownMatches( SessionMatcher matcher )
    {
        ArgonSessionTable.getInstance().shutdownMatches( matcher );
    }

    public static final ArgonManagerImpl getInstance()
    {
        return INSTANCE;
    }
}
