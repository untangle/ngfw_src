/**
 * $Id$
 */
package com.untangle.uvm.argon;

import com.untangle.uvm.node.SessionTuple;
import com.untangle.jnetcap.NetcapSession;

public interface ArgonNewSessionRequest extends SessionTuple
{
    /**
     * return the globally unique session ID
     */
    long id();

    /**
     * Return the user associated with this session
     */
    String user();

    /**
     * Gets the Netcap NodeSession associated with this session request.</p>
     *
     * @return the Netcap Session.
     */
    NetcapSession netcapSession();
    
    /**
     * Gets the Argon agent associated with this session request.</p>
     *
     * @return the Argon agent.
     */
    public ArgonAgent argonAgent();

    /**
     * Gets the global state for the session.</p>
     * @return session global state.
     */
    public SessionGlobalState sessionGlobalState();
}
