/*
 * Copyright (c) 2003,2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: SocketQueueShutdownHook.java,v 1.1 2005/01/07 00:45:35 rbscott Exp $
 */

package com.metavize.jvector;

public interface SocketQueueShutdownHook
{
    void shutdownEvent( OutgoingSocketQueue osq );

    void shutdownEvent( IncomingSocketQueue isq );
}
