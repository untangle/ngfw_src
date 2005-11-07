/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

package com.metavize.mvvm.argon;

import com.metavize.jvector.Source;
import com.metavize.jvector.Sink;
import com.metavize.jvector.SinkEndpointListener;
import com.metavize.jvector.SourceEndpointListener;

class SideListener implements SinkEndpointListener, SourceEndpointListener
{
    protected long txChunks = 0;
    protected long txBytes  = 0;

    protected long rxChunks = 0;
    protected long rxBytes  = 0;

    protected boolean isSourceShutdown = false;
    protected boolean isSinkShutdown   = false;

    protected SideListener()
    {
    }
    
    public void dataEvent( Source source, int numBytes )
    {
        rxChunks++;
        rxBytes += numBytes;
    }
    
    public void dataEvent( Sink sink, int numBytes )
    {
        txChunks++;
        txBytes += numBytes;
    }
    
    public void shutdownEvent( Source source )
    {
        isSourceShutdown = true;
    }
    
    public void shutdownEvent( Sink sink )
    {
        isSinkShutdown   = true;
    }

    protected boolean isShutdown()
    {
        return isSinkShutdown & isSourceShutdown;
    }

    protected String stats()
    {
        return "rx: " + rxBytes + "/" + rxChunks + " tx: " + txBytes + "/" + txChunks;
    
    }
}
