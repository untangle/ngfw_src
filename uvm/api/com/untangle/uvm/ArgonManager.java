/*
 * $Id$
 */
package com.untangle.uvm;

import com.untangle.uvm.SessionMatcherGlobal;
import com.untangle.uvm.vnet.PipeSpec;

public interface ArgonManager
{    
    /** Get the number of sessions from the ArgonSessionTable */
    public int getSessionCount();

    /** Get the number of sessions from the ArgonSessionTable */
    public int getSessionCount(short protocol);
    
    /** Shutdown all of the sessions that match <code>matcher</code> */
    public void shutdownMatches( SessionMatcherGlobal matcher );
    public void shutdownMatches( SessionMatcherGlobal matcher, PipeSpec ps );
    
}
