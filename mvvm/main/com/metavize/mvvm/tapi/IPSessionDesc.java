/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: IPSessionDesc.java,v 1.8 2005/01/30 09:20:31 amread Exp $
 */

package com.metavize.mvvm.tapi;

import java.net.InetAddress;

public interface IPSessionDesc extends SessionDesc {
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

    /**
     * Sessions have a direction of INBOUND when the client interface is 0 and the server
     * interface is 1.
     *
     */
    static final byte INBOUND = 1;

    /**
     * Sessions have a direction of OUTBOUND when the client interface is 1 and the server
     * interface is 0.
     *
     */
    static final byte OUTBOUND = 0;

    /**
      * Deprecated.  (Only works when there are only two possible interfaces)
      * Returns the direction of the session, either:
      * <code>INBOUND</code> or <code>OUTBOUND</code>
      *
      * @return a <code>byte</code> value giving the direction of the session
      */
    byte direction();

    /**
     * IP clients and servers have a state of <code>CLOSED</code> when both the input and
     * output sides are dead.
     *
     */
    static final byte CLOSED = 0;
    static final byte EXPIRED = 0;

    /**
     * IP client and servers have a state of <code>OPEN</code> when both the input and output
     * sides are alive.
     */
    static final byte OPEN = 4;

    byte clientState();
    byte serverState();

    // boolean isClientClosed();
    // boolean isServerClosed();

}
