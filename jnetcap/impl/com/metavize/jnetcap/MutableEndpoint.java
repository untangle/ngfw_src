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


package com.metavize.jnetcap;

import java.net.InetAddress;

public interface MutableEndpoint extends Endpoint {
    public void host( InetAddress newHost );
    public void port( int newPort );
}
