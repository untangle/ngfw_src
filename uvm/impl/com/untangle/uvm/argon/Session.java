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

package com.untangle.uvm.argon;

import com.untangle.jnetcap.NetcapSession;

import com.untangle.jvector.IncomingSocketQueue;
import com.untangle.jvector.OutgoingSocketQueue;

public interface Session extends SessionDesc {

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

    // For sizes, it means 'buffer' for TCP, 'packet' for UDP
    int maxInputSize();
    void maxInputSize(int size);

    int maxOutputSize();
    void maxOutputSize(int size);

    /* ??? Techincally, having a server and a client at the Session level isn't correct
     * because a broadcast session will not a have a client and a session, but whatever */

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
