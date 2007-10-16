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

import com.untangle.jvector.IncomingSocketQueue;
import com.untangle.jvector.OutgoingSocketQueue;

public interface PipelineListener
{
    void clientEvent( IncomingSocketQueue in );
    void clientEvent( OutgoingSocketQueue out );
    /* This is equivalent to an EPIPE */
    void clientOutputResetEvent( OutgoingSocketQueue out );

    void serverEvent( IncomingSocketQueue in );
    void serverEvent( OutgoingSocketQueue out );
    /* This is equivalent to an EPIPE */    
    void serverOutputResetEvent( OutgoingSocketQueue out );

    void raze();

    /**
     * Right now the complete event is only delivered for
     *   a released session
     *   that needs finalization
     *   where the pipeline has been registered (server and client connected, or session rejected)
     */
    void complete();
}
