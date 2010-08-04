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

public interface NewSessionRequest extends SessionDesc
{
    /**
     * Gets the Netcap Session associated with this session request.</p>
     *
     * @return the Netcap Session.
     */
    NetcapSession netcapSession();
    
    /**
     * Gets the Argon agent associated with this session request.</p>
     *
     * @return the Argon agent.
     */
    public ArgonAgent argonAgent();

    /**
     * Gets the global state for the session.</p>
     * @return session global state.
     */
    public SessionGlobalState sessionGlobalState();


}
