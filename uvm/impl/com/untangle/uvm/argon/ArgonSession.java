/**
 * $Id$
 */
package com.untangle.uvm.argon;

import com.untangle.uvm.node.SessionTuple;
import com.untangle.jnetcap.NetcapSession;
import com.untangle.jvector.IncomingSocketQueue;
import com.untangle.jvector.OutgoingSocketQueue;

public interface ArgonSession extends SessionTuple
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
     * <code>argonAgent</code> returns the ArgonAgent that this session lives on.
     *
     * @return the <code>ArgonAgent</code> that this session is for
     */
    ArgonAgent argonAgent();

    /**
     * <code>netcapSession</code> returns the netcap session that this session is apart of.
     */
    NetcapSession netcapSession();

    /**
     * <code>sessionGlobalState</code> returns all of the state that is global per session.
     */
    SessionGlobalState sessionGlobalState();
    
    /**
     * <code>isVectored</code> returns whether or not this session should be vectored.
     * ACCEPTED sessions are vectored, released sessions are not.
     */
    boolean isVectored();

    /**
     * For sizes, it means 'buffer' for TCP, 'packet' for UDP
     */
    int maxInputSize();
    void maxInputSize(int size);

    int maxOutputSize();
    void maxOutputSize(int size);

    /**
     * Shutdown the client side of the connection.
     */
    void shutdownClient();

    /**
     * Shutdown the server side of the connection.
     */
    void shutdownServer();

    /**
     * Kill the entire session.  Only used when there is an error of some sort
     */
    void killSession();

    /**
     * Register a listener a listener for the session. 
     */
    void registerListener( PipelineListener listener );
    
    
    IncomingSocketQueue clientIncomingSocketQueue();

    OutgoingSocketQueue clientOutgoingSocketQueue();

    IncomingSocketQueue serverIncomingSocketQueue();

    OutgoingSocketQueue serverOutgoingSocketQueue();
}
