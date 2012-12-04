/*
 * $Id$
 */
package com.untangle.uvm.argon;

import com.untangle.uvm.ArgonManager;
import com.untangle.uvm.SessionMatcherGlobal;

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
    public void shutdownMatches( SessionMatcherGlobal matcher )
    {
        ArgonSessionTable.getInstance().shutdownMatches( matcher );
    }

    public static final ArgonManagerImpl getInstance()
    {
        return INSTANCE;
    }
}
