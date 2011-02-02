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

import com.untangle.jvector.Sink;
import com.untangle.jvector.SinkEndpointListener;
import com.untangle.jvector.Source;
import com.untangle.jvector.SourceEndpointListener;

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
