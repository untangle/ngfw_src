/**
 * $Id$
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
