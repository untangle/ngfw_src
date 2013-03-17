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

package com.untangle.node.router;

import com.untangle.uvm.logging.LogEvent;

class RouterAttachment
{    
    /* True if this session uses a port that must be released */
    /* Port to release, 0, if a port should not be released */
    private int releasePort = 0;

    private LogEvent eventToLog = null;

    /* True if this session has created a session that must be removed from the session
     * manager.  (Presently the session manager only manages ftp sessions) */
    private boolean isManagedSession = false;
    
    RouterAttachment()
    {
    }

    boolean isManagedSession()
    {
        return this.isManagedSession;
    }

    void isManagedSession( boolean isManagedSession )
    {
        this.isManagedSession = isManagedSession;
    }

    int releasePort()
    {
        return this.releasePort;
    }

    void releasePort( int releasePort )
    {
        this.releasePort = releasePort;
    }

    LogEvent eventToLog()
    {
        return this.eventToLog;
    }

    void eventToLog(LogEvent eventToLog)
    {
        this.eventToLog = eventToLog;
    }
}
