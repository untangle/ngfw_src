/*
 * $Id$
 */
package com.untangle.uvm.argon;

import com.untangle.uvm.ArgonManager;
import com.untangle.uvm.SessionMatcher;
import com.untangle.uvm.vnet.PipeSpec;

/**
 * Argon manager.
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

    /** Shutdown all of the sessions that have been touch by the PipeSpec that match <code>matcher</code> */
    public void shutdownMatches( SessionMatcher matcher, PipeSpec ps )
    {
        ArgonSessionTable.getInstance().shutdownMatches( matcher, ps );
    }
    
    public static final ArgonManagerImpl getInstance()
    {
        return INSTANCE;
    }
}
