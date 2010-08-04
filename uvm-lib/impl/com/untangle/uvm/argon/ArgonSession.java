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

import com.untangle.jnetcap.NetcapSession;
import com.untangle.jvector.IncomingSocketQueue;
import com.untangle.jvector.OutgoingSocketQueue;

public interface ArgonSession extends ArgonSessionDesc {

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
