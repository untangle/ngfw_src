/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

package com.metavize.mvvm.argon;

import com.metavize.jvector.IncomingSocketQueue;
import com.metavize.jvector.OutgoingSocketQueue;

public interface PipelineListener
{
    void clientEvent( IncomingSocketQueue in );
    void clientEvent( OutgoingSocketQueue out );

    void serverEvent( IncomingSocketQueue in );
    void serverEvent( OutgoingSocketQueue out );

    void raze();

    /**
     * Right now the complete event is only delivered for
     *   a released session
     *   that needs finalization
     *   where the pipeline has been registered (server and client connected, or session rejected)
     */
    void complete();
}
