/*
 * Copyright (c) 2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: IPSessionDesc.java,v 1.5 2005/02/10 00:44:54 rbscott Exp $
 */

package com.metavize.mvvm.argon;

import java.net.InetAddress;

import com.metavize.jnetcap.Netcap;

public interface IPSessionDesc extends SessionDesc 
{
    public final short IPPROTO_TCP = (short)Netcap.IPPROTO_TCP;
    public final short IPPROTO_UDP = (short)Netcap.IPPROTO_UDP;

    /**
     * Returns the protocol for the session.</p>
     * @return a <code>short</code> giving one of the protocols (see Netcap.IPPROTO_*)
     */
    short protocol();
    
    /**
     * Returns an argon interface for the client.</p>
     *
     * @return a <code>byte</code> giving the client interface of the session.
     */
    byte clientIntf();

    /**
     * Returns an argon interface for the server.</p>
     *
     * @return a <code>byte</code> giving the server interface of the session.
     */
    byte serverIntf();

    /**
     * Gets the Client Address of this session. </p>
     *
     * @return  the client address
     */
    InetAddress clientAddr();

    /**
     * Gets the Server Address of this session. </p>
     *
     * @return  the server addr.
     */
    InetAddress serverAddr();

    // We provide these here, confident that nothing other than UDP or TCP
    // will be a session, others will be stateless.  (Or this will just be 0)
    
    /**
     * Gets the client port for this session.</p>
     * @return the client port.
     */
    int clientPort();

    /**
     * Gets the server port for this session.</p>
     * @return the server port.
     */
    int serverPort();
}
