/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.argon;

import com.metavize.jnetcap.*;
import com.metavize.jvector.*;

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
