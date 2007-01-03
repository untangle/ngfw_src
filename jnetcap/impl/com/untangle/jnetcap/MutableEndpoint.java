/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */


package com.untangle.jnetcap;

import java.net.InetAddress;

public interface MutableEndpoint extends Endpoint {
    public void host( InetAddress newHost );
    public void port( int newPort );
}
